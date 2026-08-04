param([switch]$InstallDebugApk)
$ErrorActionPreference='Stop'
$root=(Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$evidence=Join-Path $root 'build/android-device-runtime-evidence'
New-Item -ItemType Directory -Force $evidence|Out-Null
$devices=@(& adb devices|Select-Object -Skip 1|Where-Object{$_ -match "\tdevice$"})
if($devices.Count-ne 1){throw "Expected exactly one authorized ADB device; found $($devices.Count)."}
$apk=Join-Path $root 'android/build/outputs/apk/debug/android-debug.apk'
if($InstallDebugApk){if(-not(Test-Path $apk)){throw "Debug APK is missing."};& adb install -r $apk;if($LASTEXITCODE-ne 0){throw 'adb install -r failed.'}}
& adb shell am force-stop vn.loi.learning.android
& adb logcat -c
$timing=& adb shell am start -W -n vn.loi.learning.android/.MainActivity
$timing|Set-Content (Join-Path $evidence 'am-start.txt') -Encoding utf8
Start-Sleep -Seconds 3
$appProcessId=(@(& adb shell pidof vn.loi.learning.android 2>$null) -join '').Trim()
if(-not $appProcessId){throw 'Application process is not alive after launch.'}
$logs=& adb logcat -d -v threadtime LearningEngineStartup:I AndroidRuntime:E ActivityManager:I '*:S'
$logs|Set-Content (Join-Path $evidence 'startup-logcat.txt') -Encoding utf8
$fatal=@($logs|Select-String 'FATAL EXCEPTION|ANR in|NoSuchMethodError')
$media=& adb shell run-as vn.loi.learning.android sh -c 'find files/learning-engine/data/media -type f -printf "%s\n" 2>/dev/null' 2>&1
$sizes=@($media|Where-Object{$_ -match '^\d+$'}|ForEach-Object{[long]$_})
[ordered]@{pid=$appProcessId;timing=$timing;startupLogCount=@($logs).Count;fatalCount=$fatal.Count;mediaFileCount=$sizes.Count;mediaBytes=($sizes|Measure-Object -Sum).Sum;installed=$InstallDebugApk.IsPresent;dataCleared=$false}|ConvertTo-Json|Set-Content (Join-Path $evidence 'summary.json') -Encoding utf8
if($fatal.Count){throw "Fatal/ANR/API blocker found; inspect $evidence."}
Get-Content (Join-Path $evidence 'summary.json')
