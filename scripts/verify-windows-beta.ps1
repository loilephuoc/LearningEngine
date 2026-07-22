param(
    [string]$EvidenceDirectory = "build/beta-evidence",
    [string]$PreviousMsi,
    [switch]$InstallSmoke
)

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

if ($env:OS -ne "Windows_NT") { throw "Windows Beta verification requires Windows." }
if (-not (Get-Command jpackage.exe -ErrorAction SilentlyContinue)) {
    throw "jpackage.exe is required. Set JAVA_HOME to a full JDK 21 distribution."
}

$root = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$evidence = Join-Path $root $EvidenceDirectory
New-Item -ItemType Directory -Force -Path $evidence | Out-Null

Push-Location $root
try {
    & .\gradlew.bat clean test :desktop:packageMsi :desktop:packageExe
    if ($LASTEXITCODE -ne 0) { throw "Gradle verification failed with exit code $LASTEXITCODE." }

    $msi = Get-Item desktop/build/compose/binaries/main/msi/LearningEngine-*.msi
    $exe = Get-Item desktop/build/compose/binaries/main/exe/LearningEngine-*.exe
    $unicodeName = "User_" + [char]0x0110 + "_" + [char]0x65E5 + [char]0x672C + "_" + [char]0x0414
    $unicodeProbe = Join-Path $evidence $unicodeName
    New-Item -ItemType Directory -Force -Path $unicodeProbe | Out-Null
    $probeFile = Join-Path $unicodeProbe "writable.txt"
    $probeContent = "Learning Engine UTF-8 " + [char]0x2713
    [IO.File]::WriteAllText($probeFile, $probeContent, (New-Object Text.UTF8Encoding($false)))
    if ([IO.File]::ReadAllText($probeFile) -ne $probeContent) {
        throw "Unicode writable-directory probe failed."
    }

    $result = [ordered]@{
        schemaVersion = 1
        verifiedAtUtc = [DateTimeOffset]::UtcNow.ToString("O")
        machine = $env:COMPUTERNAME
        os = [Environment]::OSVersion.VersionString
        javaHome = $env:JAVA_HOME
        cleanTestAndPackage = "passed"
        unicodeWritableDirectory = "passed"
        msi = [ordered]@{
            name = $msi.Name
            size = $msi.Length
            sha256 = (Get-FileHash $msi.FullName -Algorithm SHA256).Hash.ToLowerInvariant()
            signature = (Get-AuthenticodeSignature $msi.FullName).Status.ToString()
        }
        exe = [ordered]@{
            name = $exe.Name
            size = $exe.Length
            sha256 = (Get-FileHash $exe.FullName -Algorithm SHA256).Hash.ToLowerInvariant()
            signature = (Get-AuthenticodeSignature $exe.FullName).Status.ToString()
        }
        installSmoke = if ($InstallSmoke) { "requested" } else { "not-run" }
        previousVersionUpgrade = if ($PreviousMsi) { "requested" } else { "not-run" }
    }

    if ($InstallSmoke) {
        $installLog = Join-Path $evidence "install.log"
        $uninstallLog = Join-Path $evidence "uninstall.log"
        if ($PreviousMsi) {
            $previous = Get-Item $PreviousMsi
            $previousLog = Join-Path $evidence "previous-install.log"
            $previousInstall = Start-Process msiexec.exe -Wait -PassThru -ArgumentList @("/i", $previous.FullName, "/qn", "/norestart", "/l*v", $previousLog)
            if ($previousInstall.ExitCode -ne 0) { throw "Previous MSI install failed with exit code $($previousInstall.ExitCode)." }
        }
        $product = Start-Process msiexec.exe -Wait -PassThru -ArgumentList @("/i", $msi.FullName, "/qn", "/norestart", "/l*v", $installLog)
        if ($product.ExitCode -ne 0) { throw "MSI install failed with exit code $($product.ExitCode)." }
        if ($PreviousMsi) {
            $result.previousVersionUpgrade = "upgrade-install-passed"
        }
        $uninstall = Start-Process msiexec.exe -Wait -PassThru -ArgumentList @("/x", $msi.FullName, "/qn", "/norestart", "/l*v", $uninstallLog)
        if ($uninstall.ExitCode -ne 0) { throw "MSI uninstall failed with exit code $($uninstall.ExitCode)." }
        $result.installSmoke = "install-and-uninstall-passed"
    }

    $result | ConvertTo-Json -Depth 5 | Set-Content -Encoding utf8 (Join-Path $evidence "windows-beta-evidence.json")
    $result | ConvertTo-Json -Depth 5
} finally {
    Pop-Location
}
