# Development workflow

## Canonical baseline

The canonical baseline is the current remote `develop` commit.

Before applying a batch:

```powershell
git switch develop
git pull --ff-only
git status --short
git rev-parse HEAD
```

The working tree must be clean and `HEAD` must match the batch manifest.

## Batch package

Each development increment is delivered as:

```text
BatchXX_APPLY.zip
├── payload/
├── apply_batch.ps1
├── manifest.json
└── README.txt
```

## Apply contract

`apply_batch.ps1` must:

1. Verify it is run from the repository root.
2. Verify branch `develop`.
3. Verify the expected base commit.
4. Refuse a dirty working tree.
5. Verify payload SHA-256 checksums.
6. Back up replaced files.
7. Apply the complete payload.
8. Run `clean test`.
9. Restore the backup when the build fails.
10. Leave a result record under `.batch-results/`.

## Successful batch

After `BUILD SUCCESSFUL`:

```powershell
git status
git add <batch files>
git commit -m "<meaningful message>"
git push
```

The pushed `develop` HEAD becomes the next baseline.

## Failed batch

Do not commit failed work. Preserve the PowerShell output and `.batch-results` record for diagnosis. The apply script is responsible for restoring files when the build command fails.

## Documentation

Documentation changes are committed with the capability they describe. ZIP snapshots are delivery or archival artifacts, not baselines.
