param(
    [string]$OutDir = "app/src/test/fixtures"
)

$ErrorActionPreference = "Stop"
New-Item -ItemType Directory -Force -Path $OutDir | Out-Null

ffmpeg -y -f lavfi -i testsrc=size=320x240:rate=30 -f lavfi -i sine=frequency=1000:sample_rate=44100 -t 3 -c:v libx264 -g 30 -pix_fmt yuv420p -c:a aac "$OutDir/with_audio.mp4"
ffmpeg -y -f lavfi -i testsrc=size=320x240:rate=30 -t 3 -c:v libx264 -g 30 -pix_fmt yuv420p "$OutDir/no_audio.mp4"
ffmpeg -y -f lavfi -i testsrc=size=640x360:rate=30 -t 3 -c:v libx264 -g 30 -pix_fmt yuv420p "$OutDir/incompatible_resolution.mp4"
