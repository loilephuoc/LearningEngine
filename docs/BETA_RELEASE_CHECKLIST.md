# Desktop Beta Release Checklist

This checklist records Phase 5 release-candidate evidence. Workflow authority remains
[`../AGENTS.md`](../AGENTS.md); Phase completion criteria remain in [`ROADMAP.md`](ROADMAP.md).

## Automated repository gate

- [ ] Use a full JDK 21 with `jpackage.exe` and run
  `powershell -File scripts/verify-windows-beta.ps1`.
- [ ] Confirm `clean test`, MSI, and EXE packaging pass.
- [ ] Archive `build/beta-evidence/windows-beta-evidence.json` with the candidate.
- [ ] Confirm artifact names, sizes, SHA-256 hashes, and expected signing status.
- [ ] Confirm the Unicode writable-directory probe passes.

## Clean-machine Windows gate

- [ ] Use a supported clean Windows VM/user account with a Unicode username.
- [ ] Run the harness without install authority first.
- [ ] Run with `-InstallSmoke` only on the disposable verification machine.
- [ ] Launch the installed app, complete/skip onboarding, and verify restart.
- [ ] Import a representative OPD3 package and complete one Study review.
- [ ] Export diagnostics and confirm no username or learning content is exposed.
- [ ] Create a manual backup, change data, restore it, and confirm safety backup/restart.
- [ ] Uninstall and verify user data remains intact.
- [ ] Reinstall and verify the preserved profile opens without migration.

## Upgrade gate

- [ ] Supply an approved previous-version MSI and record its commit/hash.
- [ ] Verify upgrade preserves data/config and does not rerun onboarding.
- [ ] Verify downgrade behavior is documented; do not claim compatibility without evidence.

## Known release limitations

- Artifacts are unsigned until signing authority and secrets are supplied.
- Cloud/scheduled backup, automatic retention, and cross-device merge are not supported.
- Localization does not yet cover every feature screen.
- Clean-machine, install/uninstall, and upgrade boxes require Product Owner evidence; local
  build results cannot substitute for them.
