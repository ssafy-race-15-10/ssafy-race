public class TestRunner {
    public static void main(String[] args) {
        testTrackParams();
        testTrackDetection();
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

    static void assertTrue(boolean condition, String message) {
        if (!condition) throw new AssertionError("FAIL: " + message);
    }
}
