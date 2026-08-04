param(
    [switch]$AllowTrackedChanges
)

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest
$root = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path

Push-Location $root
try {
    $branch = (git branch --show-current).Trim()
    $head = (git rev-parse HEAD).Trim()
    $origin = (git rev-parse origin/develop).Trim()
    if ($branch -ne "develop") { throw "Expected develop branch." }
    if ($head -ne $origin) { throw "HEAD must equal origin/develop." }
    if (-not (git tag --list v0.9.7-rc1)) { throw "v0.9.7-rc1 is missing." }
    $tracked = @(git status --porcelain --untracked-files=no)
    if ($tracked.Count -and -not $AllowTrackedChanges) { throw "Tracked worktree is dirty." }

    & .\gradlew.bat :android:testDebugUnitTest `
        --tests "vn.loi.learning.android.acceptance.*" `
        --no-daemon --console=plain
    if ($LASTEXITCODE -ne 0) { throw "Android acceptance suites failed." }

    & .\scripts\verify-android-release-candidate.ps1 `
        -AllowTrackedChanges:$AllowTrackedChanges
    if ($LASTEXITCODE -ne 0) { throw "Android RC qualification failed." }
} finally {
    Pop-Location
}
