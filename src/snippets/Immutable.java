package snippets;

public class Immutable {
    public class ImmutableKeyValuePair {
        public final int key;
        public final int value;

        public ImmutableKeyValuePair(int key, int value) {
            this.key = key;
            this.value = value;
        }
    }

    class ImmutableOneValueCache {
        private final int key;
        private final int[] values;

        ImmutableOneValueCache(int key, int[] values) {
            this.key = key;
            this.values = values;
        }

        public int[] getValues(int key) {
            return (values == null || this.key != key) ? null : values.clone();
        }
    }
}