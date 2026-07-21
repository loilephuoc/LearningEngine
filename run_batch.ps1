param(
    [Parameter(Mandatory = $true, Position = 0)]
    [ValidatePattern('^Batch\d+$')]
    [string]$Batch
)

$ErrorActionPreference = 'Stop'
$BatchRoot = 'C:\Users\M72Q\LearningEngine_Batches'
$RepoRoot = 'C:\Users\M72Q\IdeaProjects\LearningEngine'
$ArchiveName = "${Batch}_APPLY.zip"
$ExtractedName = "${Batch}_APPLY"
$ArchivePath = Join-Path $BatchRoot $ArchiveName
$ExtractedPath = Join-Path $BatchRoot $ExtractedName
$ApplyScript = Join-Path $ExtractedPath 'apply_batch.ps1'

if (-not (Test-Path -LiteralPath $ArchivePath)) {
    throw "Không tìm thấy file: $ArchivePath"
}

if (-not (Test-Path -LiteralPath $RepoRoot)) {
    throw "Không tìm thấy repository: $RepoRoot"
}

Remove-Item -LiteralPath $ExtractedPath -Recurse -Force -ErrorAction SilentlyContinue
Expand-Archive -LiteralPath $ArchivePath -DestinationPath $ExtractedPath -Force

if (-not (Test-Path -LiteralPath $ApplyScript)) {
    throw "ZIP không chứa apply_batch.ps1 ở cấp root: $ApplyScript"
}

Set-Location -LiteralPath $RepoRoot
& $ApplyScript
exit $LASTEXITCODE
