import DrivingInterface.*;

public class TestRunner {
    public static void main(String[] args) {
        testTrackParams();
        testTrackDetection();
        testSteering();
        testSpeed();
        testObstacleHandler();
        System.out.println("TestRunner ready. Add test calls here.");
    }

    static void testTrackParams() {
        MyCar.TrackParams basic = MyCar.PARAMS[MyCar.TRACK_BASIC];
        assertTrue(basic.maxSpeed == 130f,       "BASIC maxSpeed");
        assertTrue(basic.minSpeed == 40f,        "BASIC minSpeed");
        assertTrue(basic.slowdownFactor == 0.8f, "BASIC slowdownFactor");
        assertTrue(basic.steerLookAhead == 5,    "BASIC steerLookAhead");
        assertTrue(basic.speedLookAhead == 6,    "BASIC speedLookAhead");
        assertTrue(basic.K1 == 0.3f,            "BASIC K1");
        assertTrue(basic.brakeRange == 40f,      "BASIC brakeRange");

        MyCar.TrackParams speed = MyCar.PARAMS[MyCar.TRACK_SPEED];
        assertTrue(speed.maxSpeed == 120f,       "SPEED maxSpeed");
        assertTrue(speed.slowdownFactor == 0.9f, "SPEED slowdownFactor");
        assertTrue(speed.steerLookAhead == 6,    "SPEED steerLookAhead");
        assertTrue(speed.K2 == 0.4f,            "SPEED K2");

        MyCar.TrackParams ssafy = MyCar.PARAMS[MyCar.TRACK_SSAFY];
        assertTrue(ssafy.maxSpeed == 110f,       "SSAFY maxSpeed");
        assertTrue(ssafy.slowdownFactor == 1.0f, "SSAFY slowdownFactor");
        assertTrue(ssafy.decayFactor == 0.3f,   "SSAFY decayFactor");
        assertTrue(ssafy.accelerationRange == 25f, "SSAFY accelerationRange");

        MyCar.TrackParams germany = MyCar.PARAMS[MyCar.TRACK_GERMANY];
        assertTrue(germany.maxSpeed == 100f,       "GERMANY maxSpeed");
        assertTrue(germany.steerLookAhead == 8,    "GERMANY steerLookAhead");
        assertTrue(germany.speedLookAhead == 10,   "GERMANY speedLookAhead");
        assertTrue(germany.K3 == 0.4f,            "GERMANY K3");
        assertTrue(germany.brakeRange == 30f,      "GERMANY brakeRange");

        System.out.println("PASS: TrackParams");
    }

    static void testSteering() {
        MyCar.TrackParams p = MyCar.PARAMS[MyCar.TRACK_BASIC];

        // Straight road, centered, aligned → near-zero steering
        float[] straight = new float[20];
        float s1 = MyCar.computeSteering(0f, 9.25f, 0f, straight, p);
        assertTrue(Math.abs(s1) < 0.05f,
                   "Centered car on straight should steer ~0, got: " + s1);
        System.out.println("  straight+centered: " + s1);

        // Car to the right of center → should steer left (negative)
        float s2 = MyCar.computeSteering(5f, 9.25f, 0f, straight, p);
        assertTrue(s2 < 0,
                   "Car right of center should steer left (negative), got: " + s2);
        System.out.println("  right of center:   " + s2);

        // Right curve ahead → should steer right (positive)
        float[] rightCurve = new float[20];
        for (int i = 0; i < 5; i++) rightCurve[i] = 30f;
        float s3 = MyCar.computeSteering(0f, 9.25f, 0f, rightCurve, p);
        assertTrue(s3 > 0,
                   "Right curve ahead should steer right (positive), got: " + s3);
        System.out.println("  right curve ahead: " + s3);

        // All three signals maxed left → raw = K1*(-1)+K2*(-1)+K3*(-1) = -1.0 → clamp holds
        float[] leftCurve90 = new float[20];
        for (int i = 0; i < 5; i++) leftCurve90[i] = -90f;
        float s4 = MyCar.computeSteering(9.25f, 9.25f, 90f, leftCurve90, p);
        assertTrue(Math.abs(s4 - (-1.0f)) < 0.001f,
                   "All signals maxed left should yield -1.0, got: " + s4);
        System.out.println("  all-left maxed (clamped): " + s4);

        System.out.println("PASS: steering");
    }

    static void testTrackDetection() {
        assertTrue(MyCar.detectTrackType(12.25f) == MyCar.TRACK_SSAFY,
                   "half_road_limit 12.25 should be SSAFY");
        assertTrue(MyCar.detectTrackType(8.25f) == MyCar.TRACK_GERMANY,
                   "half_road_limit 8.25 should be GERMANY");
        assertTrue(MyCar.detectTrackType(9.25f) == MyCar.TRACK_BASIC,
                   "half_road_limit 9.25 should be BASIC");
        assertTrue(MyCar.detectTrackType(9.0f) == MyCar.TRACK_BASIC,
                   "half_road_limit 9.0 boundary should be BASIC");
        assertTrue(MyCar.detectTrackType(11.1f) == MyCar.TRACK_SSAFY,
                   "half_road_limit 11.1 should be SSAFY");
        System.out.println("PASS: track detection");
    }

    static void testSpeed() {
        MyCar.TrackParams p = MyCar.PARAMS[MyCar.TRACK_BASIC];

        // Straight road → target equals maxSpeed
        float[] straight = new float[20];
        float t1 = MyCar.computeTargetSpeed(straight, p);
        assertTrue(t1 == 130f,
                   "Straight road target should be maxSpeed (130), got: " + t1);
        System.out.println("  straight target speed: " + t1);

        // Sharp curve (angle=60) → reduced speed
        float[] sharp = new float[20];
        sharp[0] = 60f;
        float t2 = MyCar.computeTargetSpeed(sharp, p);
        assertTrue(t2 < 100f,
                   "Sharp curve should reduce speed below 100, got: " + t2);
        System.out.println("  sharp curve target:    " + t2);

        // Extreme curve → clamped to minSpeed
        float[] extreme = new float[20];
        for (int i = 0; i < 6; i++) extreme[i] = 120f;
        float t3 = MyCar.computeTargetSpeed(extreme, p);
        assertTrue(t3 == 40f,
                   "Extreme curve should clamp to minSpeed (40), got: " + t3);
        System.out.println("  extreme curve target:  " + t3);

        System.out.println("PASS: speed control");
    }

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

        // Obstacle at 0m, to_middle=+9.25 (far right) → steer left (negative), clamped to -1.0
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
    }

    static void assertTrue(boolean condition, String message) {
        if (!condition) throw new AssertionError("FAIL: " + message);
    }
}
