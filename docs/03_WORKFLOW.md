# Development Workflow

## Branch model

- `main`: stable milestone baseline (`fa6b498` at audit time).
- `develop`: active integration branch (`79af828` at audit time).
- Use `feature/*` when a change is large or risky enough to require isolation.

## Start a session

```powershell
git switch develop
git pull
git status
```

Read:

1. `.ai/HANDOFF.md`
2. `.ai/CURRENT_SESSION.md`
3. Relevant architecture/progress documents
4. Relevant source and tests

## Implement and verify

1. Select one narrow increment.
2. Change complete files only.
3. Add or update focused tests.
4. Run the narrowest useful test task, then the full build.
5. Wait for the user's explicit `BUILD SUCCESSFUL` output.
6. Update progress and handoff files.

Full build:

```powershell
.\gradlew build
```

Desktop run:

```powershell
.\gradlew :desktop:run
```

## Commit

```powershell
git add -A
git commit -m "type(scope): concise description"
git push
```

Preferred types: `feat`, `fix`, `refactor`, `test`, `docs`, `build`, `chore`.

## Promote a milestone

After verification and review:

```powershell
git switch main
git pull
git merge --no-ff develop
git push
git switch develop
```

## Documentation policy

- Every verified increment: `.ai/CURRENT_SESSION.md`, `docs/04_PROGRESS.md`.
- Handoff/next objective changed: `.ai/HANDOFF.md`.
- Durable architecture change: `docs/02_ARCHITECTURE.md`.
- Meaningful milestone: `docs/CHANGELOG.md`.

## Destructive-command policy

Before any cleanup command:

```powershell
git status --short
git clean -nd
```

Review the preview first. Never run `git clean -fd`, `reset --hard`, or broad deletion commands merely to restore a clean status when untracked source/data may exist.
