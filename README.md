# Android Visual UI Inspector

运行在 Android 设备上的 UI 检查工具，使用 Kotlin、Jetpack Compose / Material 3、AccessibilityService 和系统截图 API。安装后独立运行，不需要 root、Shizuku、ADB server 或 Appium server。

## Features

- 核心流程：**Select → 点击一次 Item → 尺寸、邻近间距、实际渲染颜色**。
- 蓝色尺寸标尺、橙色四方向间距、主色色块与 HEX 直接画在目标 App 上，长度同时显示 dp / px。
- 可拖动四按钮工具条：Select / A ↔ B / Picker / Close；选择模式 30 秒自动退出。
- 默认紧凑测量卡片；Details 展开尺寸、间距、颜色、位置，Advanced 默认折叠保留完整 Accessibility 信息。
- 点击 Details 中的邻居按钮可切换选择；Parent / Child / Previous / Next 与 Copy ID / Bounds / Appium 保留。
- A/B 边缘测距，斜对角分别显示两轴距离；独立单像素 Color Picker。
- Freeze 固定当前 bounds、间距、颜色与 Item 截图；Copy 导出双单位测量摘要。
- 目标变化时标记历史快照，取消旧高亮并提示重新选择。
- 密码节点文本脱敏；数据仅暂存内存，无网络权限，无上传。

## Requirements

Android 11+ / API 30+，用户手动启用 Accessibility Service。

构建使用 JDK 17+（本机 JDK 21）、Android SDK 37、Gradle Wrapper 9.3.1、AGP 9.1.1、Kotlin / Compose compiler 2.3.20。`minSdk = 30`，`compileSdk = targetSdk = 37`。首次构建需下载依赖并接受 Android SDK 许可。

## Installation

用支持 API 37 的 Android Studio 打开根目录，选择 app 模块，Build APK / Run；或：

```sh
./gradlew :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Windows 使用 `gradlew.bat`。配置 `ANDROID_HOME` 或本机 `local.properties` 中的 `sdk.dir`（不提交）。APK 输出：`app/build/outputs/apk/debug/app-debug.apk`。ADB 仅用于开发，安装后的 APK 独立运行。正式分发需配置自己的签名；当前 APK 为 debug 签名。

## Setup

1. 打开 UI Inspector，点击 **Enable Accessibility**。
2. 在系统设置手动启用 **UI Inspector**。侧载应用可能需先在应用信息中允许受限设置。
3. 返回首页，等待 **Enabled / Connected**，点击 **Start Inspector**。
4. 切换至目标 App，点击悬浮按钮，点击待检查元素。

服务通过 `BIND_ACCESSIBILITY_SERVICE` 保护，XML 启用 `canRetrieveWindowContent`、`canTakeScreenshot`、`flagReportViewIds` 和 `flagRetrieveInteractiveWindows`。不申请普通悬浮窗权限，不绕过无障碍授权。

## Usage

点击 **Select** 进入选择层，再点击目标。先展示 bounds、尺寸、可靠邻居间距，随后自动截图并更新颜色。拖动 Select 按钮移动整条工具栏；选择中点击 Cancel 或等待 30 秒退出；长按 Select 或点击 × 停止。

- **Parent / Child**：沿本次快照的父节点 / 第一个子节点移动。
- **Previous / Next**：切换原点击点覆盖的候选；不覆盖该点的导航节点显示 Tree node。
- **Refresh color**：刷新当前快照颜色，不持续抓屏。
- **Select**：重新取节点快照。目标改变后旧数据标记 stale，要求重新选择。
- **A ↔ B**：已有有效选择时作为 A，再点 B；否则先点 A 再点 B。两个节点须处于同一未改变的窗口与 density。
- **Picker**：点击一个屏幕像素，显示 HEX / RGB，可复制；不要求该点提供独立语义节点。
- **Freeze**：颜色请求完成后冻结快照（包括失败状态）。保留裁剪 Item 的 PNG，直到 Unfreeze、重新选择或关闭。旋转后仅保留历史卡片，避免在新坐标上绘制旧框。
- **Copy**：复制尺寸、四方向可靠间距、A/B 间距、主色 HEX / RGB 与快照状态。不会主动发送到其他应用。
- **Details → Advanced**：展开原有 Accessibility 属性和定位符复制。详情可滚动；Details / Freeze / Copy 固定在上方。

Copy Appium 优先 `AppiumBy.ID`，其次 `AppiumBy.ACCESSIBILITY_ID`；均缺失时只提示可能的文本，不生成虚假 XPath。定位符的唯一性取决于目标 App。

## Supported Information

| Property | Source |
| --- | --- |
| Package、Resource ID、Class | Accessibility |
| Text、Content Description | Accessibility，密码文本脱敏 |
| Bounds、X/Y、Width/Height px | `getBoundsInScreen()` |
| Width/Height、间距、位置 dp / px | DimensionValue 保存 Float，显示最多 2 位小数，去掉尾零 |
| clickable、enabled、focusable、focused、selected | Accessibility |
| checkable、checked、scrollable、editable、visibleToUser | Accessibility |
| actions、window ID、depth、child count | Accessibility |
| Center、Dominant、Top 3 Colors | 渲染像素统计 |

## Android Version Behavior

| Version / condition | Screenshot strategy |
| --- | --- |
| API 30–33 | `takeScreenshot(DEFAULT_DISPLAY, ...)`；临时隐藏本工具 overlay 后截图，再恢复 |
| API 34+ | 优先 `takeScreenshotOfWindow(windowId, ...)`，排除 Accessibility overlay 遮挡 |
| 窗口截图失败 / 无 window ID | 对可恢复错误回退 display screenshot，临时隐藏 overlay |
| Secure window | Color unavailable，不作绕过性回退，保留节点属性 |

API 34 方法均有 `Build.VERSION.SDK_INT >= 34` 判断。请求串行，至少间隔 350 ms，限频错误重试一次，4 秒超时。权限断开、无效窗口/display、转换失败均展示错误状态。

窗口截图以窗口屏幕 bounds 为原点，整屏截图以 display bounds 为原点。只接受已知的 1:1 buffer 尺寸；不能确认映射时拒绝取色，不猜测坐标。先与截图相交裁剪，中心不可见时不冒充其他像素。硬件 Bitmap 裁出所需区域后复制 ARGB_8888，转换和采样在后台线程，取消/异常路径释放 HardwareBuffer 和 Bitmap。

## Color Analyzer

分析的是 **Rendered visual color**，不是源码中的 `backgroundColor`。

1. 裁至可见节点区域，内缩左右 10%、上下 15%，小控件至少保留一个像素。
2. 按面积自适应步长，约 4096 个样本；忽略 alpha < 128。
3. R/G/B 按步长 8 量化，聚合相近色，降低字体抗锯齿和阴影干扰。
4. 每个色簇返回该簇内出现最多的真实 RGB（众数），不返回桶边界或 RGB 均值。
5. 输出前三色及占比；第一色 > 30% 时作为 dominant，否则只报告 Top 3。

中心像素单独读取，可能正好是文字。样本转换为 sRGB，输出 `#RRGGBB`。参数集中在 `ColorConfig`。

## Measurement and neighbor rules

`SelectedItemAnalysis` 是测量 UI 的唯一主要输入，包含不可变 NodeSnapshot、px / dp bounds、DimensionValue 尺寸与位置、四方向 NeighborMeasurement、窗口边距、异步 ItemColorAnalysis、可选 A/B 结果与 frozen / stale 状态。`SelectedItemAnalyzer` 统一组装几何与颜色，`ItemColorCapture` 在后台分析并保存 Item PNG；Overlay 不直接遍历 AccessibilityNodeInfo。

上下邻居要求 X 投影重叠、位于所选元素上下边缘外；左右邻居要求 Y 投影重叠、位于左右边缘外。间距为相邻边缘坐标差，不使用中心点欧氏距离。过滤不可见、零尺寸、同 bounds、双向包含、父子层级、本工具节点、不同窗口和部分越界节点。

`NeighborConfig` 默认最小有效尺寸 2dp、投影比例 0.20、置信度 0.45。投影比例相对于双方较大跨度；置信度综合投影、尺寸质量与 dp 距离。极小点过滤，细长 divider 保留并降低尺寸质量。通过门槛后先取最小 gap，同 gap 优先更大 overlap、置信度及深度。未找到可靠邻居的方向不绘制。

A/B 的投影重叠决定主测距轴；斜对角在卡片分别显示水平、垂直 separation，重叠明确报告为重叠。窗口边距独立列出，不能冒充 Item 间距。取消任务加 selection generation 防止旧颜色覆盖新选择。

## Architecture

```text
app/src/main/java/com/xiaoyue/uiinspector/
├── MainActivity.kt
├── accessibility/   # Service lifecycle and connection state
├── inspector/       # State machine, tree snapshots, ranking, orchestration
├── overlay/         # Window ownership, toolbar, capture, measurement rulers, card
├── measurement/     # DimensionValue, directional neighbors, edge gaps
├── analysis/        # SelectedItemAnalysis, staged analyzer, PNG snapshot, summary
├── screenshot/      # API guards, fallback, buffer ownership, coordinates
├── color/           # Pure Kotlin sampling and color statistics
├── util/            # Immutable bounds, density, clipboard, locators
└── ui/home/         # Compose Material 3 permission/status screen
app/src/test/        # JVM unit tests
app/src/debug/       # Debug-only screenshot validation activity
qa-target/           # Optional separate deterministic test app
docs/VALIDATION.md   # Validation record
```

Service 只处理系统生命周期与事件，Controller 负责状态与异步工作，OverlayController 管理 `TYPE_ACCESSIBILITY_OVERLAY`。服务浮窗使用原生 View，主应用使用 Compose Material 3。

NodeTreeBuilder 仅在点击时遍历，最多 5000 节点 / 100 层。遍历后释放 API 30–32 节点，UI 只保存不可变 NodeSnapshot / Bounds。排序集中于 NodeFinder：包含点击点 → 可见优先 → 面积小 → 深度大 → ID / clickable / 文本语义。窗口先筛选 bounds，按 active、focused、layer 排序，排除本工具与 accessibility overlay。

高亮将屏幕 bounds 减去 `getLocationOnScreen()` 转成 Canvas 局部坐标，绘制 2dp 边框。触摸使用 `rawX/rawY`。窗口处理系统栏 / cutout，但系统保留手势区域仍可能不可捕获。

空闲不遍历、不截图。事件仅做 250 ms debounce 失效标记。配置改变重建 idle overlay，要求重新选择，避免复用旧旋转、窗口大小或密度。

## Testing

```sh
./gradlew test assembleDebug :app:lintDebug
./gradlew :qa-target:assembleDebug
adb install -r qa-target/build/outputs/apk/debug/qa-target-debug.apk
# Manually enable the installed UI Inspector accessibility service first:
adb shell am start --user current -n com.xiaoyue.uiinspector/.ScreenshotValidationActivity
# After completion (replace 10 with `adb shell am get-current-user` output):
adb shell run-as com.xiaoyue.uiinspector --user 10 cat files/screenshot-validation.txt
```

41 项 JVM 测试包含原有 18 项与新增 DimensionValueTest、NeighborFinderTest、SpacingCalculatorTest、SelectedItemAnalyzerTest、ColorAnalyzerTest，覆盖四方向、投影、斜对角、父子/同 bounds 过滤、双单位、A/B、迟到颜色隔离、真实 RGB 众数和冻结图像所有权。设备截图验证窗口排除覆盖层、整屏临时隐藏覆盖层、FLAG_SECURE。Debug 专用 Activity 在已授权的服务进程内执行，避免 Instrumentation 强制停止服务；不会自动开启权限，Release 不含这些入口。

新增确定性测量页面及实机分析检查：

```sh
adb shell am start --user current -n com.xiaoyue.uiinspector/.MeasurementValidationActivity
adb shell run-as com.xiaoyue.uiinspector --user 10 cat files/measurement-validation.txt
```

验证尺寸 328 × 48dp、上下 24dp / 左右 16dp（允许布局转整数造成的像素误差）、四方向资源 ID、主色 #C7C6CA 及保存的 PNG。完成后留下测试页面和工具条供手动 Smoke Test。也可直接启动 fixture 并传 `--ez measurement true`。

启动安全测试窗口（普通窗口去掉 `--ez secure true`）：

```sh
adb shell am force-stop --user current com.xiaoyue.inspectorfixture
adb shell am start --user current -n com.xiaoyue.inspectorfixture/.FixtureActivity --ez secure true
```

QA Target 是独立 APK，不是 Inspector 的运行依赖。

## Limitations

- Accessibility tree **不是完整 View hierarchy**；Compose 暴露 Semantics，可能合并节点。
- 测量是 Accessibility bounds 之间的距离，不保证等同圆角、阴影等实际着色轮廓间距。装饰性且未暴露节点的元素无法成为邻居；置信度是启发式质量分数，不是统计正确率。
- Freeze 保存所选 Item 的截图和历史数据，不冻结目标 App，也不保存全屏；不跨进程重启持久化。
- 不能读取真实 drawable、cornerRadius、stroke、elevation、padding/margin、字体大小/粗细、Compose Modifier、Material theme token。
- 颜色是截图估计；渐变、图像、遮挡和色彩管理影响统计，Top 3 不代表源码颜色。
- FLAG_SECURE 阻止取色，不绕过系统限制。
- Canvas / OpenGL / 游戏 / 视频未提供虚拟子节点时，只能看到整个 Surface / View。
- WebView 仅支持其 Accessibility 节点，不保证 HTML DOM ID、CSS 或 DOM hierarchy；不使用 CDP。
- ID 与语义完整性取决于目标 App；陈旧、不可见、超过遍历上限的节点不保证可选择。
- MVP 面向默认 display；外接多 display、放大镜和特殊厂商缩放未完整验证。无法确认截图几何关系时显示 unavailable。
- API 30–33 整屏截图可能受其他 App 或服务 overlay 影响；只能隐藏本工具自己的浮窗。
- 不包含 OCR、AI、远程控制、录制回放、服务器、云上传或自动测试生成。
- 界面目前为英文，未完整本地化。日志不打印节点文本、密码或完整截图。

## Official references

- [AccessibilityService screenshot and overlay APIs](https://developer.android.com/reference/android/accessibilityservice/AccessibilityService)
- [AccessibilityNodeInfo](https://developer.android.com/reference/android/view/accessibility/AccessibilityNodeInfo)
- [Android 17 / API 37](https://developer.android.com/about/versions/17)
- [AGP and API compatibility](https://developer.android.com/build/releases/about-agp)
