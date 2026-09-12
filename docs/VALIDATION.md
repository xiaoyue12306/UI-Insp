# Validation record

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

## Remaining coverage

API 30–33 requires a separate physical device/emulator run; compilation and Lint validate guarded API usage, but are not runtime proof. External displays, magnification, OEM-specific scaling, additional ChromeOS variants and exhaustive app-specific semantics are not certified.
