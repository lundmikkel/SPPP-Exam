package snippets;

import java.util.Random;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by lundmikkel on 03/01/15.
 */
public class StartJoin {
    public static void main(String[] args) {

    }

    public void JoinedThreadsWithSeparateRanges(final int threadCount, final int perThread, final int range) {
        Thread[] threads = new Thread[threadCount];

        for (int t = 0; t < threadCount; ++t) {
            final int from = perThread * t;
            final int to = (t + 1 == threadCount) ? range : perThread * (t + 1);

            // Create thread
            threads[t] = new Thread(() -> {
                for (int p = from; p < to; ++p) {
                    // Insert code here
                }
            });
        }

        // Start threads simultaneously
        for (int t = 0; t < threadCount; t++)
            threads[t].start();

        // Wait for all threads to finish
        try {
            for (int t = 0; t < threadCount; ++t)
                threads[t].join();
        } catch (InterruptedException exn) {
        }
    }

    public void BarrierThreadsWithSeparateRanges(final int threadCount, final int perThread, final int range) {
        Thread[] threads = new Thread[threadCount];
        final CyclicBarrier startBarrier = new CyclicBarrier(threadCount + 1), stopBarrier = startBarrier;

        for (int t = 0; t < threadCount; ++t) {
            final int from = perThread * t;
            final int to = (t + 1 == threadCount) ? range : perThread * (t + 1);

            // Create thread
            threads[t] = new Thread(() -> {
                // Wait until all threads are ready
                try {
                    startBarrier.await();
                } catch (Exception exn) {
                }

                for (int p = from; p < to; ++p) {
                    // Insert code here
                }

                // Wait until all threads are done
                try {
                    stopBarrier.await();
                } catch (Exception exn) {
                }
            });
        }

        // Start threads simultaneously
        try {
            startBarrier.await();
        } catch (Exception exn) {
        }

        // Wait for all threads to finish
        try {
            stopBarrier.await();
        } catch (Exception exn) {
        }
    }
}
