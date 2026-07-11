$ErrorActionPreference = "Stop"

$sources = Get-ChildItem -Recurse -Path "app/src/main/java" -Filter "*.java"
$joined = ($sources | ForEach-Object { Get-Content -Raw $_.FullName }) -join "`n"
$joinedWithoutGuards = $joined -replace '(?s)private static void ensureLossless\(List<String> args\).*?\n    \}', ''

if ($joinedWithoutGuards -match '"-vf"' -or $joinedWithoutGuards -match '"-filter' -or $joinedWithoutGuards -match 'scale=') {
    throw "Found a filter/scale argument in main Java sources."
}

$planSources = $joined
if ($planSources -notmatch '"-c"\s*,\s*"copy"' -and $planSources -notmatch '"-c:a"\s*,\s*"copy"') {
    throw "Did not find stream-copy planning in main Java sources."
}

if ($planSources -notmatch '"-avoid_negative_ts"\s*,\s*"make_zero"') {
    throw "Did not find timestamp normalization in main Java sources."
}

Write-Host "Lossless plan scan passed."
