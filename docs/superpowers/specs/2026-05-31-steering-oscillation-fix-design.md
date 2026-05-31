# Steering Oscillation Fix — Design Spec

Date: 2026-05-31  
Status: Approved

## Problem

주행 로그(`run_20260531_231337.csv`, 1454틱) 분석 결과:

- `to_middle` 평균 절댓값 **6.3m** (halfRoadLimit ~10m 대비 63% 이탈)
- steering 방향이 이탈 방향과 불일치(과보정): **160회 (13%)**
- steering 부호 전환(좌우 진동): **54회 (3.7%)**

**근본 원인:** 현재 steering 공식에 D항(미분항)이 없어 오버슈팅 → 반대 보정 → 반복 진동이 발생. 추가로 K2(각도 0.4) > K1(중앙 0.3)이라 각도 보정이 위치 복귀보다 강해 차가 중앙에서 벗어난 상태를 유지하게 됨. EMA smoothing 부재로 moving_angle 스파이크(-171.7°)가 steering에 직접 반영됨.

## Design

### 1. Steering 공식 — PD 제어 + EMA

```
centerError  = -(to_middle / halfRoadLimit)
angleError   = -(moving_angle / 90.0)
lookahead    = computeLookaheadAngle(angles, p)
dCenter      = centerError - prevCenterError        // D항: 복귀 중이면 음수
raw          = K1*centerError + K2*angleError + K3*lookahead + K4*dCenter
steering_out = clamp(alpha*raw + (1-alpha)*prevSteering, -1, 1)  // EMA
```

- `dCenter > 0`: 이탈 증가 중 → 보정 강화
- `dCenter < 0`: 복귀 중 → 오버슈팅 전에 보정 감소

### 2. 파라미터 변경

`TrackParams`에 `K4` (미분 gain), `alpha` (EMA 계수) 필드 추가.

| 파라미터 | 이전 | 이후 | 이유 |
|---|---|---|---|
| K1 (중앙 복귀) | 0.3 | 0.45 | 위치 복귀력 강화 |
| K2 (각도 보정) | 0.4 | 0.20 | 각도 < 위치로 순서 반전, 오버슈팅 방지 |
| K3 (lookahead) | 0.3 | 0.35 | 전방 커브 예측 소폭 강화 |
| K4 (미분항)    | —   | 0.25 | 복귀 감지, 오버슈팅 선제 억제 |
| alpha (EMA)    | —   | 0.40 | 이전 steering 60% 유지, 노이즈 흡수 |

SPEED / SSAFY / GERMANY 트랙도 동일 비율로 조정. `alpha`는 전 트랙 공통.

트랙별 파라미터 (K1, K2, K3, K4, alpha):

| 트랙 | K1 | K2 | K3 | K4 | alpha |
|---|---|---|---|---|---|
| BASIC   | 0.45 | 0.20 | 0.35 | 0.25 | 0.40 |
| SPEED   | 0.45 | 0.20 | 0.35 | 0.25 | 0.40 |
| SSAFY   | 0.50 | 0.25 | 0.35 | 0.25 | 0.40 |
| GERMANY | 0.55 | 0.25 | 0.40 | 0.30 | 0.35 |

### 3. 상태 변수

`MyCar` 인스턴스 변수 2개 추가:

```java
private float prevCenterError = 0f;
private float prevSteering    = 0f;
```

### 4. `computeSteering` 시그니처 확장

```java
static float computeSteering(float toMiddle, float halfRoadLimit,
                              float movingAngle, float[] angles,
                              TrackParams p,
                              float prevCenterError, float prevSteering)
```

반환 후 `control_driving`에서 상태 갱신:

```java
float centerError = -(sensing_info.to_middle / sensing_info.half_road_limit);
car_controls.steering = computeSteering(..., prevCenterError, prevSteering);
prevSteering    = car_controls.steering;
prevCenterError = centerError;
```

### 5. StuckDetector 예외

역방향(`reverseTicks > 0`) 주행 중에는 `prevSteering` / `prevCenterError` 갱신을 건너뜀. 후진 후 재출발 시 EMA가 후진 steering 값을 물고 가는 것을 방지.

## Success Criteria

- `to_middle` 평균 절댓값 6.3m → **3m 미만**으로 감소
- steering 부호 전환 횟수 54회 → **20회 미만**
- 최대 속도 유지 (113km/h 수준 유지)
- 완주 가능 (StuckDetector 정상 동작)

## Out of Scope

- 속도 적응형 gain 스케일링 (Approach 3) — 파라미터 튜닝 부담으로 이번 스펙 제외
- 장애물 회피 로직 변경 — 별도 스펙(`2026-05-31-obstacle-handling-design.md`) 유지
