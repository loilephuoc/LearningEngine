# AI-Assisted Development Contract

## Startup

Before changing code:

1. Confirm branch, commit, and working-tree status.
2. Read `.ai/HANDOFF.md`, `.ai/CURRENT_SESSION.md`, and the relevant documents in `docs/`.
3. Inspect the actual declarations, call sites, tests, and Gradle configuration involved.
4. Produce a concise Startup Report containing current state, verified completed work, current objective, smallest next increment, and files likely to change.
5. Do not generate code before the Startup Report.

## Source of truth

- Current Git source and tests are authoritative.
- Documentation is supporting context and must be corrected when it disagrees with source.
- Never infer an API, module, class, dependency, or completed milestone from chat memory alone.
- Do not use `LearningEngine_SOURCE_ONLY.zip` or `docs.zip` as synchronization mechanisms.

## Increment policy

- Work in one narrow, buildable increment.
- Preserve behavior outside the requested scope.
- Avoid broad refactoring unless explicitly approved.
- Inspect existing code before introducing new abstractions.
- Clearly distinguish verified, implemented-but-unverified, planned, and unknown states.

## File delivery

For every changed existing file, provide:

1. Relative path.
2. A one-line PowerShell command that assigns `$path`, locates `idea64.exe` from a running process or common installation locations, and opens the file with `Start-Process`.
3. The complete replacement content.

For a new file, the same one-line command must create the parent directory and empty file before opening it.

Never provide partial patches as the primary delivery format. Do not use `Get-Content` merely to display a file.

## Verification

- Never claim `BUILD SUCCESSFUL` without the user's explicit build output.
- After each verified increment, update `.ai/CURRENT_SESSION.md` and `docs/04_PROGRESS.md`.
- Update `.ai/HANDOFF.md` when the next objective or constraints change.
- Update `docs/02_ARCHITECTURE.md` only for durable architecture changes.
- Update `docs/CHANGELOG.md` only for meaningful milestones.
- Commit source, tests, and related documentation together.

## Safety

Do not recommend destructive Git or filesystem cleanup commands such as `git clean -fd` unless the untracked-file list has first been reviewed and the user explicitly approves deletion. Prefer targeted restore/remove commands.
