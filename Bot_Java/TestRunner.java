import DrivingInterface.*;

public class TestRunner {
    public static void main(String[] args) {
        testTrackParams();
        testTrackDetection();
        testSteering();
        testSpeed();
        testObstacleHandler();
        testStuckDetector();
        System.out.println("TestRunner ready. Add test calls here.");
    }

    static void testTrackParams() {
        MyCar.TrackParams basic = MyCar.PARAMS[MyCar.TRACK_BASIC];
        assertTrue(basic.maxSpeed == 130f,         "BASIC maxSpeed");
        assertTrue(basic.minSpeed == 40f,          "BASIC minSpeed");
        assertTrue(basic.slowdownFactor == 0.8f,   "BASIC slowdownFactor");
        assertTrue(basic.steerLookAhead == 5,      "BASIC steerLookAhead");
        assertTrue(basic.speedLookAhead == 6,      "BASIC speedLookAhead");
        assertTrue(basic.K_stanley == 5.0f,        "BASIC K_stanley");
        assertTrue(basic.K3 == 0.35f,             "BASIC K3");
        assertTrue(basic.brakeRange == 40f,        "BASIC brakeRange");

        MyCar.TrackParams speed = MyCar.PARAMS[MyCar.TRACK_SPEED];
        assertTrue(speed.maxSpeed == 120f,         "SPEED maxSpeed");
        assertTrue(speed.K_stanley == 4.0f,        "SPEED K_stanley");

        MyCar.TrackParams ssafy = MyCar.PARAMS[MyCar.TRACK_SSAFY];
        assertTrue(ssafy.maxSpeed == 110f,         "SSAFY maxSpeed");
        assertTrue(ssafy.decayFactor == 0.3f,      "SSAFY decayFactor");
        assertTrue(ssafy.K_stanley == 3.0f,        "SSAFY K_stanley");

        MyCar.TrackParams germany = MyCar.PARAMS[MyCar.TRACK_GERMANY];
        assertTrue(germany.maxSpeed == 100f,       "GERMANY maxSpeed");
        assertTrue(germany.K3 == 0.40f,           "GERMANY K3");
        assertTrue(germany.K_stanley == 5.5f,      "GERMANY K_stanley");
        assertTrue(germany.brakeRange == 30f,      "GERMANY brakeRange");

        System.out.println("PASS: TrackParams");
    }

    static void testSteering() {
        MyCar.TrackParams p = MyCar.PARAMS[MyCar.TRACK_BASIC];
        float[] straight = new float[20];

        // s1: 중앙 정렬, 직선 → 0
        float s1 = MyCar.computeSteering(0f, 9.25f, 0f, 10f, straight, p);
        assertTrue(Math.abs(s1) < 0.001f,
                   "Centered car on straight should steer 0, got: " + s1);
        System.out.println("  straight+centered: " + s1);

        // s2: 오른쪽 5m 이탈, 36km/h → 음수 (왼쪽 보정)
        // centering = -atan(5.0*5/10.0)/(π/2) = -0.758
        float s2 = MyCar.computeSteering(5f, 9.25f, 0f, 36f, straight, p);
        assertTrue(s2 < 0,
                   "Car right of center should steer left, got: " + s2);
        assertTrue(Math.abs(s2 - (-0.758f)) < 0.01f,
                   "Right 5m at 36km/h: expected ~-0.758, got: " + s2);
        System.out.println("  right of center:   " + s2);

        // s3: 전방 우커브 → 양수
        float[] rightCurve = new float[20];
        for (int i = 0; i < 5; i++) rightCurve[i] = 30f;
        float s3 = MyCar.computeSteering(0f, 9.25f, 0f, 36f, rightCurve, p);
        assertTrue(s3 > 0,
                   "Right curve ahead should steer right, got: " + s3);
        System.out.println("  right curve ahead: " + s3);

        // s4: 차체 30도 기울어짐(오른쪽), 중앙, 직선 → 음수 (heading 보정)
        // heading = -(30/90) = -0.333
        float s4 = MyCar.computeSteering(0f, 9.25f, 30f, 36f, straight, p);
        assertTrue(s4 < 0,
                   "Car pointing right should steer left to correct heading, got: " + s4);
        assertTrue(Math.abs(s4 - (-0.333f)) < 0.01f,
                   "Heading 30deg: expected ~-0.333, got: " + s4);
        System.out.println("  heading correction: " + s4);

        // s5: Stanley 속도 적응성 — 같은 위치, 저속일수록 보정 강함
        float sLow  = MyCar.computeSteering(5f, 9.25f, 0f, 20f,  straight, p);
        float sHigh = MyCar.computeSteering(5f, 9.25f, 0f, 100f, straight, p);
        assertTrue(Math.abs(sLow) > Math.abs(sHigh),
                   "Lower speed should give stronger centering: low=" + sLow + " high=" + sHigh);
        System.out.println("  speed-adaptive (20km/h):" + sLow + "  (100km/h):" + sHigh);

        // s6: 모든 신호 최대 왼쪽 → 클램프 -1.0
        float[] leftCurve90 = new float[20];
        for (int i = 0; i < 5; i++) leftCurve90[i] = -90f;
        float s6 = MyCar.computeSteering(9.25f, 9.25f, 90f, 36f, leftCurve90, p);
        assertTrue(Math.abs(s6 - (-1.0f)) < 0.001f,
                   "All signals maxed left should clamp to -1.0, got: " + s6);
        System.out.println("  all-left maxed (clamped): " + s6);

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
        assertTrue(Math.abs(a2) < 0.001f, "Obstacle at exactly 25m: avoidance should be ~0, got: " + a2);

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

    static void testStuckDetector() {
        // Stuck: speed < 3 AND throttle > 0.3
        assertTrue(MyCar.isStuck(1f, 0.5f),    "speed=1 throttle=0.5 → stuck");
        assertTrue(MyCar.isStuck(0f, 1.0f),    "speed=0 throttle=1.0 → stuck");
        assertTrue(MyCar.isStuck(2.9f, 0.31f), "speed=2.9 throttle=0.31 → stuck");

        // Not stuck
        assertTrue(!MyCar.isStuck(50f, 0.8f),  "speed=50 throttle=0.8 → not stuck");
        assertTrue(!MyCar.isStuck(1f, 0.2f),   "speed=1 throttle=0.2 → not stuck");
        assertTrue(!MyCar.isStuck(3f, 0.5f),   "speed=3.0 throttle=0.5 → boundary, not stuck");

        System.out.println("PASS: stuck detector");
    }

    static void assertTrue(boolean condition, String message) {
        if (!condition) throw new AssertionError("FAIL: " + message);
    }
}
