# Desktop Release Candidate Status

Status: **Automated qualification available; manual gates pending.** Desktop is not frozen.

- Source commit, environment, exact XML counts, runtime/audio results, artifact paths, sizes,
  hashes, signatures, and timestamp are recorded in
  `build/release-candidate-evidence/release-candidate-evidence.json`.
- Automated gates cover clean tests, the ProGuard release portable image, bundled launcher/JVM,
  runtime integrity, Java Access Bridge, MP3 provider discovery, and release EXE/MSI packaging.
- Manual local gates remain: Study UI UAT, real-speaker audio, restart/persistence, long Practice,
  representative content, visual/focus/screen-reader behavior, large-package verification, and
  backup/restore acceptance.
- External gates remain: clean-machine execution, MSI install/upgrade/uninstall, code signing, and
  Defender/SmartScreen reputation.

Rerun with the approved JDK:

```powershell
$env:JAVA_HOME = 'C:\Users\M72Q\.jdks\temurin-21.0.11'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
.\scripts\verify-windows-release-candidate.ps1
```
