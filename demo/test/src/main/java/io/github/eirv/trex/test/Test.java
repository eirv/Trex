package io.github.eirv.trex.test;

@SuppressWarnings("all")
public class Test {
    private static int i;
    private static int j;

    public static Exception test() {
        i = j = 0;
        return testA();
    }

    private static Exception testA() {
        return testB();
    }

    private static Exception testB() {
        return testC();
    }

    private static Exception testC() {
        if (++i != 3) {
            return testA();
        } else {
            return testD();
        }
    }

    private static Exception testD() {
        return testE();
    }

    private static Exception testE() {
        return testF();
    }

    private static Exception testF() {
        return testG();
    }

    private static Exception testG() {
        if (++j != 5) {
            return testF();
        } else {
            return testH();
        }
    }

    private static Exception testH() {
        return testI(false, (byte) 0, (short) 0, (char) 0, 0, 0, 0, 0, null);
    }

    private static Exception testI(
            boolean z, byte b, short s, char c, int i, float f, long j, double d, Object l) {
        return testJ(null, null, null, null, null, null, null, null, null);
    }

    private static Exception testJ(
            boolean[] za,
            byte[][] baa,
            short[][][] saaa,
            char[] ca,
            int[] ia,
            float[] fa,
            long[] ja,
            double[] da,
            Object[] la) {
        try {
            ClassTest.test(
                    new Runnable() {
                        @Override
                        public void run() {
                            throw new RuntimeException("hello");
                        }
                    });
            throw new AssertionError();
        } catch (Exception e) {
            return e;
        }
    }
}
