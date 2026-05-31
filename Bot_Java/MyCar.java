import DrivingInterface.*;

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

    // --- Track detection helper ---
    static int detectTrackType(float halfRoadLimit) {
        if (halfRoadLimit > 11.0f) return TRACK_SSAFY;
        if (halfRoadLimit < 9.0f)  return TRACK_GERMANY;
        return TRACK_BASIC;
    }

    // --- State ---
    private boolean trackInitialized = false;
    private int trackType = TRACK_BASIC;

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

        // Moving straight forward
        car_controls.steering = 0;
        car_controls.throttle = 1;
        car_controls.brake = 0;

        if(is_debug) {
            System.out.println("[MyCar] steering:"+car_controls.steering+
                                     ", throttle:"+car_controls.throttle+", brake:"+car_controls.brake);
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
        try {
            System.loadLibrary("DrivingInterface/DrivingInterface");
        } catch (UnsatisfiedLinkError e) {
            System.out.println("[MyCar] Test mode: native library not loaded");
        }
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
