param(
    [string]$FixtureDir = "app/src/test/fixtures",
    [string]$OutDir = "build/litecut-ffmpeg-checks"
)

$ErrorActionPreference = "Stop"
New-Item -ItemType Directory -Force -Path $OutDir | Out-Null

$withAudio = Join-Path $FixtureDir "with_audio.mp4"
$noAudio = Join-Path $FixtureDir "no_audio.mp4"
$concatList = Join-Path $OutDir "concat.txt"

if (!(Test-Path $withAudio) -or !(Test-Path $noAudio)) {
    throw "Fixtures missing. Run tools/generate_ffmpeg_fixtures.ps1 first."
}

ffmpeg -y -ss 0.000 -i $withAudio -t 1.000 -map 0:v:0 -map 0:a? -c copy -avoid_negative_ts make_zero "$OutDir/cut.mp4"
ffmpeg -y -ss 0.000 -i $noAudio -t 1.000 -map 0:v:0 -c copy -avoid_negative_ts make_zero "$OutDir/cut_no_audio.mp4"
ffmpeg -y -i $withAudio -vn -map 0:a:0 -c:a copy -avoid_negative_ts make_zero "$OutDir/audio.m4a"

@"
file '$((Resolve-Path $withAudio).Path.Replace("'", "'\''"))'
file '$((Resolve-Path $withAudio).Path.Replace("'", "'\''"))'
"@ | Set-Content -Encoding ASCII $concatList

ffmpeg -y -f concat -safe 0 -i $concatList -c copy -avoid_negative_ts make_zero "$OutDir/merged.mp4"

Write-Host "FFmpeg stream-copy integration checks completed in $OutDir"
