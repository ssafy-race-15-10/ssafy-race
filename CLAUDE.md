# SSAFY Race

Autonomous driving racing bot for the SSAFY competition. Controls a vehicle in AirSim (Unreal Engine 4) via algorithm.

## Critical Constraint

**Only `MyCar.java` is submitted.** No other file changes are reflected on the server.

Allowed in `MyCar.java`: member variable declarations, `control_driving` body, custom functions/classes.

## Architecture

```
DrivingInterface.dll  →  calls control_driving() every 0.1 seconds
DrivingInterface.java →  raw data processing (do not modify)
MyCar.java            →  driving logic (the only file to edit and submit)
TestRunner.java       →  local unit test harness (not submitted)
```

`car_controls` is the static `MyCar` instance — write `car_controls.steering / .throttle / .brake` directly.

## Key References

- Sensing & Control API: `docs/api-reference.md`
- Tracks, settings.json, simulator shortcuts: `docs/simulator.md`
- Algorithm design: `docs/superpowers/specs/2026-05-31-driving-algorithm-design.md`
- Implementation plan: `docs/superpowers/plans/2026-05-31-driving-algorithm.md`

## Environment

- Language: Java (JDK 1.8+), IntelliJ or Eclipse
- Simulator OS: Windows 7 / Windows 10 64-bit
