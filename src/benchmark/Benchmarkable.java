package benchmark;

public abstract class Benchmarkable {
    public void setup() {
    }

    public abstract double call(int i);
}