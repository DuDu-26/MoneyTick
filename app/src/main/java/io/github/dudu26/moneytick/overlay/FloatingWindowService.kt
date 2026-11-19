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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Foreground service that owns the floating window (TYPE_APPLICATION_OVERLAY).
 * Drag the body to move, drag the corner handle to resize, double-tap to hide (or open the app).
 */
class FloatingWindowService : LifecycleService(), SavedStateRegistryOwner {
    private val savedStateController = SavedStateRegistryController.create(this)
    override val savedStateRegistry: SavedStateRegistry get() = savedStateController.savedStateRegistry

    private lateinit var windowManager: WindowManager
    private var view: ComposeView? = null
    private lateinit var params: WindowManager.LayoutParams
    private var gestureActive = false

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
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        if (intent?.action == ACTION_STOP || !Settings.canDrawOverlays(this)) {
            stopSelf()
            return START_NOT_STICKY
        }
        startInForeground()
        if (view == null) showWindow()
        return START_STICKY
    }

    override fun onDestroy() {
        runCatching { unregisterReceiver(screenReceiver) }
        view?.let { runCatching { windowManager.removeView(it) } }
        view = null
        runningState.value = false
        super.onDestroy()
    }

    private fun startInForeground() {
        val nm = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, getString(R.string.notification_channel), NotificationManager.IMPORTANCE_MIN),
            )
        }
        val openApp = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE,
        )
        val stop = PendingIntent.getService(
            this, 1, Intent(this, FloatingWindowService::class.java).setAction(ACTION_STOP), PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(getString(R.string.notification_title))
            .setContentIntent(openApp)
            .addAction(0, getString(R.string.notification_stop), stop)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        } else 0
        ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, type)
        runningState.value = true
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

        // Apply size/position changes made on the settings screen.
        lifecycleScope.launch {
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
                            DoubleTapAction.HIDE -> stopSelf()
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
        private const val NOTIFICATION_ID = 1
        private const val ACTION_STOP = "io.github.dudu26.moneytick.STOP_FLOAT"
        private const val TOUCH_SLOP_DP = 6
        private const val DOUBLE_TAP_MS = 300L

        private val runningState = MutableStateFlow(false)
        val running: StateFlow<Boolean> = runningState.asStateFlow()

        fun start(context: Context) =
            ContextCompat.startForegroundService(context, Intent(context, FloatingWindowService::class.java))

        fun stop(context: Context) {
            context.stopService(Intent(context, FloatingWindowService::class.java))
        }
    }
}
