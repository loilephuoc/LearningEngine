param(
    [string]$EvidenceDirectory = "build/android-release-candidate-evidence",
    [switch]$AllowTrackedChanges
)
$ErrorActionPreference="Stop"; Set-StrictMode -Version Latest
$root=(Resolve-Path (Join-Path $PSScriptRoot "..")).Path
function Counts([string]$path){$f=@(Get-ChildItem $path -Filter 'TEST-*.xml' -File -ErrorAction SilentlyContinue);$r=[ordered]@{suites=$f.Count;tests=0;failures=0;errors=0;skipped=0};foreach($x in $f){[xml]$z=Get-Content -Raw $x.FullName;foreach($n in @('tests','failures','errors','skipped')){$r[$n]+=[int]$z.testsuite.$n}};$r}
function Artifact([string]$path,[string]$signature){$f=Get-Item $path;[ordered]@{path=$f.FullName;size=$f.Length;sha256=(Get-FileHash $f.FullName -Algorithm SHA256).Hash.ToLowerInvariant();signature=$signature}}
function ZipEntries([string]$path){Add-Type -AssemblyName System.IO.Compression.FileSystem;$z=[IO.Compression.ZipFile]::OpenRead((Resolve-Path $path));try{@($z.Entries|ForEach-Object{[ordered]@{name=$_.FullName;size=$_.Length}})}finally{$z.Dispose()}}
Push-Location $root
try {
    $branch=(git branch --show-current).Trim();$head=(git rev-parse HEAD).Trim();$origin=(git rev-parse origin/develop).Trim()
    if($branch-ne'develop'){throw "Expected develop branch."};if($head-ne$origin){throw "HEAD must equal origin/develop."};if(-not (git tag --list v0.9.7-rc1)){throw "v0.9.7-rc1 is missing."}
    $tracked=@(git status --porcelain --untracked-files=no);if($tracked.Count -and -not $AllowTrackedChanges){throw "Tracked worktree is dirty."}
    & .\gradlew.bat clean test :android:testDebugUnitTest :android:assembleDebug :android:assembleRelease :android:bundleRelease --no-daemon --console=plain
    if($LASTEXITCODE-ne 0){throw "Gradle qualification failed."}
    $debug='android/build/outputs/apk/debug/android-debug.apk';$release='android/build/outputs/apk/release/android-release-unsigned.apk';$aab='android/build/outputs/bundle/release/android-release.aab'
    foreach($p in @($debug,$release,$aab)){if(-not(Test-Path $p)){throw "Missing artifact: $p"}}
    $apkEntries=ZipEntries $release;$aabEntries=ZipEntries $aab
    foreach($required in @('AndroidManifest.xml','resources.arsc')){if($required-notin$apkEntries.name){throw "Release APK missing $required"}}
    if(-not($apkEntries.name -match '^classes\d*\.dex$')){throw 'Release APK has no classes.dex.'}
    $manifest=Get-Content -Raw android/src/main/AndroidManifest.xml
    $forbidden=@('MANAGE_EXTERNAL_STORAGE','READ_EXTERNAL_STORAGE','WRITE_EXTERNAL_STORAGE','INTERNET','RECORD_AUDIO','CAMERA','usesCleartextTraffic="true"','largeHeap="true"');foreach($x in $forbidden){if($manifest.Contains($x)){throw "Forbidden manifest capability: $x"}}
    $permissions=@([regex]::Matches($manifest,'uses-permission[^>]+android:name="([^"]+)"')|ForEach-Object{$_.Groups[1].Value})
    $native=@($apkEntries|Where-Object{$_.name-match '^lib/[^/]+/.+\.so$'});$abis=@($native|ForEach-Object{$_.name.Split('/')[1]}|Sort-Object -Unique)
    $rootCounts=Counts 'build/test-results/test';$desktopCounts=Counts 'desktop/build/test-results/test';$androidCounts=Counts 'android/build/test-results/testDebugUnitTest'
    $total=[ordered]@{suites=$rootCounts.suites+$desktopCounts.suites+$androidCounts.suites;tests=$rootCounts.tests+$desktopCounts.tests+$androidCounts.tests;failures=$rootCounts.failures+$desktopCounts.failures+$androidCounts.failures;errors=$rootCounts.errors+$desktopCounts.errors+$androidCounts.errors;skipped=$rootCounts.skipped+$desktopCounts.skipped+$androidCounts.skipped}
    if($total.failures+$total.errors-ne 0){throw 'Test XML contains failures.'}
    $evidence=Join-Path $root $EvidenceDirectory;New-Item -ItemType Directory -Force $evidence|Out-Null
    $javaVersion=(& cmd.exe /d /c "java -version 2>&1")-join"`n"
    $gradleVersion=(& cmd.exe /d /c ".\gradlew.bat --version --no-daemon 2>&1")-join"`n"
    $r8Files=@('configuration.txt','mapping.txt','resources.txt','seeds.txt','usage.txt')|ForEach-Object{[ordered]@{name=$_;present=(Test-Path (Join-Path 'android/build/outputs/mapping/release' $_))}}
    $result=[ordered]@{schemaVersion=1;verifiedAtUtc=[DateTimeOffset]::UtcNow.ToString('O');git=[ordered]@{branch=$branch;head=$head;originDevelop=$origin;dirtyTracked=($tracked.Count-gt 0);stableTag='v0.9.7-rc1'};environment=[ordered]@{os=[Environment]::OSVersion.VersionString;java=$javaVersion;gradle=$gradleVersion;androidGradlePlugin='9.1.1';kotlin='2.4.0';composeBom='2026.06.00'};android=[ordered]@{applicationId='vn.loi.learning.android';versionCode=1;versionName='1.0';minSdk=26;targetSdk=37;compileSdk=37;minify=$true;shrinkResources=$true;signing='NotSigned (release)';r8Outputs=$r8Files};tests=[ordered]@{root=$rootCounts;desktop=$desktopCounts;android=$androidCounts;total=$total};artifacts=[ordered]@{debugApk=Artifact $debug 'DebugSigned';releaseApk=Artifact $release 'NotSigned';releaseAab=Artifact $aab 'NotSigned'};manifest=[ordered]@{permissions=$permissions;exportedComponents=@('vn.loi.learning.android.MainActivity');autoBackup=$false;cleartext=$false};native=[ordered]@{abis=$abis;libraries=$native};smoke=[ordered]@{startupGraph='passed';libraryStudyServices='passed';packageMediaAuthorities='passed';physicalLaunch='pending'};manualGates=@('phone/tablet install and first launch','cold/warm startup measurement','real document providers and large package','Library search and editor IME','export verify upgrade uninstall','scoped Study and five Recall runtimes','speaker and real images','rotation process death predictive Back','TalkBack and font scale 1.3-2.0x','portrait landscape tablet foldable','low storage backup restore long Practice','battery memory ANR crash observation','release signing Play upload Integrity pre-launch report')}
    $result|ConvertTo-Json -Depth 10|Set-Content (Join-Path $evidence 'android-release-candidate-evidence.json') -Encoding utf8
    $permissions|Set-Content (Join-Path $evidence 'permission-inventory.txt') -Encoding utf8
    $apkEntries|ForEach-Object{"$($_.name)|$($_.size)"}|Set-Content (Join-Path $evidence 'apk-content-inventory.txt') -Encoding utf8
    @($result.artifacts.debugApk,$result.artifacts.releaseApk,$result.artifacts.releaseAab)|ForEach-Object{"$($_.sha256)  $($_.path)"}|Set-Content (Join-Path $evidence 'artifact-hashes.txt') -Encoding utf8
    "# Android RC automated qualification`n`nAutomated gates: PASS.`n`nPhysical-device and release-signing gates: PENDING.`n"|Set-Content (Join-Path $evidence 'android-release-candidate-report.md') -Encoding utf8
    git diff --check;if($LASTEXITCODE-ne 0){throw 'git diff --check failed.'};$result|ConvertTo-Json -Depth 10
} finally {Pop-Location}
