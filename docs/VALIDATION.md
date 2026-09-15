# Validation record

## Visual Inspector update — 2026-09-14

Modified the existing project in place. `gradlew.bat test assembleDebug :app:lintDebug` completed successfully. **41 JVM tests passed, 0 failures / errors** (18 existing, 23 new). New suites: DimensionValueTest 4, NeighborFinderTest 8, SpacingCalculatorTest 5, SelectedItemAnalyzerTest 3, ColorAnalyzerTest 3. The A/B/C test deliberately finishes C before B and A and verifies only C is published.

Installed both debug APKs on DUET 13M9611, API 37, display 3504 × 2190, density 1.9125, user 10. Existing manually enabled Accessibility binding remained available. No permission settings changed.

`MeasurementValidationActivity`: **3 / 3 passed** using production NodeTreeBuilder, SelectedItemAnalyzer, NeighborFinder, ScreenshotProvider and ColorAnalyzer in a freeform fixture window:

| Quantity | Actual result |
| --- | --- |
| Selected bounds | `[1103,884][1730,975]` |
| Size | 627 × 91 px / 327.84 × 47.58 dp |
| Top gap | 47 px / 24.58 dp, top_neighbor |
| Bottom gap | 46 px / 24.05 dp, bottom_neighbor |
| Left gap | 32 px / 16.73 dp, left_neighbor |
| Right gap | 31 px / 16.21 dp, right_neighbor |
| Main rendered color | #C7C6CA / RGB(199, 198, 202) |
| Captured image | Nonempty retained Item PNG |

The fixture requests 328 × 48dp and T/B 24dp, L/R 16dp. Integer layout conversion at fractional density causes pixel-level differences. The inspector reports actual screen bounds, without rounding results to requested design values.

Repeated `ScreenshotValidationActivity`: **3 / 3 passed**, including window screenshot overlay exclusion, display screenshot overlay hiding and secure-window rejection.

Manual ADB touch/screenshot review confirmed the one-selection flow, aligned bounding box, W/H rulers, all four directional gaps, actual color swatch, and compact card with size/spacing/color visible without scrolling. Details shows measurement first, neighbor navigation switches selection and updates color, and Advanced is initially collapsed and still exposes resource ID, class, text and state flags. Local screenshots are under ignored `app/build/smoke-*.png`; they are not committed because other device windows may appear behind the fixture.

The update retains the coverage limitations below. Freeform translation is exercised on DUET; this is not certification of every ChromeOS/phone/multi-display configuration.

Additional touch smoke checks passed:

- Freeze retained the original button bounds, all gap values and #C7C6CA while the fixture EditText changed. Copy summary was pasted into that EditText and included Frozen status, both units, four gaps, HEX and RGB.
- A ↔ B selected Save and Bottom through accessibility; two boxes and a purple vertical ruler displayed **46 px / 24.05 dp**, matching the automatic gap.
- Independent Picker at screen pixel **(1150, 915)** returned **#C7C6CA / RGB(199, 198, 202)**.
- Repeated selection and neighbor navigation updated measurements and color without a crash. The automated race test, rather than this manual smoke, verifies out-of-order completion.
- After an APK replacement, launching validation before service reconnection initially reported an unavailable service. The Debug measurement entry point now waits up to 10 seconds for the existing connection; no permission is granted by the test.

Dates: 2026-09-11 and 2026-09-12. Commands run from the repository root on Windows using JDK 21.

## Build and JVM checks

- `gradlew.bat :app:assembleDebug`: successful throughout phases 1–7.
- `gradlew.bat :app:testDebugUnitTest`: **18 passed, 0 failed**.
- `gradlew.bat :app:lintDebug`: successful, no errors. Remaining warnings concern dependency updates, English strings, explicit screen-coordinate LEFT gravity and programmatic touch views.
- `gradlew.bat :qa-target:assembleDebug`: successful.
- On 2026-09-12, `:app:testDebugUnitTest :app:lintDebug :app:assembleRelease` completed successfully; 18 JVM tests passed. Release merged manifest contains neither ScreenshotValidationActivity nor the debug fixture package query. Release APK is unsigned; the installable development artifact remains app-debug.apk.
- The original instrumentation APK compiled, but its runner was replaced by a Debug-only validation Activity after device testing exposed process-lifecycle interference (see below).
- Initial SDK 37 discovery failed with AGP 9.1.0; fixed by upgrading to 9.1.1. SDK 37.0 revision 2 installed.
- Initial Lint failure was an unescaped local Windows drive separator in `local.properties`; fixed. No baseline suppresses errors.

## Googlebook manual device checks

Device: Lenovo Googlebook 15 (`ruby`), Android API 37, 2880 × 1800, density 1.5. User manually enabled the service; `dumpsys accessibility` confirmed it bound.

| Case | Result |
| --- | --- |
| Installation and connected home status | Passed with `adb install -r`, launch and screenshot |
| Floating overlay above another app | Passed |
| TextView in another app | Passed, properties and screen bounds displayed |
| QA Target Button in freeform window | Passed, resource ID / text / description / states displayed |
| Screen coordinate and dp | `[553,531][1078,675]`, 525 × 144 px = 350 × 96 dp |
| Highlight alignment | Passed by screenshot review; frame matches button edges |
| Copy ID | Pasted into QA Target EditText: `com.xiaoyue.inspectorfixture:id/color_button` |
| Parent / Child / Candidate | Passed; TextView (1/3) ↔ LinearLayout card (2/3), highlight bounds updated |
| API 34+ window screenshot | Used successfully while Inspector overlays were present |
| Flat button color with central text | Expected `#C7C6CA`, dominant `#C6C5C9` at 97.8%; RGB error = 1/channel |
| Center pixel | `#202124`, matching the button text rather than background |
| FLAG_SECURE | Accessibility properties remained; color stated target prevents screenshots; no crash |
| Landscape / freeform desktop layout | Passed on the device's native landscape desktop |

Screenshots from these checks are local development evidence under ignored `app/build/`, not published to avoid including unrelated apps behind the fixture.

## Second device and automated suite

During testing the connected device changed to DUET 13M9611 (`sapphire`), API 37, 3504 × 2190, density override 306 dpi. The device reports the same ADB serial as the previous device, so model and resolution were checked separately.

On 2026-09-12 the user enabled Accessibility on DUET. The original instrumentation runner force-stopped the target app, which caused the system to mark the Accessibility connection as crashed. The test's null-service error therefore did not mean the user had omitted authorization. Replaced the runner with `ScreenshotValidationActivity` in `src/debug`, running within the already-authorized app process. Reinstalling the APK restored the existing service binding without changing secure settings. The validation entry point and fixture package query are absent from Release builds.

Executed:

```text
adb shell am start --user current -n com.xiaoyue.uiinspector/.ScreenshotValidationActivity
adb shell run-as com.xiaoyue.uiinspector --user 10 cat files/screenshot-validation.txt
```

Result at 09:42 on DUET: **3 / 3 passed**.

| Automated check | Result |
| --- | --- |
| Window capture with a full red accessibility overlay | PASS, dominant `#C7C6CA`, exactly matching fixture |
| Display capture with overlay temporarily hidden | PASS, dominant `#C7C6CA`, exactly matching fixture |
| FLAG_SECURE | PASS, explicit screenshot-prevention error rather than pixels |

The center sampled an antialiased part of the button text (`#8B8A8E`); dominant correctly remained the background color. Report content ends with `DONE`. All three cases use the production ScreenshotProvider and ColorAnalyzer. The report contains only fixture diagnostics.

Repeated all three checks at 09:43–09:44 after `cmd window user-rotation lock 1`. WindowManager reported rotation 1 / ROTATION_90 for the physical display. **3 / 3 passed again**, with exact dominant `#C7C6CA` for both capture modes. Restored and verified the original `lock 0` rotation setting afterward. This validates screenshot geometry after rotation; it is not an exhaustive test of every phone/tablet UI layout.

The suite covers window capture excluding a full red accessibility overlay, display capture hiding that overlay, and secure-window rejection. It never changes secure settings or grants Accessibility itself.

## Interaction simplification — 2026-09-15

Continued the existing project and retained NodeFinder, NeighborFinder, measurement,
screenshot and color-analysis engines. The interaction controller now presents a
single bubble, transparent selection, a compact result card and optional details.

`gradlew.bat test assembleDebug :app:lintDebug :app:assembleRelease --console=plain`
passed. **52 JVM tests passed, zero failures**; Lint reported **0 errors, 28 warnings**.
Release manifest inspection found no validation activities or InteractionProbeReceiver.
The Release APK is unsigned; Debug is the installable artifact.

Device: DUET 13M9611, API 37, 3504 × 2190, density 1.9125, user 10.
Debug APK installed successfully using the existing Accessibility authorization.

| Real-device interaction | Evidence / result |
| --- | --- |
| First-run welcome | One short instruction and Got it; not shown on subsequent starts |
| Select item | Bubble → Save; transparent capture removed after selection |
| Continuous selection | Save → Bottom → Right, each with bubble + item taps, no Close required |
| Quick result | Dual-unit size, two spacing rows, bordered color swatch, Details and Close |
| Details and Back | Grouped panel; Back collapses to quick result, another Back returns to Idle |
| Smaller / Larger | Save `[1103,884][1730,975]` → window `[606,348][3128,1924]` → original Save |
| Small item | 45 × 45 px icon; compact size chip outside the item, no long rulers |
| Large container | Window outline and compact size chip, no full-length dimension rulers |
| Pair measurement | Menu → A → B; vertical gap 46 px / 24.05 dp, Done and Measure again |
| Color picker | Real screenshot magnifier; selected fixture background returned `#C7C6CA` with Copy / Done |
| Bubble drag and edge snap | Moved left to right; right edge and normalized Y `0.38351595` retained after reinstall/restart |
| Home Start | Starts Idle bubble, finishes MainActivity and returns to the previous fixture |
| Crash log | No entries in the crash buffer during final checks |

The edge-drag test initially triggered the system Back gesture. A gesture exclusion
rectangle limited to the bubble fixed this; dragging and snapping then passed.
The readonly Debug interaction probe checks state without launching an Activity.
Avoid `uiautomator dump` while validating service continuity: on this device its
automation session temporarily disconnected the service, which rebound afterward.
No secure settings were changed.

Production-engine regressions repeated on DUET: **measurement 3/3** (bounds and
units, all four gaps, exact color and retained PNG) and **screenshots 3/3** (window
excludes overlays, display capture hides overlays, secure window rejected).
The Save reference remains 627 × 91 px / 327.84 × 47.58 dp, gaps T/B/L/R
47/46/32/31 px, rendered background `#C7C6CA`.

Local screenshots are under ignored `app/build/ux-*.png`; they are not published
because unrelated applications are visible behind the fixture. Phone panel layout
and API 30–33 still require runtime coverage; tablet smoke tests are not evidence
that every device, orientation or gesture-navigation implementation is covered.

## Remaining coverage

API 30–33 requires a separate physical device/emulator run; compilation and Lint validate guarded API usage, but are not runtime proof. External displays, magnification, OEM-specific scaling, additional ChromeOS variants and exhaustive app-specific semantics are not certified.
