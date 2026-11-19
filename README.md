# 摸鱼币 MoneyTick

把「今天正在挣到的钱」放到安卓手机上：实时显示今日入账，还能开一个**大小可调、内容可自定义的悬浮窗**，在任何 App 上面随时瞄一眼。

> 作者：**dudu（[@DuDu-26](https://github.com/DuDu-26)）**
>
> 本项目是 [PayDance（薪跳）](https://github.com/MrBaoboer/PayDance) 的**修改版安卓移植**，并非 PayDance 官方版本，也未获得其作者背书。

## 功能

- **实时金额**：今日入账按秒增长，精确到分。
- **常见工作制**：月薪 / 日薪 / 时薪自动换算，每周工作日、午休剔除、跨零点夜班。
- **看板**：今日进度、已工作时长、距离上班/复工/下班、今日预计；时薪、分薪、秒薪换算。
- **悬浮窗**（安卓新增）：
  - 拖动移动，拖右下角调整大小，双击回到 App；
  - 显示内容自由勾选和排序：金额、状态、进度条、倒计时、秒薪、今日预计；
  - 宽高、字号、不透明度（10%–100%）、圆角、背景色、文字色可调；
  - 可锁定位置和大小，防止误触。
- **省电**：息屏时悬浮窗完全停止刷新，App 退到后台也停止；下班和休息日每分钟才刷新一次；刷新频率可选实时 / 1 秒 / 5 秒 / 1 分钟；悬浮窗不做任何动画。
- **本地优先**：不联网、不需要账号、无遥测，配置只存在手机里。

## 下载

每次推送到 `main` 后，GitHub Actions 会自动打包 APK：打开 [Actions](../../actions) → 最新一次运行 → 下载 `moneytick-apk`。打 `v*` 标签时会发布到 [Releases](../../releases)。

首次开启悬浮窗需要授予「显示在其他应用上层」权限。

## 开发

- Kotlin + Jetpack Compose（Material 3），minSdk 26（Android 8.0），targetSdk 35
- `core/`：纯 Kotlin 薪资计算引擎和设置模型（无 Android 依赖，可在任何 JVM 上测试）
- `app/`：安卓界面与悬浮窗前台服务（`TYPE_APPLICATION_OVERLAY`）

```bash
./gradlew :core:test                  # 单元测试（没有 Android SDK 时加 -PcoreOnly=true）
./gradlew :app:assembleDebug          # 打包调试 APK（需要 Android SDK）
```

方案与路线图见 [docs/PLAN.md](docs/PLAN.md)。

## 许可

代码以 [AGPL-3.0-only](LICENSE) 发布，并附带 PayDance 的 [附加条款](legal/ADDITIONAL_TERMS.md)。

Based on PayDance. Copyright (C) 2026 Mr.Baoboer.
Licensed under the GNU Affero General Public License v3.0 only.

基于 PayDance。版权所有 (C) 2026 Mr.Baoboer。采用 GNU Affero General Public License v3.0 only 许可。

本项目新增与修改部分版权所有 (C) 2026 dudu（@DuDu-26）。「薪跳」「PayDance」名称与图标归原作者所有，本项目不使用其品牌素材。
