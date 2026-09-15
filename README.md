# Android Visual UI Inspector

运行在 Android 设备上的 UI 检查工具，使用 Kotlin、Jetpack Compose / Material 3、AccessibilityService 和系统截图 API。安装后独立运行，不需要 root、Shizuku、ADB server 或 Appium server。

## Features

- 尺寸使用无障碍节点的实际屏幕像素；dp 是按当前密度换算的值，不代表能还原源码布局常量。快速结果在距整数 dp 不超过半个像素时标记为 `≈`，详情尺寸保留精确换算值。例如 306 dpi 下，61 px = 31.90 dp，快速显示 `≈32 dp`，px 始终保持 61。
- 应用图标与悬浮球使用用户提供的放大镜图片，底色为白色。

- 核心流程：**点悬浮球 → 点 Item → 立即看尺寸、间距和颜色**，连续检查无需关闭旧结果。
- 唯一常驻入口为 48dp 半透明悬浮球，可拖动、左右吸边并记住位置；长按只有测距、取色、设置、停止四项。
- 蓝色尺寸标尺、默认最多两个有意义的橙色间距、主色色块与 HEX，dp 主值 / px 次值同时可见。
- Quick Result Card 只有 Details / ×；手机展开底部详情，平板展开侧面详情，仅展示尺寸、周边间距和颜色。
- Details 顶部 Smaller / Larger 在同一点候选中即时修正选择；Spacing 行可点击切换邻居。
- A/B 测距和带截图放大镜的单像素取色均在目标 App 上完成，无独立操作 Activity。
- 结果自动保留 bounds、间距、颜色与 Item 截图；无需 Freeze / Unfreeze。
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
3. 返回首页，看到 **Enabled** 后点击 **Start Inspector**；首页自动 finish，返回之前的界面。
4. 首次只需关闭一次短提示，然后点悬浮球、点待检查元素。

服务通过 `BIND_ACCESSIBILITY_SERVICE` 保护，XML 启用 `canRetrieveWindowContent`、`canTakeScreenshot`、`flagReportViewIds` 和 `flagRetrieveInteractiveWindows`。不申请普通悬浮窗权限，不绕过无障碍授权。

## Usage

点悬浮球直接进入完全透明的选择层，顶部轻提示 Tap an item to inspect。抬手完成选择后立即移除触摸捕获，标尺不拦截触摸。先显示尺寸与间距，再自动补充颜色。每个 Item 两次点击即可；悬浮球始终代表重新选择。

- **Details → Smaller / Larger**：按原点击点候选的视觉面积修正，跳过重复 bounds，不依赖 Parent / Child 层级。
- **Details → Spacing**：点击邻居行直接更新尺寸、间距和颜色。
- **× / Back**：× 只隐藏当前结果；Back 按 Selecting → 取消、Details → 收起、Result → 隐藏处理。Idle 不拦截 Back，也不关闭无障碍服务。
- **长按 → Measure between two items**：提示选择第一个、第二个元素，完成后提供 Done / Measure again。两个节点须处于同一未改变的窗口与 density。
- **长按 → Color picker**：点屏幕像素得到真实色块、HEX / RGB 和 Done。按住可看进入取色时的截图放大预览，松手重新采样最终像素；不要求独立语义节点。
- **长按 → Settings**：主单位 dp / px（只改变视觉主次）、Show all spacing、Details 展开偏好。本地保存，首次为 dp / 双单位显示 / 最多两个间距 / 收起详情。
- **长按 → Stop inspector**：才关闭全部 Inspector 悬浮界面。
- **保存结果**：普通结果本身即快照，直到重新选择或关闭；页面变化后保留测量、颜色与 Item PNG，并移除失效坐标上的标尺。旧快照不继续抓屏。


## Supported Information

| Property | Source |
| --- | --- |
| Width/Height px | `getBoundsInScreen()` |
| Width/Height、间距 dp / px | DimensionValue 保存 Float，显示最多 2 位小数，去掉尾零 |
| 背景色候选、文字色候选 | 渲染像素估计，非控件源码属性；无法可靠区分时明确提示 |

颜色详情不再展示中心像素及多个颜色排名。背景候选要求主色占比至少 70%；有文字语义时，文字候选从重复出现、与背景有明显对比的颜色簇中选择。图标、渐变、阴影和抗锯齿仍可能影响判断，因此始终标注 estimated，不把估计值当作真实 `textColor`。单像素取色器可用于人工核对。

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

## Presentation policy

原始 Neighbor 数据不变。`SpacingPresentation` 以 overlap、confidence 和 gap 提示主要布局方向；明显纵向/横向时优先对应两侧，否则取最近两个。Show all spacing 只放开展示数量，标签仍须通过避让检查。

`LabelPlacementEngine` 优先安放宽高，再放间距和主色；考虑屏幕边缘、所选 bounds、邻居、结果卡和已放置标签。次要标签没有空间时不画，不与宽高重叠。小于等于 32dp 的小 Item 使用外部紧凑尺寸 chip；大于所在窗口约 75% 的容器使用框和尺寸 chip，避免超长标尺。

`ResultCardPlacement` 在屏幕上下两侧和左右边缘比较位置，优先避开当前 Item。色块至少 24dp、有边框，白色也可辨认。混合颜色显示 Mixed colors，截图失败显示普通语言；不展示节点技术属性。

## Architecture

```text
app/src/main/java/com/xiaoyue/uiinspector/
├── MainActivity.kt
├── accessibility/   # Service lifecycle and connection state
├── inspector/       # State machine, tree snapshots, ranking, orchestration
├── overlay/         # Bubble, transparent capture, placement, rulers, quick card
├── interaction/     # Sealed UI state, Back policy, preferences, orchestration
├── measurement/     # DimensionValue, directional neighbors, edge gaps
├── analysis/        # SelectedItemAnalysis, staged analyzer, PNG snapshot, summary
├── screenshot/      # API guards, fallback, buffer ownership, coordinates
├── color/           # Pure Kotlin sampling and color statistics
├── util/            # Immutable bounds, density, clipboard, locators
└── ui/              # Home permission/start/settings/diagnostics; grouped Details
app/src/test/        # JVM unit tests
app/src/debug/       # Debug-only screenshot validation activity
qa-target/           # Optional separate deterministic test app
docs/VALIDATION.md   # Validation record
```

Service 保留系统生命周期与事件能力，仅新增 Back / Escape 转交；InspectorInteractionController 负责 UI 状态与异步协调，旧 InspectorController 名称为兼容别名。OverlayController 管理 TYPE_ACCESSIBILITY_OVERLAY。服务浮窗使用原生 View，主应用使用 Compose Material 3。

NodeTreeBuilder 仅在点击时遍历，最多 5000 节点 / 100 层。遍历后释放 API 30–32 节点，UI 只保存不可变 NodeSnapshot / Bounds。排序集中于 NodeFinder：包含点击点 → 可见优先 → 面积小 → 深度大 → ID / clickable / 文本语义。窗口先筛选 bounds，按 active、focused、layer 排序，排除本工具与 accessibility overlay。

高亮将屏幕 bounds 减去 `getLocationOnScreen()` 转成 Canvas 局部坐标，绘制 2dp 边框。触摸使用 `rawX/rawY`。窗口处理系统栏 / cutout，但系统保留手势区域仍可能不可捕获。

空闲不遍历、不截图。事件仅做 250 ms debounce 快照标记。配置改变保留旧数据卡片并移除旧坐标标尺；悬浮球按记忆的屏幕侧边与纵向比例重新放置。Back 使用可聚焦且非触摸模态的控件窗口、API 33+ 返回回调及无障碍 Back/Escape 按键转交；其他按键不截获。悬浮球自身设置小范围手势排除，防止贴边拖动误触系统返回。

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

52 项 JVM 测试包含原有 41 项及 InteractionPolicyTest、OverlayPresentationTest，新增覆盖返回层级、候选修正、双单位视觉主次、主要间距筛选、卡片与标签避让、空间不足隐藏次要标签。设备截图验证窗口排除覆盖层、整屏临时隐藏覆盖层、FLAG_SECURE。Debug 专用 Activity 在已授权的服务进程内执行，避免 Instrumentation 强制停止服务；不会自动开启权限，Release 不含这些入口。

新增确定性测量页面及实机分析检查：

```sh
adb shell am start --user current -n com.xiaoyue.uiinspector/.MeasurementValidationActivity
adb shell run-as com.xiaoyue.uiinspector --user 10 cat files/measurement-validation.txt
```

验证尺寸 328 × 48dp、上下 24dp / 左右 16dp（允许布局转整数造成的像素误差）、四方向资源 ID、主色 #C7C6CA 及保存的 PNG。完成后留下测试页面和悬浮球供手动 Smoke Test，包含小图标 fixture。也可直接启动 fixture 并传 `--ez measurement true`。

调试 APK 还提供只读状态探针，方便确认 ADB 触摸后的实际 UI 状态，无需再启动 Activity：

```sh
adb shell am broadcast --user current -n com.xiaoyue.uiinspector/.InteractionProbeReceiver
adb shell run-as com.xiaoyue.uiinspector --user 10 cat files/interaction-validation.txt
```

启动安全测试窗口（普通窗口去掉 `--ez secure true`）：

```sh
adb shell am force-stop --user current com.xiaoyue.inspectorfixture
adb shell am start --user current -n com.xiaoyue.inspectorfixture/.FixtureActivity --ez secure true
```

QA Target 是独立 APK，不是 Inspector 的运行依赖。

## Limitations

- Accessibility tree **不是完整 View hierarchy**；Compose 暴露 Semantics，可能合并节点。
- 测量是 Accessibility bounds 之间的距离，不保证等同圆角、阴影等实际着色轮廓间距。装饰性且未暴露节点的元素无法成为邻居；置信度是启发式质量分数，不是统计正确率。
- 结果快照保存所选 Item 的截图和历史数据，不冻结目标 App；不跨进程重启持久化。取色放大镜临时保存进入取色时的屏幕预览，退出即释放，动态页面最终像素可能与预览不同。
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
