param(
    [int]$Runs    = 5,
    [int]$Warmup  = 8,
    [int]$Cooldown = 6
)

[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$OutputEncoding = [System.Text.Encoding]::UTF8

$AlgoExe    = "$PSScriptRoot\Algo.exe"
$BotJavaDir = Resolve-Path "$PSScriptRoot\..\Bot_Java"
$OutDir     = "Build\Release"

# 사전 확인
if (-not (Test-Path $AlgoExe)) {
    Write-Error "Algo.exe not found: $AlgoExe"
    exit 1
}

# java.exe / javac.exe 탐색 (PATH → JAVA_HOME → 일반 설치 경로 순)
$JavaExe = "java"
if (-not (Get-Command java -ErrorAction SilentlyContinue)) {
    $candidates = @(
        "$env:JAVA_HOME\bin\java.exe",
        "C:\Program Files\Java\jdk*\bin\java.exe",
        "C:\Program Files\Eclipse Adoptium\*\bin\java.exe",
        "C:\Program Files\Microsoft\jdk*\bin\java.exe",
        "C:\Program Files\JetBrains\*\jbr\bin\java.exe"
    )
    $found = $null
    foreach ($pattern in $candidates) {
        $found = Get-Item $pattern -ErrorAction SilentlyContinue | Select-Object -Last 1
        if ($found) { $JavaExe = $found.FullName; break }
    }
    if (-not $found) {
        Write-Error "java.exe를 찾을 수 없습니다. JAVA_HOME 환경변수를 설정하거나 Java를 PATH에 추가하세요."
        exit 1
    }
}
$JavacExe = if ($JavaExe -eq "java") { "javac" } else { Join-Path (Split-Path $JavaExe) "javac.exe" }
Write-Host "Using java : $JavaExe"
Write-Host "Using javac: $JavacExe"

# 빌드
Write-Host "Building MyCar..." -ForegroundColor Yellow
New-Item -ItemType Directory -Path "$BotJavaDir\$OutDir" -Force | Out-Null
$build = Start-Process -FilePath $JavacExe `
                       -ArgumentList "-encoding UTF-8 -cp . -d `"$OutDir`" MyCar.java DrivingInterface\DrivingInterface.java" `
                       -WorkingDirectory $BotJavaDir `
                       -NoNewWindow -Wait -PassThru
if ($build.ExitCode -ne 0) {
    Write-Error "빌드 실패 (exit code $($build.ExitCode)). MyCar.java 컴파일 오류를 확인하세요."
    exit 1
}
Write-Host "Build OK." -ForegroundColor Green

# 실행 루프
try {
    for ($i = 1; $i -le $Runs; $i++) {
        Write-Host "=== Run $i/$Runs ===" -ForegroundColor Cyan
        Stop-Process -Name "Algo" -Force -ErrorAction SilentlyContinue

        # 1. 시뮬레이터 시작
        $algo = Start-Process -FilePath $AlgoExe `
                              -ArgumentList "-ResX=640 -ResY=480 -windowed" `
                              -PassThru
        Start-Sleep -Seconds $Warmup

        # 2. MyCar 실행 (종료될 때까지 대기)
        $java = Start-Process -FilePath $JavaExe `
                              -ArgumentList "-cp `"$OutDir`" -Djava.library.path=DrivingInterface MyCar" `
                              -WorkingDirectory $BotJavaDir `
                              -NoNewWindow -Wait -PassThru
        Write-Host "Exit code: $($java.ExitCode)"
        if ($java.ExitCode -ne 0) {
            Write-Warning "Run $i ended with non-zero exit code: $($java.ExitCode)"
        }

        # 3. 시뮬레이터 종료 — 프로세스가 완전히 사라질 때까지 대기
        Stop-Process -Name "Algo" -Force -ErrorAction SilentlyContinue
        $waited = 0
        while ((Get-Process -Name "Algo" -ErrorAction SilentlyContinue) -and $waited -lt 10) {
            Start-Sleep -Seconds 1; $waited++
        }
        Write-Host "Algo stopped. Waiting ${Cooldown}s..."
        Start-Sleep -Seconds $Cooldown
    }
} finally {
    Stop-Process -Name "Algo" -Force -ErrorAction SilentlyContinue
}

Write-Host "All $Runs runs complete." -ForegroundColor Green
