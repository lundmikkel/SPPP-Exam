package benchmark;

// Simple microbenchmark setups
// sestoft@itu.dk * 2013-06-02, 2014-09-10
// mirl@itu.dk * 2015-01-02

import java.util.function.IntToDoubleFunction;

class Benchmark {
    public static void main(String[] args) {
        SystemInfo.SystemInfo();

        Mark("pow",  i -> Math.pow(10.0, 0.1 * (i & 0xFF)));
        Mark("exp",  i -> Math.exp(0.1 * (i & 0xFF)));
        Mark("log",  i -> Math.log(0.1 + 0.1 * (i & 0xFF)));
        Mark("sin",  i -> Math.sin(0.1 * (i & 0xFF)));
        Mark("cos",  i -> Math.cos(0.1 * (i & 0xFF)));
        Mark("tan",  i -> Math.tan(0.1 * (i & 0xFF)));
        Mark("asin", i -> Math.asin(1.0/256.0 * (i & 0xFF)));
        Mark("acos", i -> Math.acos(1.0/256.0 * (i & 0xFF)));
        Mark("atan", i -> Math.atan(1.0/256.0 * (i & 0xFF)));
    }

    public static double Mark(String msg, IntToDoubleFunction f) {
        return Mark(msg, "", f, 10, 0.25);
    }

    public static double Mark(String msg, String info, IntToDoubleFunction f) {
        return Mark(msg, info, f, 10, 0.25);
    }

    public static double Mark(String msg, String info, IntToDoubleFunction f, int n, double minTime) {
        int count = 1, totalCount = 0;
        double dummy = 0.0, runningTime = 0.0, st, sst;
        do {
            count *= 2;
            st = sst = 0.0;
            for (int j = 0; j < n; ++j) {
                Timer t = new Timer();
                for (int i = 0; i < count; ++i)
                    dummy += f.applyAsDouble(i);
                runningTime = t.check();
                double time = runningTime * 1e9 / count;
                st += time;
                sst += time * time;
                totalCount += count;
            }
        } while (runningTime < minTime && count < Integer.MAX_VALUE / 2);
        double mean = st / n, sdev = Math.sqrt(sst / n - mean * mean);
        System.out.printf("%-25s %s%15.1f ns %10.2f %10d%n", msg, info, mean, sdev, count);
        return dummy / totalCount;
    }

    public static double Mark(String msg, Benchmarkable f) {
        return Mark(msg, "", f, 10, 0.25);
    }

    public static double Mark(String msg, String info, Benchmarkable f) {
        return Mark(msg, info, f, 10, 0.25);
    }

    public static double Mark(String msg, String info, Benchmarkable f, int n, double minTime) {
        int count = 1, totalCount = 0;
        double dummy = 0.0, runningTime = 0.0, st, sst;
        do {
            count *= 2;
            st = sst = 0.0;
            for (int j = 0; j < n; ++j) {
                Timer t = new Timer();
                for (int i = 0; i < count; ++i) {
                    t.pause();
                    f.setup();
                    t.play();
                    dummy += f.call(i);
                }
                runningTime = t.check();
                double time = runningTime * 1e9 / count;
                st += time;
                sst += time * time;
                totalCount += count;
            }
        } while (runningTime < minTime && count < Integer.MAX_VALUE / 2);
        double mean = st / n, sdev = Math.sqrt(sst / n - mean * mean);
        System.out.printf("%-25s %s%15.1f ns %10.2f %10d%n", msg, info, mean, sdev, count);
        return dummy / totalCount;
    }
}
