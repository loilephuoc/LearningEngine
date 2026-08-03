param(
    [string]$EvidenceDirectory = "build/release-candidate-evidence"
)

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

if ($env:OS -ne "Windows_NT") { throw "Windows release-candidate qualification requires Windows." }
if (-not $env:JAVA_HOME) { throw "JAVA_HOME must point to the approved full JDK 21 distribution." }
if (-not (Get-Command java.exe -ErrorAction SilentlyContinue)) { throw "java.exe is unavailable." }
if (-not (Get-Command jpackage.exe -ErrorAction SilentlyContinue)) { throw "jpackage.exe is unavailable." }

$root = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$expectedJavaHome = "C:\Users\M72Q\.jdks\temurin-21.0.11"
if (-not [IO.Path]::GetFullPath($env:JAVA_HOME).Equals($expectedJavaHome, [StringComparison]::OrdinalIgnoreCase)) {
    throw "JAVA_HOME must be $expectedJavaHome for this qualification."
}

function Get-TestCounts([string]$directory) {
    $files = @(Get-ChildItem -LiteralPath $directory -Filter "TEST-*.xml" -File -ErrorAction SilentlyContinue)
    $counts = [ordered]@{ suites = $files.Count; tests = 0; failures = 0; errors = 0; skipped = 0 }
    foreach ($file in $files) {
        [xml]$xml = Get-Content -Raw -LiteralPath $file.FullName
        $suite = $xml.testsuite
        $counts.tests += [int]$suite.tests
        $counts.failures += [int]$suite.failures
        $counts.errors += [int]$suite.errors
        $counts.skipped += [int]$suite.skipped
    }
    return $counts
}

function Get-ArtifactEvidence([IO.FileInfo]$artifact) {
    $signature = Get-AuthenticodeSignature -LiteralPath $artifact.FullName
    return [ordered]@{
        path = $artifact.FullName
        filename = $artifact.Name
        size = $artifact.Length
        sha256 = (Get-FileHash -LiteralPath $artifact.FullName -Algorithm SHA256).Hash.ToLowerInvariant()
        signature = $signature.Status.ToString()
    }
}

Push-Location $root
try {
    & .\gradlew.bat clean test :desktop:verifyWindowsReleaseLauncher :desktop:packageReleaseExe :desktop:packageReleaseMsi --no-daemon --console=plain
    if ($LASTEXITCODE -ne 0) { throw "Release qualification failed with exit code $LASTEXITCODE." }

    $distribution = Get-Item "desktop/build/compose/binaries/main-release/app/LearningEngine"
    $exe = @(Get-Item "desktop/build/compose/binaries/main-release/exe/LearningEngine-*.exe")
    $msi = @(Get-Item "desktop/build/compose/binaries/main-release/msi/LearningEngine-*.msi")
    if ($exe.Count -ne 1) { throw "Expected exactly one release EXE, found $($exe.Count)." }
    if ($msi.Count -ne 1) { throw "Expected exactly one release MSI, found $($msi.Count)." }

    $launcherOutputPath = "desktop/build/release-launcher-smoke/launcher-output.txt"
    $launcherOutput = Get-Content -Raw -LiteralPath $launcherOutputPath
    if ($launcherOutput -notmatch "LE_STARTUP_VERIFICATION=passed") { throw "Missing launcher verification marker." }
    if ($launcherOutput -notmatch "LE_AUDIO_PROVIDER_PROBE=passed") { throw "Missing audio provider marker." }

    $rootCounts = Get-TestCounts "build/test-results/test"
    $desktopCounts = Get-TestCounts "desktop/build/test-results/test"
    $totalCounts = [ordered]@{
        suites = $rootCounts.suites + $desktopCounts.suites
        tests = $rootCounts.tests + $desktopCounts.tests
        failures = $rootCounts.failures + $desktopCounts.failures
        errors = $rootCounts.errors + $desktopCounts.errors
        skipped = $rootCounts.skipped + $desktopCounts.skipped
    }
    if (($totalCounts.failures + $totalCounts.errors) -ne 0) { throw "Test XML contains failures or errors." }

    $manifestLines = @(
        Get-ChildItem -LiteralPath $distribution.FullName -Recurse -File |
            Sort-Object FullName |
            ForEach-Object {
                $relative = $_.FullName.Substring($distribution.FullName.Length).TrimStart([char[]]@('\', '/')).Replace('\', '/')
                "$relative|$($_.Length)|$((Get-FileHash -LiteralPath $_.FullName -Algorithm SHA256).Hash.ToLowerInvariant())"
            }
    )
    $manifestText = $manifestLines -join "`n"
    $sha256 = [Security.Cryptography.SHA256]::Create()
    try {
        $manifestHash = ([BitConverter]::ToString($sha256.ComputeHash([Text.Encoding]::UTF8.GetBytes($manifestText)))).Replace("-", "").ToLowerInvariant()
    } finally {
        $sha256.Dispose()
    }

    $evidence = Join-Path $root $EvidenceDirectory
    New-Item -ItemType Directory -Force -Path $evidence | Out-Null
    [IO.File]::WriteAllText((Join-Path $evidence "portable-manifest.txt"), $manifestText + "`n", (New-Object Text.UTF8Encoding($false)))

    $javaVersion = (& cmd.exe /d /c "`"$env:JAVA_HOME\bin\java.exe`" -version 2>&1") -join "`n"
    $jpackageVersion = (& jpackage.exe --version 2>&1) -join "`n"
    $gradleVersion = (& .\gradlew.bat --version --no-daemon 2>&1) -join "`n"
    $branch = (& git branch --show-current).Trim()
    $head = (& git rev-parse HEAD).Trim()
    $originDevelop = (& git rev-parse origin/develop).Trim()
    $stableTag = (& git tag --merged HEAD --sort=-version:refname | Select-Object -First 1)
    $trackedStatus = @(& git status --porcelain --untracked-files=no)

    $result = [ordered]@{
        schemaVersion = 1
        verifiedAtUtc = [DateTimeOffset]::UtcNow.ToString("O")
        git = [ordered]@{
            branch = $branch
            head = $head
            originDevelop = $originDevelop
            dirtyTracked = ($trackedStatus.Count -gt 0)
            stableTag = if ($stableTag) { $stableTag.Trim() } else { $null }
        }
        environment = [ordered]@{
            os = [Environment]::OSVersion.VersionString
            javaHome = $env:JAVA_HOME
            javaVersion = $javaVersion
            jpackageVersion = $jpackageVersion
            gradleVersion = $gradleVersion
        }
        fullTest = [ordered]@{
            result = "passed"
            root = $rootCounts
            desktop = $desktopCounts
            total = $totalCounts
        }
        releaseDistributable = [ordered]@{
            path = $distribution.FullName
            manifestPath = (Join-Path $evidence "portable-manifest.txt")
            manifestSha256 = $manifestHash
        }
        launcherSmoke = [ordered]@{ result = "passed"; outputPath = (Resolve-Path $launcherOutputPath).Path }
        runtimeIntegrity = [ordered]@{ result = "passed"; bundledRuntime = $true; accessibilityModule = $true; proguardRelease = $true }
        audioProbe = [ordered]@{ result = "provider-discovery-passed"; speakerPlayback = "not-run"; outputPath = (Resolve-Path $launcherOutputPath).Path }
        exe = Get-ArtifactEvidence $exe[0]
        msi = Get-ArtifactEvidence $msi[0]
        unverifiedGates = @(
            "real speaker playback", "clean-machine execution", "MSI installation",
            "upgrade from previous MSI", "uninstall", "code signing",
            "Windows Defender reputation/SmartScreen", "manual UI UAT",
            "representative long-session real usage", "large 179 MB package manual verification",
            "backup/restore manual acceptance", "accessibility screen-reader manual acceptance"
        )
    }

    $jsonPath = Join-Path $evidence "release-candidate-evidence.json"
    $result | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath $jsonPath -Encoding utf8
    $result | ConvertTo-Json -Depth 8
} finally {
    Pop-Location
}
