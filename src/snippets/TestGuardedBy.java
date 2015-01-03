import javax.annotation.concurrent.GuardedBy;

class LongCounter {
    @GuardedBy("this")
    private long count = 0;

    public void increment() {
        count++;
    }

    public synchronized long get() {
        return count;
    }
}