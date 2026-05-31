import DrivingInterface.*;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MyCar {

    boolean is_debug = false;
    static boolean enable_api_control = true; // true(Controlled by code) /false(Controlled by keyboard)

    // --- Track type constants ---
    static final int TRACK_BASIC   = 0;
    static final int TRACK_SPEED   = 1;
    static final int TRACK_SSAFY   = 2;
    static final int TRACK_GERMANY = 3;

    // --- TrackParams inner class ---
    static class TrackParams {
        final float maxSpeed, minSpeed, slowdownFactor;
        final float K1, K2, K3, decayFactor;
        final float accelerationRange, brakeRange;
        final int steerLookAhead, speedLookAhead;

        TrackParams(float maxSpeed, float minSpeed, float slowdownFactor,
                    int steerLookAhead, int speedLookAhead,
                    float K1, float K2, float K3, float decayFactor,
                    float accelerationRange, float brakeRange) {
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
        }
    }

    static final TrackParams[] PARAMS = {
        // BASIC:   maxSpd minSpd slow  steerLA speedLA K1    K2    K3    decay  accR  brkR
        new TrackParams(130, 40, 0.8f,  5,  6, 0.3f, 0.4f, 0.3f, 0.4f, 30, 40),
        // SPEED
        new TrackParams(120, 35, 0.9f,  6,  7, 0.3f, 0.4f, 0.3f, 0.4f, 30, 40),
        // SSAFY
        new TrackParams(110, 30, 1.0f,  7,  8, 0.4f, 0.5f, 0.3f, 0.3f, 25, 35),
        // GERMANY
        new TrackParams(100, 25, 1.2f,  8, 10, 0.5f, 0.6f, 0.4f, 0.5f, 20, 30)
    };

    // --- Steering helpers ---
    static float computeLookaheadAngle(float[] angles, TrackParams p) {
        double wSum = 0, aSum = 0;
        int n = Math.min(p.steerLookAhead, angles.length);
        for (int i = 0; i < n; i++) {
            double w = Math.exp(-i * p.decayFactor);
            aSum += angles[i] * w;
            wSum += w;
        }
        return wSum == 0 ? 0f : (float) (aSum / wSum / 90.0);
    }

    static float computeSteering(float toMiddle, float halfRoadLimit,
                                  float movingAngle, float[] angles, TrackParams p) {
        float centerError    = -(toMiddle / halfRoadLimit);
        float angleError     = -(movingAngle / 90.0f);
        float lookaheadAngle = computeLookaheadAngle(angles, p);
        float raw = p.K1 * centerError + p.K2 * angleError + p.K3 * lookaheadAngle;
        return clamp(raw, -1.0f, 1.0f);
    }

    static float clamp(float v, float min, float max) {
        return Math.max(min, Math.min(max, v));
    }

    static float computeTargetSpeed(float[] angles, TrackParams p) {
        float maxCurve = 0;
        int n = Math.min(p.speedLookAhead, angles.length);
        for (int i = 0; i < n; i++) {
            maxCurve = Math.max(maxCurve, Math.abs(angles[i]));
        }
        return clamp(p.maxSpeed - p.slowdownFactor * maxCurve, p.minSpeed, p.maxSpeed);
    }

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

    static float[] toFloatArray(java.util.ArrayList<Float> list) {
        float[] arr = new float[list.size()];
        for (int i = 0; i < list.size(); i++) arr[i] = list.get(i);
        return arr;
    }

    // --- Obstacle avoidance ---
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

    static float nearestObstacleDist(java.util.ArrayList<DrivingInterface.ObstaclesInfo> obstacles) {
        float min = -1f;
        for (DrivingInterface.ObstaclesInfo obs : obstacles) {
            if (min < 0f || obs.dist < min) min = obs.dist;
        }
        return min;
    }

    static float computeObstacleSpeedCap(java.util.ArrayList<DrivingInterface.ObstaclesInfo> obstacles,
                                         float maxSpeed) {
        float nearestDist = nearestObstacleDist(obstacles);
        if (nearestDist < 0f || nearestDist >= 40f) return maxSpeed;
        float proximity = 1.0f - (nearestDist / 40f);
        return maxSpeed * (1.0f - 0.4f * proximity);
    }

    // --- Track detection helper ---
    static int detectTrackType(float halfRoadLimit) {
        if (halfRoadLimit > 11.0f) return TRACK_SSAFY;
        if (halfRoadLimit < 9.0f)  return TRACK_GERMANY;
        return TRACK_BASIC;
    }

    // --- State ---
    private boolean trackInitialized = false;
    private int trackType = TRACK_BASIC;
    private int recoveryTicks = 0;

    // --- Lap logger ---
    private PrintWriter logWriter = null;
    private long lapStartTime = 0;
    private float lastProgress = 0f;
    private int lapCount = 0;
    private int tickCount = 0;

    public void control_driving(boolean a1, float a2, float a3, float a4, float a5, float a6, float a7, float a8,
                                float[] a9, float[] a10, float[] a11, float[] a12) {

        // ===========================================================
        // Don't remove this area. ===================================
        // ===========================================================
        DrivingInterface di = new DrivingInterface();
        DrivingInterface.CarStateValues sensing_info = di.get_car_state(a1,a2,a3,a4,a5,a6,a7,a8,a9,a10,a11,a12);
        // ===========================================================

        if(is_debug) {
            System.out.println("=========================================================");
            System.out.println("[MyCar] to middle: " + sensing_info.to_middle);

            System.out.println("[MyCar] collided: " + sensing_info.collided);
            System.out.println("[MyCar] car speed: " + sensing_info.speed + "km/h");

            System.out.println("[MyCar] is moving forward: " + sensing_info.moving_forward);
            System.out.println("[MyCar] moving angle: " + sensing_info.moving_angle);
            System.out.println("[MyCar] lap_progress: " + sensing_info.lap_progress);

            StringBuilder forward_angles = new StringBuilder("[MyCar] track_forward_angles: ");
            for (Float track_forward_angle : sensing_info.track_forward_angles) {
                forward_angles.append(track_forward_angle).append(", ");
            }
            System.out.println(forward_angles);

            StringBuilder to_way_points = new StringBuilder("[MyCar] distance_to_way_points: ");
            for (Float distance_to_way_point : sensing_info.distance_to_way_points) {
                to_way_points.append(distance_to_way_point).append(", ");
            }
            System.out.println(to_way_points);

            StringBuilder forward_obstacles = new StringBuilder("[MyCar] track_forward_obstacles: ");
            for (DrivingInterface.ObstaclesInfo track_forward_obstacle : sensing_info.track_forward_obstacles) {
                forward_obstacles.append("{dist:").append(track_forward_obstacle.dist)
                        .append(", to_middle:").append(track_forward_obstacle.to_middle).append("}, ");
            }
            System.out.println(forward_obstacles);

            StringBuilder opponent_cars = new StringBuilder("[MyCar] opponent_cars_info: ");
            for (DrivingInterface.CarsInfo carsInfo : sensing_info.opponent_cars_info) {
                opponent_cars.append("{dist:").append(carsInfo.dist)
                        .append(", to_middle:").append(carsInfo.to_middle)
                        .append(", speed:").append(carsInfo.speed).append("km/h}, ");
            }
            System.out.println(opponent_cars);

            System.out.println("=========================================================");
        }

        // ===========================================================
        // Area for writing code about driving rule ==================
        // ===========================================================
        // Editing area starts from here
        //

        // Track detection: once on first call, upgrade Basic→Speed on first obstacle
        if (!trackInitialized) {
            trackType = detectTrackType(sensing_info.half_road_limit);
            trackInitialized = true;
        }
        if (trackType == TRACK_BASIC && !sensing_info.track_forward_obstacles.isEmpty()) {
            trackType = TRACK_SPEED;
        }

        TrackParams p = PARAMS[trackType];
        float[] angles = toFloatArray(sensing_info.track_forward_angles);

        car_controls.steering = computeSteering(
            sensing_info.to_middle,
            sensing_info.half_road_limit,
            sensing_info.moving_angle,
            angles, p
        );

        // Obstacle avoidance: blend avoidance signal into steering
        float avoidSteer = computeObstacleAvoidance(sensing_info.track_forward_obstacles,
                                                    sensing_info.half_road_limit);
        car_controls.steering = clamp(car_controls.steering + avoidSteer, -1.0f, 1.0f);

        float speedCap    = computeObstacleSpeedCap(sensing_info.track_forward_obstacles, p.maxSpeed);
        float targetSpeed = Math.min(computeTargetSpeed(angles, p), speedCap);
        applySpeedControl(sensing_info.speed, targetSpeed, p);

        // Collision recovery: brake hard for 20 ticks (~2s) after impact
        if (sensing_info.collided) recoveryTicks = 20;
        if (recoveryTicks > 0) {
            car_controls.throttle = 0.1f;
            car_controls.brake    = 0.6f;
            recoveryTicks--;
        }

        if(is_debug) {
            System.out.println("[MyCar] steering:"+car_controls.steering+
                                     ", throttle:"+car_controls.throttle+", brake:"+car_controls.brake);
        }

        // --- Lap logging ---
        if (logWriter == null) {
            try {
                new File("logs").mkdirs();
                String ts = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
                logWriter = new PrintWriter(new FileWriter("logs/run_" + ts + ".csv"));
                logWriter.println("tick,lap,progress,speed,to_middle,moving_angle,steering,throttle,brake,collided,elapsed_ms");
                lapStartTime = System.currentTimeMillis();
            } catch (IOException e) {
                System.out.println("[LOG] Cannot open log: " + e.getMessage());
            }
        }
        if (logWriter != null) {
            tickCount++;
            long elapsed = System.currentTimeMillis() - lapStartTime;
            logWriter.printf("%d,%d,%.1f,%.1f,%.3f,%.1f,%.3f,%.3f,%.3f,%b,%d%n",
                tickCount, lapCount + 1,
                sensing_info.lap_progress, sensing_info.speed,
                sensing_info.to_middle, sensing_info.moving_angle,
                car_controls.steering, car_controls.throttle, car_controls.brake,
                sensing_info.collided, elapsed);
            logWriter.flush();

            if (lastProgress > 90f && sensing_info.lap_progress < 10f) {
                lapCount++;
                logWriter.printf("# LAP %d COMPLETE: %.2fs%n", lapCount, elapsed / 1000.0);
                System.out.printf("[LOG] Lap %d: %.2fs%n", lapCount, elapsed / 1000.0);
                lapStartTime = System.currentTimeMillis();
            }
            lastProgress = sensing_info.lap_progress;
        }

        //
        // Editing area ends
        // =======================================================
    }

    // ===========================================================
    // Don't remove below area. ==================================
    // ===========================================================
    public native int StartDriving(boolean enable_api_control);

    static MyCar car_controls;

    float throttle;
    float steering;
    float brake;

    static {
        String[] candidates = {
            "DrivingInterface/DrivingInterface.dll",
            "Bot_Java/DrivingInterface/DrivingInterface.dll"
        };
        boolean loaded = false;
        for (String path : candidates) {
            File dll = new File(path);
            if (dll.exists()) {
                try {
                    System.load(dll.getAbsolutePath());
                    System.out.println("[DLL] Loaded: " + dll.getAbsolutePath());
                    loaded = true;
                } catch (UnsatisfiedLinkError e) {
                    System.out.println("[DLL] Load failed: " + e.getMessage());
                }
                break;
            }
        }
        if (!loaded) System.out.println("[DLL] Test mode: DLL not found");
    }

    public static void main(String[] args) {
        System.out.println("[MyCar] Start Bot! (JAVA)");

        car_controls = new MyCar();
        int return_code = car_controls.StartDriving(enable_api_control);

        System.out.println("[MyCar] End Bot! (JAVA), return_code = " + return_code);

        System.exit(return_code);
    }
    // ===========================================================
}
