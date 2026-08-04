# Android Release Candidate Status

ANDROID-UAT-006 automated full build passes: Root 393 suites / 2,059 tests, Desktop 230 / 1,326,
Android 11 / 63, total 634 / 3,448 with zero failures/errors/skipped. Root startup now has typed
Bootstrapping/Ready/Failure rendering, one application graph owner, deterministic root navigation,
and isolated Library/Study feature states. Physical interaction remains unverified: install and
cold launch were issued, but keyguard-blocked UI automation hung and the ADB transport disconnected.
RC qualification passed against the explicitly validated local baseline HEAD.

ANDROID-UAT-005 automated qualification passes: Root 393 suites / 2,059 tests, Desktop 230 / 1,326,
Android 10 / 57, total 633 / 3,442 with zero failures/errors/skipped. On device `24090RA29C`, an
install-over-existing-data cold launch completed `study_initial_load` for the existing 990-content
active session in 3,969 ms (repeat: 4,059 ms), with no fatal/ANR and no sustained GC events during
the 35-second observation. Interactive media/Study journey remains gated by the device keyguard.

ANDROID-UAT-004 automated qualification passes: Root 393 suites / 2,056 tests, Desktop 230 / 1,326,
Android 10 / 54, total 633 / 3,436 with zero failures/errors/skipped. Debug APK is 20,647,134 bytes;
unsigned release APK is 1,732,115 bytes; unsigned release AAB is 4,547,185 bytes. Device timings,
real images, MediaPlayer Playing and audible output remain pending because ADB found no device.

ANDROID-UAT-003 automated qualification passes with Root 392 / 2,054 tests, Desktop 230 / 1,326,
Android 9 / 51, total 631 / 3,431 and zero failures/errors/skipped. Debug APK is 20,630,750 bytes;
unsigned release APK is 1,732,115 bytes; unsigned release AAB is 4,545,739 bytes. Physical device
media playback and Study/Review journey remain pending because ADB reported no attached device.

ANDROID-010 changes native navigation and presentation. Physical media and end-to-end Study
interaction remain explicit device gates whenever no ADB device is attached.

Final ANDROID-010 qualification: Root 392 suites / 2,054 tests, Desktop 230 / 1,326,
Android 9 / 48, total 631 / 3,428 with zero failures/errors/skipped. Debug APK is 20,597,982
bytes (`20bb87b95e6f265a7ec4e4ed943c32711c22de0d0b9dac4abcd1ac7aefb1bb15`); unsigned release
APK is 1,679,639 bytes (`1a296615796aa7faaa49646084ba4eb5d9fdcbb7ca8376afc798754ca928f90e`);
unsigned release AAB is 4,425,125 bytes (`e0ba3d4bc57fb8127e4cfe7d371d5a0bbd96aebeb3b9eda76021f8cbac409392`).

## Qualification state

**Android Release Candidate — Automated Qualification Passed, Physical-Device Gates Pending**

ANDROID-008 additionally exercises deterministic system acceptance for Library recreation/stale
navigation and storage/provider failure boundaries. Automated acceptance does not change the
physical-device or production-signing status.

ANDROID-UAT-001 passed install-over-existing-data startup on device `24090RA29C`: MainActivity
launched, the process remained alive, and bounded logcat contained no fatal exception,
`NoSuchMethodError`, or `Files.readString`. Remaining physical UAT gates are still pending.

The automated authority is `scripts/verify-android-release-candidate.ps1`. Generated evidence is
written under `build/android-release-candidate-evidence/` and is intentionally not tracked.

## Verified baseline and build

- Qualification baseline: `develop` at `4937d9076566c320b4f87566baf7fce01db4da26`, equal to
  `origin/develop`; stable tag `v0.9.7-rc1` exists.
- Release configuration: non-debuggable, R8/minification enabled, resource shrinking enabled.
- Manifest: no requested permissions, no cleartext/network/camera/microphone/storage capability,
  backup disabled, and only the launcher activity is exported.
- Tests: Root 392 suites / 2,054 tests; Desktop 230 / 1,326; Android 9 / 46; total 631 suites /
  3,426 tests with zero failures, errors, or skipped tests.
- Startup graph and canonical Library, Study, editor, package-operation and scoped-Study service
  composition probes pass. Physical launch is not claimed.

## Artifacts

| Artifact | Size (bytes) | SHA-256 | Signature |
| --- | ---: | --- | --- |
| `android-debug.apk` | 13,667,067 | `5302505661c881543e321399dce4cba00190fc02830fcf5b1bce1353aaf54664` | DebugSigned |
| `android-release-unsigned.apk` | 1,646,447 | `9c171dcb2684026105894237e6d6d67a2a61d2f5240349563098c96b15d12087` | NotSigned |
| `android-release.aab` | 4,331,249 | `60e10cf8a70bf5971b802f8650c718f89b31d386cb31233d9ffa7671a48ba71d` | NotSigned |

The release APK contains four copies of the dependency-provided
`libandroidx.graphics.path.so`: `arm64-v8a`, `armeabi-v7a`, `x86`, and `x86_64`. No ABI is removed
without device-compatibility evidence. Version metadata remains canonical repository configuration:
application ID `vn.loi.learning.android`, version code `1`, version name `1.0`, min SDK 26, target
SDK 37, compile SDK 37.

## One-pass physical-device checklist

Run once on a representative phone and tablet using a signed release candidate:

1. Install, first launch, cold/warm start, Home and Library navigation.
2. Import from real document providers, including a representative large package; search, detail,
   edit with IME, export, verify, upgrade and uninstall.
3. Start package, lesson and selected-content Study; exercise Typing, Multiple Choice, Listening,
   Image Recall and Example Completion plus Practice, Undo and completion.
4. Verify speaker output and real images; rotate, background/restore, process death and predictive
   Back without duplicate operation or stale navigation.
5. Verify TalkBack, keyboard/focus, font scale 1.3–2.0x, portrait/landscape and responsive tablet/
   foldable layouts.
6. Exercise low-storage behavior, backup/restore and a long Practice session; observe battery,
   memory, ANR and crash behavior.
7. Apply externally managed production signing, upload the AAB, then complete Play Integrity and
   Play pre-launch report. Do not mark Play-ready before these gates pass.
