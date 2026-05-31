# Obstacle Handling & Enhanced Logging Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add proactive obstacle avoidance, stuck/reverse escape, and enhanced CSV logging to `MyCar.java`.

**Architecture:** Two new static helper methods (`computeObstacleSpeedCap`, updated `computeObstacleAvoidance`) feed into a modified `applySpeedControl`, and an instance-level StuckDetector overrides outputs during reverse. All state lives in new member variables `stuckTicks` and `reverseTicks`.

**Tech Stack:** Java 8, JUnit-free (custom `assertTrue` in `TestRunner.java`)

---

## File Map

| File | Change |
|------|--------|
| `Bot_Java/MyCar.java` | Add `computeObstacleSpeedCap`, replace `computeObstacleAvoidance`, modify `applySpeedControl` signature, add StuckDetector state + logic, remove `recoveryTicks`, expand CSV logger |
| `Bot_Java/TestRunner.java` | Add `testObstacleHandler()`, `testStuckDetector()` |

---

### Task 1: ObstacleHandler — Speed Cap

**Files:**
- Modify: `Bot_Java/MyCar.java` — add `computeObstacleSpeedCap`, modify `applySpeedControl`
- Modify: `Bot_Java/TestRunner.java` — add `testObstacleHandler()`

- [ ] **Step 1: Write the failing test in `TestRunner.java`**

Add method and call in `main`:

```java
// In main():
testObstacleHandler();

// New method:
static void testObstacleHandler() {
    float maxSpeed = 130f;

    // No obstacles → cap equals maxSpeed
    java.util.ArrayList<DrivingInterface.ObstaclesInfo> none = new java.util.ArrayList<>();
    float c1 = MyCar.computeObstacleSpeedCap(none, maxSpeed);
    assertTrue(c1 == maxSpeed, "No obstacles: cap should equal maxSpeed, got: " + c1);

    // Obstacle at exactly 40m → no reduction (proximity = 0)
    java.util.ArrayList<DrivingInterface.ObstaclesInfo> far = new java.util.ArrayList<>();
    DrivingInterface.ObstaclesInfo o1 = new DrivingInterface.ObstaclesInfo();
    o1.dist = 40f; o1.to_middle = 0f;
    far.add(o1);
    float c2 = MyCar.computeObstacleSpeedCap(far, maxSpeed);
    assertTrue(Math.abs(c2 - maxSpeed) < 0.01f,
               "Obstacle at 40m: cap should equal maxSpeed, got: " + c2);

    // Obstacle at 0m → cap = maxSpeed * (1 - 0.4) = 78.0
    java.util.ArrayList<DrivingInterface.ObstaclesInfo> contact = new java.util.ArrayList<>();
    DrivingInterface.ObstaclesInfo o2 = new DrivingInterface.ObstaclesInfo();
    o2.dist = 0f; o2.to_middle = 0f;
    contact.add(o2);
    float c3 = MyCar.computeObstacleSpeedCap(contact, maxSpeed);
    assertTrue(Math.abs(c3 - 78f) < 0.01f,
               "Obstacle at 0m: cap should be 78.0, got: " + c3);

    // Obstacle at 20m → proximity=0.5, cap = 130*(1-0.4*0.5) = 130*0.8 = 104.0
    java.util.ArrayList<DrivingInterface.ObstaclesInfo> mid = new java.util.ArrayList<>();
    DrivingInterface.ObstaclesInfo o3 = new DrivingInterface.ObstaclesInfo();
    o3.dist = 20f; o3.to_middle = 0f;
    mid.add(o3);
    float c4 = MyCar.computeObstacleSpeedCap(mid, maxSpeed);
    assertTrue(Math.abs(c4 - 104f) < 0.01f,
               "Obstacle at 20m: cap should be 104.0, got: " + c4);

    System.out.println("PASS: obstacle speed cap");
}
```

- [ ] **Step 2: Run test to confirm it fails**

```
cd Bot_Java
javac -cp DrivingInterface DrivingInterface/DrivingInterface.java MyCar.java TestRunner.java
java -cp .;DrivingInterface TestRunner
```

Expected: `error: cannot find symbol — computeObstacleSpeedCap`

- [ ] **Step 3: Add `computeObstacleSpeedCap` to `MyCar.java`**

Add after `computeObstacleAvoidance`:

```java
static float computeObstacleSpeedCap(java.util.ArrayList<DrivingInterface.ObstaclesInfo> obstacles,
                                     float maxSpeed) {
    float nearestDist = Float.MAX_VALUE;
    for (DrivingInterface.ObstaclesInfo obs : obstacles) {
        if (obs.dist < nearestDist) nearestDist = obs.dist;
    }
    if (nearestDist >= 40f) return maxSpeed;
    float proximity = 1.0f - (nearestDist / 40f);
    return maxSpeed * (1.0f - 0.4f * proximity);
}
```

- [ ] **Step 4: Modify `applySpeedControl` to accept pre-computed `targetSpeed`**

Replace current signature:

```java
// BEFORE:
static void applySpeedControl(float currentSpeed, float[] angles, TrackParams p) {
    float targetSpeed = computeTargetSpeed(angles, p);
    float diff = targetSpeed - currentSpeed;
    if (diff > 0) {
        car_controls.throttle = Math.min(1.0f, diff / p.accelerationRange);
        car_controls.brake    = 0f;
    } else {
        car_controls.throttle = 0f;
        car_controls.brake    = Math.min(1.0f, -diff / p.brakeRange);
    }
}

// AFTER:
static void applySpeedControl(float currentSpeed, float targetSpeed, TrackParams p) {
    float diff = targetSpeed - currentSpeed;
    if (diff > 0) {
        car_controls.throttle = Math.min(1.0f, diff / p.accelerationRange);
        car_controls.brake    = 0f;
    } else {
        car_controls.throttle = 0f;
        car_controls.brake    = Math.min(1.0f, -diff / p.brakeRange);
    }
}
```

- [ ] **Step 5: Update `control_driving()` call site**

Replace:
```java
applySpeedControl(sensing_info.speed, angles, p);
```

With:
```java
float speedCap    = computeObstacleSpeedCap(sensing_info.track_forward_obstacles, p.maxSpeed);
float targetSpeed = Math.min(computeTargetSpeed(angles, p), speedCap);
applySpeedControl(sensing_info.speed, targetSpeed, p);
```

- [ ] **Step 6: Run tests to confirm passing**

```
javac -cp DrivingInterface DrivingInterface/DrivingInterface.java MyCar.java TestRunner.java
java -cp .;DrivingInterface TestRunner
```

Expected: `PASS: obstacle speed cap` and all prior tests still pass.

- [ ] **Step 7: Commit**

```bash
git add Bot_Java/MyCar.java Bot_Java/TestRunner.java
git commit -m "feat: add obstacle speed cap and update applySpeedControl signature"
```

---

### Task 2: ObstacleHandler — Avoidance Steering (Replace Existing)

**Files:**
- Modify: `Bot_Java/MyCar.java` — replace `computeObstacleAvoidance`, add `nearestObstacleDist`
- Modify: `Bot_Java/TestRunner.java` — add avoidance steering tests to `testObstacleHandler()`

- [ ] **Step 1: Add avoidance steering tests to `testObstacleHandler()`**

Append to `testObstacleHandler()` before the final `println`:

```java
// Avoidance steering: no obstacles → 0
float a1 = MyCar.computeObstacleAvoidance(none, 9.25f);
assertTrue(a1 == 0f, "No obstacles: avoidance should be 0, got: " + a1);

// Obstacle at 25m → proximity=0, avoidance=0
java.util.ArrayList<DrivingInterface.ObstaclesInfo> edge = new java.util.ArrayList<>();
DrivingInterface.ObstaclesInfo oe = new DrivingInterface.ObstaclesInfo();
oe.dist = 25f; oe.to_middle = 5f;
edge.add(oe);
float a2 = MyCar.computeObstacleAvoidance(edge, 9.25f);
assertTrue(a2 == 0f, "Obstacle at exactly 25m: avoidance should be 0, got: " + a2);

// Obstacle at 0m, to_middle=+9.25 (far right) → steer left (negative)
java.util.ArrayList<DrivingInterface.ObstaclesInfo> rightObs = new java.util.ArrayList<>();
DrivingInterface.ObstaclesInfo or1 = new DrivingInterface.ObstaclesInfo();
or1.dist = 0f; or1.to_middle = 9.25f;
rightObs.add(or1);
float a3 = MyCar.computeObstacleAvoidance(rightObs, 9.25f);
assertTrue(a3 < 0f, "Obstacle hard right at 0m: avoidance should be negative (steer left), got: " + a3);
assertTrue(Math.abs(a3 - (-1.0f)) < 0.001f, "Obstacle hard right at 0m: should clamp to -1.0, got: " + a3);

// Obstacle at 0m, to_middle=-9.25 (far left) → steer right (positive)
java.util.ArrayList<DrivingInterface.ObstaclesInfo> leftObs = new java.util.ArrayList<>();
DrivingInterface.ObstaclesInfo ol1 = new DrivingInterface.ObstaclesInfo();
ol1.dist = 0f; ol1.to_middle = -9.25f;
leftObs.add(ol1);
float a4 = MyCar.computeObstacleAvoidance(leftObs, 9.25f);
assertTrue(a4 > 0f, "Obstacle hard left at 0m: avoidance should be positive (steer right), got: " + a4);

System.out.println("PASS: obstacle avoidance steering");
```

- [ ] **Step 2: Run test to confirm the new assertions fail**

```
javac -cp DrivingInterface DrivingInterface/DrivingInterface.java MyCar.java TestRunner.java
java -cp .;DrivingInterface TestRunner
```

Expected: FAIL on `Obstacle at exactly 25m: avoidance should be 0` (old code uses 40m range).

- [ ] **Step 3: Replace `computeObstacleAvoidance` in `MyCar.java`**

Replace the entire existing method:

```java
static float computeObstacleAvoidance(java.util.ArrayList<DrivingInterface.ObstaclesInfo> obstacles,
                                      float halfRoadLimit) {
    DrivingInterface.ObstaclesInfo nearest = null;
    for (DrivingInterface.ObstaclesInfo obs : obstacles) {
        if (obs.dist < 25f && (nearest == null || obs.dist < nearest.dist)) {
            nearest = obs;
        }
    }
    if (nearest == null) return 0f;
    float proximity = 1.0f - (nearest.dist / 25f);
    float lateral   = nearest.to_middle / halfRoadLimit;
    return clamp(-2.0f * proximity * lateral, -1.0f, 1.0f);
}
```

- [ ] **Step 4: Add `nearestObstacleDist` helper (for logging)**

Add after `computeObstacleAvoidance`:

```java
static float nearestObstacleDist(java.util.ArrayList<DrivingInterface.ObstaclesInfo> obstacles) {
    float min = -1f;
    for (DrivingInterface.ObstaclesInfo obs : obstacles) {
        if (min < 0f || obs.dist < min) min = obs.dist;
    }
    return min;
}
```

- [ ] **Step 5: Run tests to confirm passing**

```
javac -cp DrivingInterface DrivingInterface/DrivingInterface.java MyCar.java TestRunner.java
java -cp .;DrivingInterface TestRunner
```

Expected: all tests pass including new avoidance steering assertions.

- [ ] **Step 6: Commit**

```bash
git add Bot_Java/MyCar.java Bot_Java/TestRunner.java
git commit -m "feat: replace obstacle avoidance with closest-obstacle 25m-range logic"
```

---

### Task 3: StuckDetector

**Files:**
- Modify: `Bot_Java/MyCar.java` — remove `recoveryTicks`, add `stuckTicks`/`reverseTicks`, add `isStuck` helper, add `logEvent` helper, wire into `control_driving`
- Modify: `Bot_Java/TestRunner.java` — add `testStuckDetector()`

- [ ] **Step 1: Write failing test in `TestRunner.java`**

Add method and call in `main`:

```java
// In main():
testStuckDetector();

// New method:
static void testStuckDetector() {
    // Stuck: speed < 3 AND throttle > 0.3
    assertTrue(MyCar.isStuck(1f, 0.5f),   "speed=1 throttle=0.5 → stuck");
    assertTrue(MyCar.isStuck(0f, 1.0f),   "speed=0 throttle=1.0 → stuck");
    assertTrue(MyCar.isStuck(2.9f, 0.31f),"speed=2.9 throttle=0.31 → stuck");

    // Not stuck
    assertTrue(!MyCar.isStuck(50f, 0.8f), "speed=50 throttle=0.8 → not stuck");
    assertTrue(!MyCar.isStuck(1f, 0.2f),  "speed=1 throttle=0.2 → not stuck");
    assertTrue(!MyCar.isStuck(3f, 0.5f),  "speed=3.0 throttle=0.5 → boundary, not stuck");

    System.out.println("PASS: stuck detector");
}
```

- [ ] **Step 2: Run test to confirm it fails**

```
javac -cp DrivingInterface DrivingInterface/DrivingInterface.java MyCar.java TestRunner.java
java -cp .;DrivingInterface TestRunner
```

Expected: `error: cannot find symbol — isStuck`

- [ ] **Step 3: Update state variables in `MyCar.java`**

Remove `recoveryTicks`, add `stuckTicks` and `reverseTicks`:

```java
// REMOVE this line:
private int recoveryTicks = 0;

// ADD these lines (alongside existing state):
private int stuckTicks   = 0;
private int reverseTicks = 0;
```

- [ ] **Step 4: Add `isStuck` and `logEvent` helpers to `MyCar.java`**

Add after `nearestObstacleDist`:

```java
static boolean isStuck(float speed, float throttle) {
    return speed < 3f && throttle > 0.3f;
}

private void logEvent(String msg) {
    if (logWriter != null) {
        logWriter.println(msg);
        logWriter.flush();
    }
}
```

- [ ] **Step 5: Replace old recovery block in `control_driving()` with StuckDetector**

Find and remove:
```java
        // Collision recovery: brake hard for 20 ticks (~2s) after impact
        if (sensing_info.collided) recoveryTicks = 20;
        if (recoveryTicks > 0) {
            car_controls.throttle = 0.1f;
            car_controls.brake    = 0.6f;
            recoveryTicks--;
        }
```

Replace with (place immediately after `applySpeedControl` call):
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
        } else {
            if (isStuck(sensing_info.speed, car_controls.throttle)) {
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

- [ ] **Step 6: Run tests to confirm passing**

```
javac -cp DrivingInterface DrivingInterface/DrivingInterface.java MyCar.java TestRunner.java
java -cp .;DrivingInterface TestRunner
```

Expected: `PASS: stuck detector` and all prior tests still pass.

- [ ] **Step 7: Commit**

```bash
git add Bot_Java/MyCar.java Bot_Java/TestRunner.java
git commit -m "feat: add StuckDetector with reverse escape, remove recoveryTicks"
```

---

### Task 4: Enhanced CSV Logging

**Files:**
- Modify: `Bot_Java/MyCar.java` — expand CSV header and row format

- [ ] **Step 1: Update CSV header in `control_driving()`**

Find:
```java
logWriter.println("tick,lap,progress,speed,to_middle,moving_angle,steering,throttle,brake,collided,elapsed_ms");
```

Replace with:
```java
logWriter.println("tick,lap,progress,speed,to_middle,moving_angle," +
                  "steering,throttle,brake,collided," +
                  "obs_count,obs_nearest_dist,target_speed," +
                  "stuck_ticks,reverse_ticks,elapsed_ms");
```

- [ ] **Step 2: Store `targetSpeed` and `nearestDist` as local variables**

In `control_driving()`, the existing lines:
```java
float speedCap    = computeObstacleSpeedCap(sensing_info.track_forward_obstacles, p.maxSpeed);
float targetSpeed = Math.min(computeTargetSpeed(angles, p), speedCap);
applySpeedControl(sensing_info.speed, targetSpeed, p);
```

Already have `targetSpeed`. Add `nearestDist` right after:
```java
float nearestDist = nearestObstacleDist(sensing_info.track_forward_obstacles);
```

- [ ] **Step 3: Update the per-tick CSV write**

Find the existing `logWriter.printf(...)` call and replace with:

```java
logWriter.printf("%d,%d,%.1f,%.1f,%.3f,%.1f,%.3f,%.3f,%.3f,%b,%d,%.1f,%.1f,%d,%d,%d%n",
    tickCount, lapCount + 1,
    sensing_info.lap_progress, sensing_info.speed,
    sensing_info.to_middle, sensing_info.moving_angle,
    car_controls.steering, car_controls.throttle, car_controls.brake,
    sensing_info.collided,
    sensing_info.track_forward_obstacles.size(),
    nearestDist,
    targetSpeed,
    stuckTicks, reverseTicks,
    elapsed);
logWriter.flush();
```

- [ ] **Step 4: Add OBSTACLE CLOSE event marker**

After the per-tick write, add:
```java
if (nearestDist >= 0f && nearestDist < 15f) {
    float avoidLogged = computeObstacleAvoidance(sensing_info.track_forward_obstacles,
                                                 sensing_info.half_road_limit);
    logEvent(String.format("# OBSTACLE CLOSE: dist=%.1f avoid_steer=%.3f",
                           nearestDist, avoidLogged));
}
```

- [ ] **Step 5: Compile and verify CSV output**

```
javac -cp DrivingInterface DrivingInterface/DrivingInterface.java MyCar.java TestRunner.java
java -cp .;DrivingInterface TestRunner
```

Expected: all tests pass (no runtime changes, compile check only).

- [ ] **Step 6: Commit**

```bash
git add Bot_Java/MyCar.java
git commit -m "feat: expand CSV log with obstacle, target speed, and stuck state columns"
```

---

### Task 5: Console Cleanup + Final Integration

**Files:**
- Modify: `Bot_Java/MyCar.java` — remove old `[LOG]` lap print, verify console output is event-only

- [ ] **Step 1: Update lap-complete console output**

Find the existing lap-complete console line:
```java
System.out.printf("[LOG] Lap %d: %.2fs%n", lapCount, elapsed / 1000.0);
```

Replace with:
```java
System.out.printf("[LAP %d] %.2fs%n", lapCount, elapsed / 1000.0);
```

- [ ] **Step 2: Verify no stray per-tick println outside `is_debug` block**

Scan `control_driving()` for any `System.out.println` or `System.out.printf` calls not inside `if(is_debug)` and not one of: `[STUCK]`, `[REVERSE]`, `[LAP N]`. Remove any found.

- [ ] **Step 3: Run full test suite**

```
javac -cp DrivingInterface DrivingInterface/DrivingInterface.java MyCar.java TestRunner.java
java -cp .;DrivingInterface TestRunner
```

Expected output (exactly):
```
PASS: TrackParams
PASS: track detection
PASS: steering
PASS: speed control
PASS: obstacle speed cap
PASS: obstacle avoidance steering
PASS: stuck detector
TestRunner ready. Add test calls here.
```

- [ ] **Step 4: Final commit**

```bash
git add Bot_Java/MyCar.java
git commit -m "chore: clean up console output to event-only"
```
