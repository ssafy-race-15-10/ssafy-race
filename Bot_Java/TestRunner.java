public class TestRunner {
    public static void main(String[] args) {
        testTrackParams();
        System.out.println("TestRunner ready. Add test calls here.");
    }

    static void testTrackParams() {
        MyCar.TrackParams basic = MyCar.PARAMS[MyCar.TRACK_BASIC];
        assertTrue(basic.maxSpeed == 130f, "BASIC maxSpeed should be 130");
        assertTrue(basic.minSpeed == 40f,  "BASIC minSpeed should be 40");
        assertTrue(basic.steerLookAhead == 5, "BASIC steerLookAhead should be 5");
        assertTrue(basic.speedLookAhead == 6, "BASIC speedLookAhead should be 6");

        MyCar.TrackParams germany = MyCar.PARAMS[MyCar.TRACK_GERMANY];
        assertTrue(germany.maxSpeed == 100f, "GERMANY maxSpeed should be 100");
        assertTrue(germany.steerLookAhead == 8, "GERMANY steerLookAhead should be 8");
        assertTrue(germany.speedLookAhead == 10, "GERMANY speedLookAhead should be 10");

        System.out.println("PASS: TrackParams");
    }

    static void assertTrue(boolean condition, String message) {
        if (!condition) throw new AssertionError("FAIL: " + message);
    }
}
