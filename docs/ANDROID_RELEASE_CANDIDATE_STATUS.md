# Android Release Candidate Status

## Qualification state

**Android Release Candidate — Automated Qualification Passed, Physical-Device Gates Pending**

ANDROID-008 additionally exercises deterministic system acceptance for Library recreation/stale
navigation and storage/provider failure boundaries. Automated acceptance does not change the
physical-device or production-signing status.

The automated authority is `scripts/verify-android-release-candidate.ps1`. Generated evidence is
written under `build/android-release-candidate-evidence/` and is intentionally not tracked.

## Verified baseline and build

- Qualification baseline: `develop` at `8000b9810b0fea4cb592cb5d3b531a239892963a`, equal to
  `origin/develop`; stable tag `v0.9.7-rc1` exists.
- Release configuration: non-debuggable, R8/minification enabled, resource shrinking enabled.
- Manifest: no requested permissions, no cleartext/network/camera/microphone/storage capability,
  backup disabled, and only the launcher activity is exported.
- Tests: Root 391 suites / 2,051 tests; Desktop 230 / 1,326; Android 7 / 44; total 628 suites /
  3,421 tests with zero failures, errors, or skipped tests.
- Startup graph and canonical Library, Study, editor, package-operation and scoped-Study service
  composition probes pass. Physical launch is not claimed.

## Artifacts

| Artifact | Size (bytes) | SHA-256 | Signature |
| --- | ---: | --- | --- |
| `android-debug.apk` | 13,667,067 | `938a2559c1c342d5da65292746b062bff71f6fff707a04f02a40627739a31b48` | DebugSigned |
| `android-release-unsigned.apk` | 1,646,447 | `7faabb76ddbceb2bd2ead19af88cadc0bd956200a893a6fd3f1b258adba32048` | NotSigned |
| `android-release.aab` | 4,332,035 | `98e16e0c757199d674a0649f9369dfa22fb6063b6d710855bd5f104fa343724b` | NotSigned |

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
