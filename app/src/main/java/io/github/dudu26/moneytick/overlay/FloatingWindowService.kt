// SPDX-FileCopyrightText: 2026 dudu (DuDu-26)
// SPDX-License-Identifier: AGPL-3.0-only

package io.github.dudu26.moneytick.overlay

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import io.github.dudu26.moneytick.R
import io.github.dudu26.moneytick.core.DoubleTapAction
import io.github.dudu26.moneytick.core.FloatConfig
import io.github.dudu26.moneytick.data.settingsRepository
import io.github.dudu26.moneytick.ui.MainActivity
import io.github.dudu26.moneytick.ui.rememberSalarySnapshot
import io.github.dudu26.moneytick.core.AppSettings
import io.github.dudu26.moneytick.core.calculatePayday
import io.github.dudu26.moneytick.core.calculateSalarySnapshot
import io.github.dudu26.moneytick.core.formatAmount
import io.github.dudu26.moneytick.ui.countdownLabel
import io.github.dudu26.moneytick.ui.formatCoarseDuration
import io.github.dudu26.moneytick.ui.paydayText
import io.github.dudu26.moneytick.ui.statusText
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import java.time.LocalDateTime
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Foreground service that owns the floating window (TYPE_APPLICATION_OVERLAY) and, when
 * enabled, the live "today's earnings" notification. It runs while either is wanted.
 * Drag the body to move, drag the corner handle to resize, double-tap to hide (or open the app).
 */
class FloatingWindowService : LifecycleService(), SavedStateRegistryOwner {
    private val savedStateController = SavedStateRegistryController.create(this)
    override val savedStateRegistry: SavedStateRegistry get() = savedStateController.savedStateRegistry

    private lateinit var windowManager: WindowManager
    private var view: ComposeView? = null
    private lateinit var params: WindowManager.LayoutParams
    private var gestureActive = false
    private var layoutJob: Job? = null
    private var handledFirstStart = false

    // Power saving: the ticker stops completely while the screen is off.
    private val screenOn = MutableStateFlow(true)
    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            screenOn.value = intent.action != Intent.ACTION_SCREEN_OFF
        }
    }

    private val density get() = resources.displayMetrics.density
    private fun Int.dpToPx() = (this * density).roundToInt()
    private fun Int.pxToDp() = (this / density).roundToInt()

    override fun onCreate() {
        savedStateController.performAttach()
        savedStateController.performRestore(null)
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        screenOn.value = getSystemService(PowerManager::class.java).isInteractive
        ContextCompat.registerReceiver(
            this,
            screenReceiver,
            IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_ON)
                addAction(Intent.ACTION_SCREEN_OFF)
            },
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        aliveState.value = true
        // Live notification: refresh every 15 s, only while the screen is on.
        lifecycleScope.launch {
            combine(screenOn, settingsRepository.settings) { on, s -> on to s }
                .collectLatest { (on, s) ->
                    if (handledFirstStart && !s.notificationEnabled && view == null) {
                        stopSelf()
                        return@collectLatest
                    }
                    postNotification(s)
                    while (on && s.notificationEnabled) {
                        delay(NOTIFICATION_REFRESH_MS)
                        postNotification(s)
                    }
                }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        // Every start must promote to foreground first, even if we stop right after.
        postNotification(settingsRepository.settings.value, foreground = true)
        when (intent?.action) {
            ACTION_STOP -> {
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_SHOW -> if (Settings.canDrawOverlays(this) && view == null) showWindow()
            ACTION_HIDE -> removeWindow()
        }
        handledFirstStart = true
        if (view == null && !settingsRepository.settings.value.notificationEnabled) {
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    override fun onDestroy() {
        runCatching { unregisterReceiver(screenReceiver) }
        removeWindow()
        aliveState.value = false
        super.onDestroy()
    }

    private fun removeWindow() {
        layoutJob?.cancel()
        layoutJob = null
        view?.let { runCatching { windowManager.removeView(it) } }
        view = null
        runningState.value = false
    }

    private fun hideWindow() {
        removeWindow()
        if (!settingsRepository.settings.value.notificationEnabled) stopSelf()
    }

    private fun postNotification(settings: AppSettings, foreground: Boolean = false) {
        val nm = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, getString(R.string.notification_channel), NotificationManager.IMPORTANCE_MIN),
            )
            nm.createNotificationChannel(
                NotificationChannel(LIVE_CHANNEL_ID, getString(R.string.notification_live_channel), NotificationManager.IMPORTANCE_LOW)
                    .apply { setShowBadge(false) },
            )
        }
        val openApp = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE,
        )
        val stop = PendingIntent.getService(
            this, 1, Intent(this, FloatingWindowService::class.java).setAction(ACTION_STOP), PendingIntent.FLAG_IMMUTABLE,
        )
        val builder = if (settings.notificationEnabled) {
            val now = LocalDateTime.now()
            val snap = calculateSalarySnapshot(now, settings.salary)
            val payday = calculatePayday(now, settings.salary, settings.paydayOfMonth)
            val parts = buildList {
                add(statusText(snap))
                countdownLabel(snap)?.takeIf { it != "今日完成" }?.let { add("$it ${formatCoarseDuration(snap.nextTransitionMs)}") }
                payday?.let { add(paydayText(it.daysUntil)) }
            }
            NotificationCompat.Builder(this, LIVE_CHANNEL_ID)
                .setContentTitle("今日入账 " + formatAmount(snap.earnedToday, settings.currencySymbol))
                .setContentText(parts.joinToString(" · "))
                .setProgress(1000, (snap.progress * 1000).toInt(), false)
                .setPriority(NotificationCompat.PRIORITY_LOW)
        } else {
            NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(getString(R.string.notification_title))
                .setPriority(NotificationCompat.PRIORITY_MIN)
        }
        val notification = builder
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(openApp)
            .addAction(0, getString(R.string.notification_stop), stop)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setShowWhen(false)
            .build()
        if (foreground) {
            val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            } else 0
            ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, type)
        } else {
            runCatching { nm.notify(NOTIFICATION_ID, notification) }
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    private fun showWindow() {
        val repo = settingsRepository
        val initial = repo.settings.value.floatConfig
        params = WindowManager.LayoutParams(
            initial.widthDp.dpToPx(),
            initial.heightDp.dpToPx(),
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = initial.x
            y = initial.y
        }
        val composeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@FloatingWindowService)
            setViewTreeSavedStateRegistryOwner(this@FloatingWindowService)
            setContent {
                val settings by repo.settings.collectAsStateWithLifecycle()
                val visible by screenOn.collectAsStateWithLifecycle()
                val snapshot = rememberSalarySnapshot(settings.salary, settings.refreshRate, visible)
                Box(Modifier.fillMaxSize()) {
                    FloatContent(
                        settings, snapshot,
                        Modifier.pointerInteropFilter { onBodyTouch(it, settings.floatConfig.locked) },
                    )
                    if (!settings.floatConfig.locked) {
                        Box(
                            Modifier
                                .align(Alignment.BottomEnd)
                                .size(18.dp)
                                .background(Color(settings.floatConfig.textArgb.toInt()).copy(alpha = 0.18f), RoundedCornerShape(topStart = 10.dp))
                                .pointerInteropFilter { onResizeTouch(it) },
                        )
                    }
                }
            }
        }
        windowManager.addView(composeView, params)
        view = composeView
        runningState.value = true

        // Apply size/position changes made on the settings screen.
        layoutJob = lifecycleScope.launch {
            repo.settings.collect { s ->
                if (gestureActive) return@collect
                val f = s.floatConfig
                val w = f.widthDp.dpToPx()
                val h = f.heightDp.dpToPx()
                if (params.width != w || params.height != h || params.x != f.x || params.y != f.y) {
                    params.width = w; params.height = h; params.x = f.x; params.y = f.y
                    view?.let { windowManager.updateViewLayout(it, params) }
                }
            }
        }
    }

    // --- gestures (raw screen coordinates, so moving the window does not skew the deltas) ---

    private var downRawX = 0f
    private var downRawY = 0f
    private var startX = 0
    private var startY = 0
    private var startW = 0
    private var startH = 0
    private var moved = false
    private var lastTapAt = 0L

    private fun onBodyTouch(e: MotionEvent, locked: Boolean): Boolean {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downRawX = e.rawX; downRawY = e.rawY
                startX = params.x; startY = params.y
                moved = false
                gestureActive = true
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = e.rawX - downRawX
                val dy = e.rawY - downRawY
                if (!moved && abs(dx) + abs(dy) > TOUCH_SLOP_DP * density) moved = true
                if (moved && !locked) {
                    params.x = startX + dx.roundToInt()
                    params.y = startY + dy.roundToInt()
                    view?.let { windowManager.updateViewLayout(it, params) }
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                gestureActive = false
                if (moved && !locked) {
                    persist { it.copy(x = params.x, y = params.y) }
                } else if (e.actionMasked == MotionEvent.ACTION_UP) {
                    val now = e.eventTime
                    if (now - lastTapAt < DOUBLE_TAP_MS) {
                        lastTapAt = 0
                        when (settingsRepository.settings.value.floatConfig.doubleTapAction) {
                            DoubleTapAction.HIDE -> hideWindow()
                            DoubleTapAction.OPEN_APP -> openApp()
                        }
                    } else {
                        lastTapAt = now
                    }
                }
            }
        }
        return true
    }

    private fun onResizeTouch(e: MotionEvent): Boolean {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downRawX = e.rawX; downRawY = e.rawY
                startW = params.width; startH = params.height
                gestureActive = true
            }
            MotionEvent.ACTION_MOVE -> {
                params.width = (startW + (e.rawX - downRawX).roundToInt())
                    .coerceIn(FloatConfig.MIN_WIDTH_DP.dpToPx(), FloatConfig.MAX_WIDTH_DP.dpToPx())
                params.height = (startH + (e.rawY - downRawY).roundToInt())
                    .coerceIn(FloatConfig.MIN_HEIGHT_DP.dpToPx(), FloatConfig.MAX_HEIGHT_DP.dpToPx())
                view?.let { windowManager.updateViewLayout(it, params) }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                gestureActive = false
                persist { it.copy(widthDp = params.width.pxToDp(), heightDp = params.height.pxToDp()) }
            }
        }
        return true
    }

    private fun persist(transform: (FloatConfig) -> FloatConfig) {
        settingsRepository.update { it.copy(floatConfig = transform(it.floatConfig).normalized()) }
    }

    @SuppressLint("StartActivityFromNonActivityContext")
    private fun openApp() {
        // Apps holding SYSTEM_ALERT_WINDOW may start activities from the background.
        startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    companion object {
        private const val CHANNEL_ID = "floating_window"
        private const val LIVE_CHANNEL_ID = "live_amount"
        private const val NOTIFICATION_ID = 1
        private const val NOTIFICATION_REFRESH_MS = 15_000L
        private const val ACTION_STOP = "io.github.dudu26.moneytick.STOP"
        private const val ACTION_SHOW = "io.github.dudu26.moneytick.SHOW_FLOAT"
        private const val ACTION_HIDE = "io.github.dudu26.moneytick.HIDE_FLOAT"
        private const val ACTION_SYNC = "io.github.dudu26.moneytick.SYNC"
        private const val TOUCH_SLOP_DP = 6
        private const val DOUBLE_TAP_MS = 300L

        /** True while the floating window is on screen. */
        private val runningState = MutableStateFlow(false)
        val running: StateFlow<Boolean> = runningState.asStateFlow()

        /** True while the service process is up (floating window and/or notification). */
        private val aliveState = MutableStateFlow(false)

        private fun send(context: Context, action: String) =
            ContextCompat.startForegroundService(context, Intent(context, FloatingWindowService::class.java).setAction(action))

        /** Shows the floating window (caller has checked the overlay permission). */
        fun start(context: Context) = send(context, ACTION_SHOW)

        /** Hides the floating window; the service keeps running if the notification is on. */
        fun stop(context: Context) {
            if (aliveState.value) send(context, ACTION_HIDE)
        }

        /** Starts or stops the service to match the notification setting. */
        fun syncNotification(context: Context, enabled: Boolean) {
            if (enabled || aliveState.value) send(context, ACTION_SYNC)
        }
    }
}
