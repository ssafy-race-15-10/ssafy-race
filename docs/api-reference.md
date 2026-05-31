# API Reference

## Sensing API (`sensing_info` — `DrivingInterface.CarStateValues`)

| Field | Type | Description |
|-------|------|-------------|
| `to_middle` | float | Distance from road center (m). Positive = right, negative = left |
| `speed` | float | Current speed (km/h) |
| `moving_forward` | float | 1.0 = forward, 0.0 = reverse |
| `moving_angle` | float | Alignment angle with road. 0 = parallel, + = right-leaning, − = left-leaning |
| `collided` | bool | Whether currently colliding |
| `lap_progress` | float | Completion percentage 0–100 |
| `track_forward_angles` | ArrayList\<Float\> | Road angles for 20 segments × 10m = 200m ahead. Positive = right curve |
| `track_forward_obstacles` | ArrayList\<ObstaclesInfo\> | Obstacles within 200m. Each: `{dist, to_middle}`. Width fixed at 2m (±1m) |
| `opponent_cars_info` | ArrayList\<CarsInfo\> | Opponents within ±200m. Each: `{dist, to_middle, speed}`. dist positive = ahead |
| `distance_to_way_points` | ArrayList\<Float\> | Straight-line distances to next 20 waypoints |
| `half_road_limit` | float | Road-out threshold = half road width + 1.25m (half car width) |

## Control API (`car_controls` — static `MyCar` instance)

| Field | Range | Description |
|-------|-------|-------------|
| `steering` | −1.0 to +1.0 | Positive = right, negative = left |
| `throttle` | −1.0 to +1.0 | Positive = forward, negative = reverse (auto gear) |
| `brake` | 0.0 to 1.0 | Decelerate/stop, independent of throttle |

Off-road penalty: simulator forces `brake = 0.9` when car goes out of bounds.

## Java Types Note

- `track_forward_angles` is `ArrayList<Float>` — use `toFloatArray()` helper before passing to static methods
- `track_forward_obstacles` is `ArrayList<DrivingInterface.ObstaclesInfo>`
- `opponent_cars_info` is `ArrayList<DrivingInterface.CarsInfo>`
- `car_controls` is the static `MyCar` field — write `car_controls.steering` etc. directly inside `control_driving`
