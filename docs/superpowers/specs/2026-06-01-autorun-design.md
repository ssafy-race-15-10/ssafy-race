# Autorun Script Design

**Date:** 2026-06-01
**Topic:** Simulator + MyCar 반복 실행 자동화

## Problem

현재 매 실행마다 run.bat 실행 → IntelliJ에서 MyCar 실행 → run.bat 종료를 수동으로 반복해야 한다. N번 반복 테스트 시 반복 작업이 많다.

## Solution

PowerShell 스크립트 `autorun.ps1`을 작성해 Algo.exe와 java 프로세스 생명주기를 자동으로 관리한다.

## File Location

```
Simulator/
  autorun.ps1   ← 신규
  run.bat       ← 기존 (유지)
  Algo.exe
```

## Parameters

| 파라미터 | 타입 | 기본값 | 설명 |
|--------|------|--------|------|
| `-Runs` | int | 5 | 반복 횟수 |
| `-Warmup` | int | 3 | Algo.exe 시작 후 java 실행 전 대기(초) |
| `-Cooldown` | int | 2 | Algo.exe kill 후 다음 반복 전 대기(초) |

## Execution

```powershell
# PowerShell 터미널에서 직접
.\autorun.ps1 -Runs 5

# ExecutionPolicy 제한 환경에서
PowerShell -ExecutionPolicy Bypass -File autorun.ps1 -Runs 5
```

## Directory Layout

```
$PSScriptRoot           = Simulator/        (autorun.ps1 위치)
$PSScriptRoot\..\Bot_Java = Bot_Java/       (MyCar.class, DrivingInterface/ 위치)
```

`Start-Process`의 `-WorkingDirectory`로 두 프로세스를 각자 올바른 디렉터리에서 실행한다.

## Loop Flow (1 iteration)

```
1. "=== Run i/N ===" 출력
2. Start-Process "$PSScriptRoot\Algo.exe" -ArgumentList "-ResX=640 -ResY=480 -windowed"
3. Start-Sleep $Warmup
4. Start-Process java -ArgumentList "-cp .;DrivingInterface -Djava.library.path=DrivingInterface MyCar" -WorkingDirectory "$PSScriptRoot\..\Bot_Java" -Wait -PassThru
5. 종료 코드(ExitCode) 콘솔 출력
6. Stop-Process -Name Algo -Force -ErrorAction SilentlyContinue
7. Start-Sleep $Cooldown
```

## Java Command Details

MyCar는 IntelliJ에서 사전 컴파일된 .class 파일을 사용한다. 스크립트는 컴파일하지 않는다.

```
Working directory : Bot_Java/
Classpath         : .;DrivingInterface
java.library.path : DrivingInterface   (DrivingInterface.dll 위치)
Main class        : MyCar
```

경로는 `autorun.ps1` 기준 상대경로로 산출한다 (`$PSScriptRoot\..\Bot_Java`).

## Output

각 run마다 콘솔에 출력:
```
=== Run 1/5 ===
[MyCar] Start Bot! (JAVA)
...
[MyCar] End Bot! (JAVA), return_code = 0
Exit code: 0
Algo stopped. Waiting 2s...

=== Run 2/5 ===
...
All 5 runs complete.
```

## Out of Scope

- MyCar 컴파일 자동화 (IntelliJ 담당)
- 로그 파일 자동 수집 (MyCar 자체 로그 사용)
- 무한 반복 (N번 고정)
- 멀티플레이 모드 (runsv.bat 시나리오)
