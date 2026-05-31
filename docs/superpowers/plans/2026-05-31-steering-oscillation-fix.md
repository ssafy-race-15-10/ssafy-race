# Steering Oscillation Fix Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** PD 제어(미분항) + EMA smoothing으로 좌우 진동을 제거하고 도로 중앙 이탈을 6.3m → 3m 미만으로 줄인다.

**Architecture:** `computeSteering`에 `prevCenterError`(D항) · `prevSteering`(EMA) 두 파라미터를 추가해 순수 계산 함수를 유지한다. 상태는 `MyCar` 인스턴스 변수로 관리하며, `control_driving`에서 틱마다 갱신한다. StuckDetector 역방향 구간에서는 상태 갱신을 건너뛴다.

**Tech Stack:** Java 8, TestRunner.java (단위 테스트 harness), `javac -cp . MyCar.java TestRunner.java`

---

## 파일 목록

- 수정: `Bot_Java/MyCar.java` — TrackParams 필드 추가, PARAMS 값 변경, computeSteering 시그니처·본문 변경, 인스턴스 변수 추가, control_driving 호출부 수정
- 수정: `Bot_Java/TestRunner.java` — testTrackParams 단언 수정, testSteering 시그니처·케이스 업데이트

---

## Task 1: TrackParams에 K4·alpha 필드 추가 및 PARAMS 값 갱신

**Files:**
- Modify: `Bot_Java/MyCar.java:18-51`
- Modify: `Bot_Java/TestRunner.java:4-40`

- [ ] **Step 1: testTrackParams 실패 테스트 작성**

`TestRunner.java`의 `testTrackParams()`에서 기존 K1·K2 단언을 새 값으로 교체하고 K4·alpha 단언을 추가한다. 컴파일은 되지만 실행 시 실패해야 한다.

```java
static void testTrackParams() {
    MyCar.TrackParams basic = MyCar.PARAMS[MyCar.TRACK_BASIC];
    assertTrue(basic.maxSpeed == 130f,        "BASIC maxSpeed");
    assertTrue(basic.minSpeed == 40f,         "BASIC minSpeed");
    assertTrue(basic.slowdownFactor == 0.8f,  "BASIC slowdownFactor");
    assertTrue(basic.steerLookAhead == 5,     "BASIC steerLookAhead");
    assertTrue(basic.speedLookAhead == 6,     "BASIC speedLookAhead");
    assertTrue(basic.K1 == 0.45f,            "BASIC K1");         // 0.3 → 0.45
    assertTrue(basic.K4 == 0.25f,            "BASIC K4");         // 신규
    assertTrue(basic.alpha == 0.40f,         "BASIC alpha");      // 신규
    assertTrue(basic.brakeRange == 40f,       "BASIC brakeRange");

    MyCar.TrackParams speed = MyCar.PARAMS[MyCar.TRACK_SPEED];
    assertTrue(speed.maxSpeed == 120f,        "SPEED maxSpeed");
    assertTrue(speed.slowdownFactor == 0.9f,  "SPEED slowdownFactor");
    assertTrue(speed.steerLookAhead == 6,     "SPEED steerLookAhead");
    assertTrue(speed.K2 == 0.20f,            "SPEED K2");         // 0.4 → 0.20
    assertTrue(speed.K4 == 0.25f,            "SPEED K4");
    assertTrue(speed.alpha == 0.40f,         "SPEED alpha");

    MyCar.TrackParams ssafy = MyCar.PARAMS[MyCar.TRACK_SSAFY];
    assertTrue(ssafy.maxSpeed == 110f,        "SSAFY maxSpeed");
    assertTrue(ssafy.slowdownFactor == 1.0f,  "SSAFY slowdownFactor");
    assertTrue(ssafy.decayFactor == 0.3f,    "SSAFY decayFactor");
    assertTrue(ssafy.accelerationRange == 25f,"SSAFY accelerationRange");
    assertTrue(ssafy.K4 == 0.25f,           "SSAFY K4");
    assertTrue(ssafy.alpha == 0.40f,        "SSAFY alpha");

    MyCar.TrackParams germany = MyCar.PARAMS[MyCar.TRACK_GERMANY];
    assertTrue(germany.maxSpeed == 100f,      "GERMANY maxSpeed");
    assertTrue(germany.steerLookAhead == 8,   "GERMANY steerLookAhead");
    assertTrue(germany.speedLookAhead == 10,  "GERMANY speedLookAhead");
    assertTrue(germany.K3 == 0.40f,          "GERMANY K3");
    assertTrue(germany.brakeRange == 30f,     "GERMANY brakeRange");
    assertTrue(germany.K4 == 0.30f,          "GERMANY K4");
    assertTrue(germany.alpha == 0.35f,       "GERMANY alpha");

    System.out.println("PASS: TrackParams");
}
```

- [ ] **Step 2: 컴파일 및 실패 확인**

```bash
cd /Users/whqtker/Documents/SSAFY/ssafy-race/Bot_Java
javac -cp . MyCar.java TestRunner.java 2>&1
java -cp . TestRunner 2>&1
```

Expected: `FAIL: BASIC K1` (K4 필드가 없어 컴파일 에러 또는 값 불일치)

- [ ] **Step 3: TrackParams에 K4·alpha 필드 추가, PARAMS 값 갱신**

`MyCar.java`의 `TrackParams` 클래스 전체를 아래로 교체한다.

```java
static class TrackParams {
    final float maxSpeed, minSpeed, slowdownFactor;
    final float K1, K2, K3, decayFactor;
    final float accelerationRange, brakeRange;
    final int steerLookAhead, speedLookAhead;
    final float K4, alpha;

    TrackParams(float maxSpeed, float minSpeed, float slowdownFactor,
                int steerLookAhead, int speedLookAhead,
                float K1, float K2, float K3, float decayFactor,
                float accelerationRange, float brakeRange,
                float K4, float alpha) {
        this.maxSpeed          = maxSpeed;
        this.minSpeed          = minSpeed;
        this.slowdownFactor    = slowdownFactor;
        this.steerLookAhead    = steerLookAhead;
        this.speedLookAhead    = speedLookAhead;
        this.K1                = K1;
        this.K2                = K2;
        this.K3                = K3;
        this.decayFactor       = decayFactor;
        this.accelerationRange = accelerationRange;
        this.brakeRange        = brakeRange;
        this.K4                = K4;
        this.alpha             = alpha;
    }
}
```

그리고 `PARAMS` 배열 전체를 아래로 교체한다.

```java
static final TrackParams[] PARAMS = {
    // BASIC:  maxSpd minSpd slow  stLA spLA  K1     K2     K3     decay  accR  brkR   K4     alpha
    new TrackParams(130, 40, 0.8f,  5,  6, 0.45f, 0.20f, 0.35f, 0.4f, 30, 40, 0.25f, 0.40f),
    // SPEED
    new TrackParams(120, 35, 0.9f,  6,  7, 0.45f, 0.20f, 0.35f, 0.4f, 30, 40, 0.25f, 0.40f),
    // SSAFY
    new TrackParams(110, 30, 1.0f,  7,  8, 0.50f, 0.25f, 0.35f, 0.3f, 25, 35, 0.25f, 0.40f),
    // GERMANY
    new TrackParams(100, 25, 1.2f,  8, 10, 0.55f, 0.25f, 0.40f, 0.5f, 20, 30, 0.30f, 0.35f)
};
```

- [ ] **Step 4: 컴파일 및 테스트 통과 확인**

```bash
cd /Users/whqtker/Documents/SSAFY/ssafy-race/Bot_Java
javac -cp . MyCar.java TestRunner.java 2>&1 && java -cp . TestRunner 2>&1
```

Expected 출력 (일부):
```
PASS: TrackParams
PASS: track detection
...
PASS: speed control
PASS: obstacle avoidance steering
PASS: stuck detector
```

`FAIL:` 로 시작하는 줄이 없어야 한다. testSteering은 아직 5-인수 시그니처를 사용하므로 컴파일 에러가 없다면 통과할 것이다.

- [ ] **Step 5: 커밋**

```bash
cd /Users/whqtker/Documents/SSAFY/ssafy-race
git add Bot_Java/MyCar.java Bot_Java/TestRunner.java
git commit -m "feat: add K4/alpha to TrackParams; rebalance steering gains"
```

---

## Task 2: computeSteering — PD 미분항 + EMA smoothing 추가

**Files:**
- Modify: `Bot_Java/MyCar.java:65-72` (computeSteering 함수)
- Modify: `Bot_Java/MyCar.java:235-240` (control_driving 호출부 — 임시로 0f,0f 전달)
- Modify: `Bot_Java/TestRunner.java:42-80` (testSteering)

- [ ] **Step 1: testSteering 업데이트 (실패 케이스 포함)**

`TestRunner.java`의 `testSteering()` 전체를 아래로 교체한다.

```java
static void testSteering() {
    MyCar.TrackParams p = MyCar.PARAMS[MyCar.TRACK_BASIC];
    float[] straight = new float[20];

    // s1: 중앙 정렬, 직선 → 거의 0
    float s1 = MyCar.computeSteering(0f, 9.25f, 0f, straight, p, 0f, 0f);
    assertTrue(Math.abs(s1) < 0.05f,
               "Centered car on straight should steer ~0, got: " + s1);
    System.out.println("  straight+centered: " + s1);

    // s2: 오른쪽 이탈 → 음수(왼쪽 보정)
    // centerError=-0.5405, dCenter=-0.5405, raw=0.45*(-0.5405)+0.25*(-0.5405)=-0.378
    // steering=0.4*(-0.378)=-0.151
    float s2 = MyCar.computeSteering(5f, 9.25f, 0f, straight, p, 0f, 0f);
    assertTrue(s2 < 0,
               "Car right of center should steer left (negative), got: " + s2);
    System.out.println("  right of center:   " + s2);

    // s3: 전방 우커브 → 양수
    float[] rightCurve = new float[20];
    for (int i = 0; i < 5; i++) rightCurve[i] = 30f;
    float s3 = MyCar.computeSteering(0f, 9.25f, 0f, rightCurve, p, 0f, 0f);
    assertTrue(s3 > 0,
               "Right curve ahead should steer right (positive), got: " + s3);
    System.out.println("  right curve ahead: " + s3);

    // s4: 모든 신호 최대 좌측, prevSteering=-1.0 → -1.0에 고정(클램프)
    // centerError=-1, dCenter=-1-1=-2, raw=-0.45-0.20-0.35+0.25*(-2)=-1.5→clamp-1
    // steering=0.4*(-1)+0.6*(-1)=-1.0
    float[] leftCurve90 = new float[20];
    for (int i = 0; i < 5; i++) leftCurve90[i] = -90f;
    float s4 = MyCar.computeSteering(9.25f, 9.25f, 90f, leftCurve90, p, 1.0f, -1.0f);
    assertTrue(Math.abs(s4 - (-1.0f)) < 0.001f,
               "All signals maxed left should yield -1.0, got: " + s4);
    System.out.println("  all-left maxed (clamped): " + s4);

    // EMA smoothing: prevSteering=0 → 출력은 alpha*raw 수준
    // to_middle=9.25, straight → centerError=-1, dCenter=-1, raw=-0.45-0.25=-0.70
    // steering=0.4*(-0.70)+0.6*0=-0.28
    float sEMA0 = MyCar.computeSteering(9.25f, 9.25f, 0f, straight, p, 0f, 0f);
    assertTrue(Math.abs(sEMA0 - (-0.28f)) < 0.01f,
               "EMA from neutral: expected ~-0.28, got: " + sEMA0);
    System.out.println("  EMA from neutral:  " + sEMA0);

    // EMA smoothing: prevSteering=-0.8 → 이전 값 반영돼 더 음수
    // steering=0.4*(-0.70)+0.6*(-0.8)=-0.28-0.48=-0.76
    float sEMA1 = MyCar.computeSteering(9.25f, 9.25f, 0f, straight, p, 0f, -0.8f);
    assertTrue(Math.abs(sEMA1 - (-0.76f)) < 0.01f,
               "EMA with prev=-0.8: expected ~-0.76, got: " + sEMA1);
    assertTrue(sEMA1 < sEMA0,
               "EMA: prev steering magnifies output, got sEMA0=" + sEMA0 + " sEMA1=" + sEMA1);
    System.out.println("  EMA with prev=-0.8: " + sEMA1);

    // D-term: 이탈 속도 빠를수록 보정 강화
    // prevCenterError=0 → dCenter=-1.0: raw=-0.45-0.25=-0.70, steering=0.4*(-0.70)=-0.28
    // prevCenterError=-0.5 → dCenter=-0.5: raw=-0.45-0.125=-0.575, steering=0.4*(-0.575)=-0.23
    float sDterm0 = MyCar.computeSteering(9.25f, 9.25f, 0f, straight, p, 0f, 0f);
    float sDterm1 = MyCar.computeSteering(9.25f, 9.25f, 0f, straight, p, -0.5f, 0f);
    assertTrue(sDterm0 < sDterm1,
               "D-term more aggressive when dCenter is large: sDterm0=" + sDterm0 + " sDterm1=" + sDterm1);
    System.out.println("  D-term dCenter=-1.0: " + sDterm0 + "  dCenter=-0.5: " + sDterm1);

    // D-term: 복귀 중이면 오버슈팅 억제 (prevCenterError=-1.0, now=0 → dCenter=+1.0)
    // raw=0.45*0+0.25*1.0=0.25, steering=0.4*0.25+0.6*(-0.4)=0.10-0.24=-0.14
    // 비교: prevCenterError=0, same → dCenter=0, raw=0, steering=0.4*0+0.6*(-0.4)=-0.24
    float sDterm2 = MyCar.computeSteering(0f, 9.25f, 0f, straight, p, -1.0f, -0.4f);
    float sDterm3 = MyCar.computeSteering(0f, 9.25f, 0f, straight, p, 0f, -0.4f);
    assertTrue(sDterm2 > sDterm3,
               "D-term reduces overcorrection when returning: sDterm2=" + sDterm2 + " sDterm3=" + sDterm3);
    System.out.println("  D-term anti-overshoot: " + sDterm2 + " vs no-D: " + sDterm3);

    System.out.println("PASS: steering");
}
```

- [ ] **Step 2: 컴파일하여 실패 확인**

```bash
cd /Users/whqtker/Documents/SSAFY/ssafy-race/Bot_Java
javac -cp . MyCar.java TestRunner.java 2>&1
java -cp . TestRunner 2>&1
```

Expected: testSteering에서 `FAIL: EMA from neutral` 또는 컴파일 에러 (5-인수 computeSteering에 7개 인수 전달).

- [ ] **Step 3: computeSteering 업데이트**

`MyCar.java`의 `computeSteering` 메서드 전체를 아래로 교체한다.

```java
static float computeSteering(float toMiddle, float halfRoadLimit,
                              float movingAngle, float[] angles,
                              TrackParams p,
                              float prevCenterError, float prevSteering) {
    float centerError    = -(toMiddle / halfRoadLimit);
    float angleError     = -(movingAngle / 90.0f);
    float lookaheadAngle = computeLookaheadAngle(angles, p);
    float dCenter        = centerError - prevCenterError;
    float raw = p.K1 * centerError + p.K2 * angleError
              + p.K3 * lookaheadAngle + p.K4 * dCenter;
    return clamp(p.alpha * raw + (1f - p.alpha) * prevSteering, -1.0f, 1.0f);
}
```

- [ ] **Step 4: control_driving 호출부 임시 수정 (0f, 0f 전달)**

`control_driving` 안의 `computeSteering` 호출을 아래와 같이 수정한다. (Task 3에서 실제 상태 변수로 교체)

```java
car_controls.steering = computeSteering(
    sensing_info.to_middle,
    sensing_info.half_road_limit,
    sensing_info.moving_angle,
    angles, p,
    0f, 0f
);
```

- [ ] **Step 5: 컴파일 및 테스트 통과 확인**

```bash
cd /Users/whqtker/Documents/SSAFY/ssafy-race/Bot_Java
javac -cp . MyCar.java TestRunner.java 2>&1 && java -cp . TestRunner 2>&1
```

Expected 출력 (일부):
```
PASS: TrackParams
PASS: track detection
  straight+centered: 0.0
  right of center:   -0.15...
  right curve ahead: 0.04...
  all-left maxed (clamped): -1.0
  EMA from neutral:  -0.28
  EMA with prev=-0.8: -0.76
  D-term dCenter=-1.0: -0.28  dCenter=-0.5: -0.23
  D-term anti-overshoot: -0.14 vs no-D: -0.24
PASS: steering
...
PASS: stuck detector
```

`FAIL:` 없어야 한다.

- [ ] **Step 6: 커밋**

```bash
cd /Users/whqtker/Documents/SSAFY/ssafy-race
git add Bot_Java/MyCar.java Bot_Java/TestRunner.java
git commit -m "feat: PD controller + EMA smoothing in computeSteering"
```

---

## Task 3: 상태 변수 추가 및 control_driving 배선

**Files:**
- Modify: `Bot_Java/MyCar.java:153-165` (인스턴스 변수 블록)
- Modify: `Bot_Java/MyCar.java:235-280` (control_driving 호출부 + StuckDetector 블록)

- [ ] **Step 1: prevCenterError · prevSteering 인스턴스 변수 추가**

`MyCar.java`의 `// --- State ---` 블록을 아래와 같이 수정한다.

```java
// --- State ---
private boolean trackInitialized = false;
private int trackType = TRACK_BASIC;
private int stuckTicks   = 0;
private int reverseTicks = 0;
private float prevCenterError = 0f;
private float prevSteering    = 0f;
```

- [ ] **Step 2: control_driving — computeSteering 호출부에 상태 변수 연결**

Task 2에서 `0f, 0f`로 임시 처리한 줄을 아래로 교체한다.

```java
float centerError = -(sensing_info.to_middle / sensing_info.half_road_limit);
car_controls.steering = computeSteering(
    sensing_info.to_middle,
    sensing_info.half_road_limit,
    sensing_info.moving_angle,
    angles, p,
    prevCenterError, prevSteering
);
```

- [ ] **Step 3: StuckDetector 블록 — 정상 주행 시 상태 갱신, 역방향 시 건너뜀**

기존 StuckDetector 블록 전체를 아래로 교체한다.

```java
// StuckDetector
if (reverseTicks > 0) {
    car_controls.throttle = -0.5f;
    car_controls.brake    = 0f;
    car_controls.steering = clamp(-(sensing_info.to_middle / sensing_info.half_road_limit),
                                  -1.0f, 1.0f);
    reverseTicks--;
    if (reverseTicks == 0) {
        stuckTicks = 0;
        System.out.printf("[REVERSE] DONE at progress=%.1f%n", sensing_info.lap_progress);
        logEvent(String.format("# REVERSE DONE at progress=%.1f", sensing_info.lap_progress));
    }
    // 역방향 중: EMA 상태 갱신 건너뜀 (후진 steering이 정상 주행 EMA를 오염하지 않도록)
} else {
    // EMA 상태 갱신
    prevSteering    = car_controls.steering;
    prevCenterError = centerError;

    if (sensing_info.lap_progress > 1f && isStuck(sensing_info.speed, car_controls.throttle)) {
        stuckTicks++;
        if (stuckTicks == 5) {
            reverseTicks = 20;
            System.out.printf("[STUCK] progress=%.1f speed=%.1f%n",
                              sensing_info.lap_progress, sensing_info.speed);
            logEvent(String.format("# STUCK at progress=%.1f speed=%.1f",
                                   sensing_info.lap_progress, sensing_info.speed));
            System.out.printf("[REVERSE] START at progress=%.1f%n", sensing_info.lap_progress);
            logEvent(String.format("# REVERSE START at progress=%.1f", sensing_info.lap_progress));
        }
    } else {
        stuckTicks = 0;
    }
}
```

- [ ] **Step 4: 컴파일 및 전체 테스트 통과 확인**

```bash
cd /Users/whqtker/Documents/SSAFY/ssafy-race/Bot_Java
javac -cp . MyCar.java TestRunner.java 2>&1 && java -cp . TestRunner 2>&1
```

Expected: `FAIL:` 없이 모든 PASS. 마지막 줄 `TestRunner ready.`

- [ ] **Step 5: 커밋**

```bash
cd /Users/whqtker/Documents/SSAFY/ssafy-race
git add Bot_Java/MyCar.java
git commit -m "feat: wire prevCenterError/prevSteering state into control_driving"
```

---

## 검증

Windows에서 pull 후 시뮬레이터 실행, 한 바퀴 완주 후 `logs/` CSV를 Mac으로 push.

```python
# 빠른 검증 스크립트 (로컬 실행)
import csv, statistics
rows = list(csv.DictReader(open("logs/<run_file>.csv")))
middles = [abs(float(r['to_middle'])) for r in rows if r['to_middle']]
steerings = [float(r['steering']) for r in rows if r['steering']]
flips = sum(1 for i in range(1, len(steerings)) if steerings[i]*steerings[i-1] < 0)
print(f"to_middle 평균절댓값: {statistics.mean(middles):.2f}m  (목표: <3.0m)")
print(f"steering 부호전환: {flips}회  (목표: <20회)")
```

목표:
- `to_middle` 평균 절댓값 **< 3.0m** (현재 6.3m)
- steering 부호 전환 **< 20회** (현재 54회)
- 최대 속도 **113km/h 수준 유지**
- 완주 가능
