param(
    [int]$Runs    = 5,
    [int]$Warmup  = 3,
    [int]$Cooldown = 2
)

$AlgoExe    = "$PSScriptRoot\Algo.exe"
$BotJavaDir = "$PSScriptRoot\..\Bot_Java"

# 사전 확인
if (-not (Test-Path $AlgoExe)) {
    Write-Error "Algo.exe not found: $AlgoExe"
    exit 1
}
if (-not (Test-Path $BotJavaDir)) {
    Write-Error "Bot_Java directory not found: $BotJavaDir"
    exit 1
}

for ($i = 1; $i -le $Runs; $i++) {
    Write-Host "=== Run $i/$Runs ===" -ForegroundColor Cyan

    # 1. 시뮬레이터 시작
    $algo = Start-Process -FilePath $AlgoExe `
                          -ArgumentList "-ResX=640 -ResY=480 -windowed" `
                          -PassThru
    Start-Sleep -Seconds $Warmup

    # 2. MyCar 실행 (종료될 때까지 대기)
    $java = Start-Process -FilePath "java" `
                          -ArgumentList "-cp .;DrivingInterface -Djava.library.path=DrivingInterface MyCar" `
                          -WorkingDirectory $BotJavaDir `
                          -NoNewWindow `
                          -Wait `
                          -PassThru
    Write-Host "Exit code: $($java.ExitCode)"

    # 3. 시뮬레이터 종료
    Stop-Process -Name "Algo" -Force -ErrorAction SilentlyContinue
    Write-Host "Algo stopped. Waiting ${Cooldown}s..."
    Start-Sleep -Seconds $Cooldown
}

Write-Host "All $Runs runs complete." -ForegroundColor Green
