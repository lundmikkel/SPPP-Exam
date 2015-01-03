package snippets;

// For week 7
// sestoft@itu.dk * 2014-10-09

// The underlying.forEach call in class WrapConcurrentHashMap works
// only in Java 8; comment out if you have Java 7.

import java.util.Random;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;

import benchmark.Benchmark;
import com.sun.tools.doclets.formats.html.SourceToHTMLConverter;

public class TestStripedMap {
    public static void main(String[] args) {
        Benchmark.SystemInfo();
        //testAllMaps();    // Must be run with: java -ea TestStripedMap
        //exerciseAllMaps();
        timeAllMaps();
    }

    private static void timeAllMaps() {
        final int bucketCount = 100_032, lockCount = 32;
        for (int t = 1; t <= 32; t++) {
            final int threadCount = t;
            Benchmark.PrettyPrint(String.format("%-25s %3d", "SynchronizedMap", threadCount),
                    Benchmark.Mark(i -> timeMap(threadCount, new SynchronizedMap<>(bucketCount))));
            Benchmark.PrettyPrint(String.format("%-25s %3d", "StripedMap", threadCount),
                    Benchmark.Mark(i -> timeMap(threadCount, new StripedMap<>(bucketCount, lockCount))));
            Benchmark.PrettyPrint(String.format("%-25s %3d", "StripedWriteMap", threadCount),
                    Benchmark.Mark(i -> timeMap(threadCount, new StripedWriteMap<>(bucketCount, lockCount))));
            Benchmark.PrettyPrint(String.format("%-25s %3d", "WrapConcHashMap", threadCount),
                    Benchmark.Mark(i -> timeMap(threadCount, new WrapConcurrentHashMap<>())));
        }
    }

    private static double timeMap(int threadCount, final OurMap<Integer, String> map) {
        final int iterations = 5_000_000, perThread = iterations / threadCount;
        final int range = 200_000;
        return exerciseMap(threadCount, perThread, range, map);
    }

    private static double exerciseMap(int threadCount, final int perThread, final int range, final OurMap<Integer, String> map) {
        Thread[] threads = new Thread[threadCount];
        for (int t = 0; t < threadCount; t++) {
            final int myThread = t;
            threads[t] = new Thread(() -> {
                Random random = new Random(37 * myThread + 78);
                for (int i = 0; i < perThread; i++) {
                    Integer key = random.nextInt(range);
                    if (!map.containsKey(key)) {
                        // Add key with probability 60%
                        if (random.nextDouble() < 0.60)
                            map.put(key, Integer.toString(key));
                    } else // Remove key with probability 2% and reinsert
                        if (random.nextDouble() < 0.02) {
                            map.remove(key);
                            map.putIfAbsent(key, Integer.toString(key));
                        }
                }
                final AtomicInteger ai = new AtomicInteger();
                map.forEach((k, v) -> ai.getAndIncrement());
                // System.out.println(ai.intValue() + " " + map.size());
            });
        }
        for (int t = 0; t < threadCount; t++)
            threads[t].start();
        map.reallocateBuckets();
        try {
            for (int t = 0; t < threadCount; t++)
                threads[t].join();
        } catch (InterruptedException ignored) {
        }
        return map.size();
    }

    private static void exerciseAllMaps() {
        final int bucketCount = 100_000,
                lockCount = 32,
                threadCount = 16,
                iterations = 1_600_000,
                perThread = iterations / threadCount,
                range = 100_000;

        Benchmark.PrettyPrint(String.format("%-21s %d", "SynchronizedMap", threadCount),
                Benchmark.Mark(i -> exerciseMap(threadCount, perThread, range, new SynchronizedMap<>(bucketCount))));
        Benchmark.PrettyPrint(String.format("%-21s %d", "StripedMap", threadCount),
                Benchmark.Mark(i -> exerciseMap(threadCount, perThread, range, new StripedMap<>(bucketCount, lockCount))));
        Benchmark.PrettyPrint(String.format("%-21s %d", "StripedWriteMap", threadCount),
                Benchmark.Mark(i -> exerciseMap(threadCount, perThread, range, new StripedWriteMap<>(bucketCount, lockCount))));
        Benchmark.PrettyPrint(String.format("%-21s %d", "WrapConcHashMap", threadCount),
                Benchmark.Mark(i -> exerciseMap(threadCount, perThread, range, new WrapConcurrentHashMap<>())));
    }

    // Very basic sequential functional test of a hash map.  You must
    // run with assertions enabled for this to work, as in
    //   java -ea TestStripedMap
    private static void testMap(final OurMap<Integer, String> map) {
        System.out.printf("%n%s%n", map.getClass());
        assert map.size() == 0;
        assert !map.containsKey(117);
        assert map.get(117) == null;
        assert map.put(117, "A") == null;
        assert map.containsKey(117);
        assert map.get(117).equals("A");
        assert map.put(17, "B") == null;
        assert map.size() == 2;
        assert map.containsKey(17);
        assert map.get(117).equals("A");
        assert map.get(17).equals("B");
        assert map.put(117, "C").equals("A");
        assert map.containsKey(117);
        assert map.get(117).equals("C");
        assert map.size() == 2;
        map.forEach((k, v) -> System.out.printf("%10d maps to %s%n", k, v));
        assert map.remove(117).equals("C");
        assert !map.containsKey(117);
        assert map.get(117) == null;
        assert map.size() == 1;
        assert map.putIfAbsent(17, "D").equals("B");
        assert map.get(17).equals("B");
        assert map.size() == 1;
        assert map.containsKey(17);
        assert map.putIfAbsent(217, "E") == null;
        assert map.get(217).equals("E");
        assert map.size() == 2;
        assert map.containsKey(217);
        assert map.putIfAbsent(34, "F") == null;
        map.forEach((k, v) -> System.out.printf("%10d maps to %s%n", k, v));
        map.reallocateBuckets();
        assert map.size() == 3;
        assert map.get(17).equals("B") && map.containsKey(17);
        assert map.get(217).equals("E") && map.containsKey(217);
        assert map.get(34).equals("F") && map.containsKey(34);
        map.forEach((k, v) -> System.out.printf("%10d maps to %s%n", k, v));
        map.reallocateBuckets();
        assert map.size() == 3;
        assert map.get(17).equals("B") && map.containsKey(17);
        assert map.get(217).equals("E") && map.containsKey(217);
        assert map.get(34).equals("F") && map.containsKey(34);
        map.forEach((k, v) -> System.out.printf("%10d maps to %s%n", k, v));
    }

    private static void testAllMaps() {
        testMap(new SynchronizedMap<>(25));
        testMap(new StripedMap<>(25, 5));
        testMap(new StripedWriteMap<>(25, 5));
        testMap(new WrapConcurrentHashMap<>());
    }
}

interface Consumer<K, V> {
    void accept(K k, V v);
}

interface OurMap<K, V> {
    boolean containsKey(K k);

    V get(K k);

    V put(K k, V v);

    V putIfAbsent(K k, V v);

    V remove(K k);

    int size();

    void forEach(Consumer<K, V> consumer);

    void reallocateBuckets();
}

// ----------------------------------------------------------------------
// A hashmap that permits thread-safe concurrent operations, similar
// to a synchronized version of HashMap<K,V>.

class SynchronizedMap<K, V> implements OurMap<K, V> {
    // Synchronization policy:
    //   buckets[hash] and cachedSize are guarded by this
    private ItemNode<K, V>[] buckets;
    private int cachedSize;

    public SynchronizedMap(int bucketCount) {
        this.buckets = makeBuckets(bucketCount);
    }

    @SuppressWarnings("unchecked")
    private static <K, V> ItemNode<K, V>[] makeBuckets(int size) {
        // Java's @$#@?!! type system requires this unsafe cast
        return (ItemNode<K, V>[]) new ItemNode[size];
    }

    // Return true if key k is in map, else false
    public synchronized boolean containsKey(K k) {
        final int h = k.hashCode(), hash = h % buckets.length;
        return ItemNode.search(buckets[hash], k) != null;
    }

    // Return value v associated with key k, or null
    public synchronized V get(K k) {
        final int h = k.hashCode(), hash = h % buckets.length;
        ItemNode<K, V> node = ItemNode.search(buckets[hash], k);
        if (node != null)
            return node.v;
        else
            return null;
    }

    public synchronized int size() {
        return cachedSize;
    }

    // Put v at key k, or update if already present
    public synchronized V put(K k, V v) {
        final int h = k.hashCode(), hash = h % buckets.length;
        ItemNode<K, V> node = ItemNode.search(buckets[hash], k);
        if (node != null) {
            V old = node.v;
            node.v = v;
            return old;
        } else {
            buckets[hash] = new ItemNode<>(k, v, buckets[hash]);
            cachedSize++;
            return null;
        }
    }

    // Put v at key k only if absent
    public synchronized V putIfAbsent(K k, V v) {
        final int h = k.hashCode(), hash = h % buckets.length;
        ItemNode<K, V> node = ItemNode.search(buckets[hash], k);
        if (node != null)
            return node.v;
        else {
            buckets[hash] = new ItemNode<>(k, v, buckets[hash]);
            cachedSize++;
            return null;
        }
    }

    // Remove and return the value at key k if any, else return null
    public synchronized V remove(K k) {
        final int h = k.hashCode(), hash = h % buckets.length;
        ItemNode<K, V> prev = buckets[hash];
        if (prev == null)
            return null;
        else if (k.equals(prev.k)) {        // Delete first ItemNode
            V old = prev.v;
            cachedSize--;
            buckets[hash] = prev.next;
            return old;
        } else {                            // Search later ItemNodes
            while (prev.next != null && !k.equals(prev.next.k))
                prev = prev.next;
            // Now prev.next == null || k.equals(prev.next.k)
            if (prev.next != null) {  // Delete ItemNode prev.next
                V old = prev.next.v;
                cachedSize--;
                prev.next = prev.next.next;
                return old;
            } else
                return null;
        }
    }

    // Iterate over the hashmap's entries one bucket at a time
    public synchronized void forEach(Consumer<K, V> consumer) {
        for (int hash = 0; hash < buckets.length; hash++) {
            ItemNode<K, V> node = buckets[hash];
            while (node != null) {
                consumer.accept(node.k, node.v);
                node = node.next;
            }
        }
    }

    // Double bucket table size, rehash, and redistribute entries.

    public synchronized void reallocateBuckets() {
        final ItemNode<K, V>[] newBuckets = makeBuckets(2 * buckets.length);
        for (int hash = 0; hash < buckets.length; hash++) {
            ItemNode<K, V> node = buckets[hash];
            while (node != null) {
                final int newHash = node.k.hashCode() % newBuckets.length;
                ItemNode<K, V> next = node.next;
                node.next = newBuckets[newHash];
                newBuckets[newHash] = node;
                node = next;
            }
        }
        buckets = newBuckets;
    }

    static class ItemNode<K, V> {
        private final K k;
        private V v;
        private ItemNode<K, V> next;

        public ItemNode(K k, V v, ItemNode<K, V> next) {
            this.k = k;
            this.v = v;
            this.next = next;
        }

        public static <K, V> ItemNode<K, V> search(ItemNode<K, V> node, K k) {
            while (node != null && !k.equals(node.k))
                node = node.next;
            return node;
        }
    }
}

// ----------------------------------------------------------------------
// A hash map that permits thread-safe concurrent operations, using
// lock striping (intrinsic locks on Objects created for the purpose).

// NOT IMPLEMENTED: get, putIfAbsent, size, remove and forEach.

// bucketCount % lockCount == 0

// The bucketCount must be a multiple of the number lockCount of
// stripes, so that h % lockCount == (h % bucketCount) % lockCount and
// so that h % lockCount is invariant under doubling the number of
// buckets in method reallocateBuckets.  Otherwise there is a risk of
// locking a stripe, only to have the relevant entry moved to a
// different stripe by an intervening call to reallocateBuckets.

class StripedMap<K, V> implements OurMap<K, V> {
    // Synchronization policy:
    //   buckets[hash] is guarded by locks[hash%lockCount]
    //   sizes[stripe] is guarded by locks[stripe]
    private volatile ItemNode<K, V>[] buckets;
    private final int lockCount;
    private final Object[] locks;
    private final int[] sizes;

    public StripedMap(int bucketCount, int lockCount) {
        if (bucketCount % lockCount != 0)
            throw new RuntimeException("bucket count must be a multiple of stripe count");
        this.lockCount = lockCount;
        this.buckets = makeBuckets(bucketCount);
        this.locks = new Object[lockCount];
        this.sizes = new int[lockCount];
        for (int stripe = 0; stripe < lockCount; ++stripe)
            this.locks[stripe] = new Object();
    }

    @SuppressWarnings("unchecked")
    private static <K, V> ItemNode<K, V>[] makeBuckets(int size) {
        // Java's @$#@?!! type system requires this unsafe cast
        return (ItemNode<K, V>[]) new ItemNode[size];
    }

    // Return true if key k is in map, else false
    public boolean containsKey(K k) {
        final int h = k.hashCode(),
                stripe = h % lockCount,
                hash = h % buckets.length;
        synchronized (locks[stripe]) {
            return ItemNode.search(buckets[hash], k) != null;
        }
    }

    // Return value v associated with key k, or null
    public V get(K k) {
        final int h = k.hashCode(),
                stripe = h % lockCount,
                hash = h % buckets.length;
        synchronized (locks[stripe]) {
            ItemNode<K, V> node = ItemNode.search(buckets[hash], k);
            return node != null ? node.v : null;
        }
    }

    public int size() {
        int sum = 0;
        for (int stripe = 0; stripe < lockCount; ++stripe) {
            synchronized (locks[stripe]) {
                sum += sizes[stripe];
            }
        }
        return sum;
    }

    // Put v at key k, or update if already present
    public V put(K k, V v) {
        final int h = k.hashCode(),
                stripe = h % lockCount,
                hash = h % buckets.length;
        synchronized (locks[stripe]) {
            final ItemNode<K, V> node = ItemNode.search(buckets[hash], k);
            if (node != null) {
                V old = node.v;
                node.v = v;
                return old;
            } else {
                buckets[hash] = new ItemNode<>(k, v, buckets[hash]);
                sizes[stripe]++;
                return null;
            }
        }
    }

    // Put v at key k only if absent
    public V putIfAbsent(K k, V v) {
        final int h = k.hashCode(),
                stripe = h % lockCount,
                hash = h % buckets.length;
        synchronized (locks[stripe]) {
            final ItemNode<K, V> node = ItemNode.search(buckets[hash], k);
            if (node != null) {
                return node.v;
            } else {
                buckets[hash] = new ItemNode<>(k, v, buckets[hash]);
                sizes[stripe]++;
                return null;
            }
        }
    }

    // Remove and return the value at key k if any, else return null
    public V remove(K k) {
        final int h = k.hashCode(),
                stripe = h % lockCount,
                hash = h % buckets.length;
        synchronized (locks[stripe]) {
            ItemNode<K, V> prev = buckets[hash];
            if (prev == null)
                return null;
                // Delete first ItemNode
            else if (k.equals(prev.k)) {
                V old = prev.v;
                buckets[hash] = prev.next;
                sizes[stripe]--;
                return old;
            }
            // Search later ItemNodes
            else {
                while (prev.next != null && !k.equals(prev.next.k))
                    prev = prev.next;

                // Now prev.next == null || k.equals(prev.next.k)
                // Delete ItemNode prev.next
                if (prev.next != null) {
                    V old = prev.next.v;
                    prev.next = prev.next.next;
                    sizes[stripe]--;
                    return old;
                } else
                    return null;
            }
        }
    }

    // Iterate over the hashmap's entries one stripe at a time; less locking
    public void forEach(Consumer<K, V> consumer) {
        final ItemNode<K, V>[] bs = buckets;
        for (int stripe = 0; stripe < lockCount; ++stripe) {
            //TODO: It is not necessary to lock the stripe as the bucket reference
            //      is final and thus cannot change during iteration. It is only
            //      a read operation and thus no locking is needed.
            synchronized (locks[stripe]) {
                for (int hash = stripe; hash < bs.length; hash += lockCount) {
                    ItemNode<K, V> node = bs[hash];
                    while (node != null) {
                        consumer.accept(node.k, node.v);
                        node = node.next;
                    }
                }
            }
        }
    }

    // First lock all stripes.  Then double bucket table size, rehash,
    // and redistribute entries.  Since the number of stripes does not
    // change, and since N = buckets.length is a multiple of lockCount,
    // a key that belongs to stripe s because (k.hashCode() % N) %
    // lockCount == s will continue to belong to stripe s.  Hence the
    // sizes array need not be recomputed.

    public void reallocateBuckets() {
        lockAllAndThen(() -> {
            final ItemNode<K, V>[] newBuckets = makeBuckets(2 * buckets.length);
            for (int hash = 0; hash < buckets.length; hash++) {
                ItemNode<K, V> node = buckets[hash];
                while (node != null) {
                    final int newHash = node.k.hashCode() % newBuckets.length;
                    ItemNode<K, V> next = node.next;
                    node.next = newBuckets[newHash];
                    newBuckets[newHash] = node;
                    node = next;
                }
            }
            buckets = newBuckets;
        });
    }

    // Lock all stripes, perform the action, then unlock all stripes
    private void lockAllAndThen(Runnable action) {
        lockAllAndThen(0, action);
    }

    private void lockAllAndThen(int nextStripe, Runnable action) {
        if (nextStripe >= lockCount)
            action.run();
        else
            synchronized (locks[nextStripe]) {
                lockAllAndThen(nextStripe + 1, action);
            }
    }

    static class ItemNode<K, V> {
        private final K k;
        private V v;
        private ItemNode<K, V> next;

        public ItemNode(K k, V v, ItemNode<K, V> next) {
            this.k = k;
            this.v = v;
            this.next = next;
        }

        // Assumes locks[k.hashcode() % lockCount] is held by the thread
        public static <K, V> ItemNode<K, V> search(ItemNode<K, V> node, K k) {
            while (node != null && !k.equals(node.k))
                node = node.next;
            return node;
        }

        public String toString() {
            return k.toString() + " - " + v.toString();
        }
    }
}

// ----------------------------------------------------------------------
// A hashmap that permits thread-safe concurrent operations, using
// lock striping (intrinsic locks on Objects created for the purpose),
// and with immutable ItemNodes, so that reads do not need to lock at
// all, only need visibility of writes, which is ensured through the
// AtomicIntegerArray called sizes.

// NOT IMPLEMENTED: _get, putIfAbsent, _size, remove and forEach.

// The bucketCount must be a multiple of the number lockCount of
// stripes, so that h % lockCount == (h % bucketCount) % lockCount and
// so that h % lockCount is invariant under doubling the number of
// buckets in method reallocateBuckets.  Otherwise there is a risk of
// locking a stripe, only to have the relevant entry moved to a
// different stripe by an intervening call to reallocateBuckets.

class StripedWriteMap<K, V> implements OurMap<K, V> {
    // Synchronization policy: writing to
    //   buckets[hash] is guarded by locks[hash % lockCount]
    //   sizes[stripe] is guarded by locks[stripe]
    // Visibility of writes to reads is ensured by writes writing to
    // the stripe's size component (even if size does not change) and
    // reads reading from the stripe's size component.
    private volatile ItemNode<K, V>[] buckets;
    private final int lockCount;
    private final Object[] locks;
    private final AtomicIntegerArray sizes;

    public StripedWriteMap(int bucketCount, int lockCount) {
        if (bucketCount % lockCount != 0)
            throw new RuntimeException("bucket count must be a multiple of stripe count");
        this.lockCount = lockCount;
        this.buckets = makeBuckets(bucketCount);
        this.locks = new Object[lockCount];
        this.sizes = new AtomicIntegerArray(lockCount);
        for (int stripe = 0; stripe < lockCount; stripe++)
            this.locks[stripe] = new Object();
    }

    @SuppressWarnings("unchecked")
    private static <K, V> ItemNode<K, V>[] makeBuckets(int size) {
        // Java's @$#@?!! type system requires "unsafe" cast here:
        return (ItemNode<K, V>[]) new ItemNode[size];
    }

    // Return true if key k is in map, else false
    public boolean containsKey(K k) {
        final ItemNode<K, V>[] bs = buckets;
        final int h = k.hashCode(),
                stripe = h % lockCount,
                hash = h % bs.length;
        // The sizes access is necessary for visibility of bs elements
        return sizes.get(stripe) != 0 && ItemNode.search(bs[hash], k, null);
    }

    // Return value v associated with key k, or null
    public V get(K k) {
        final ItemNode<K, V>[] bs = buckets;
        final int h = k.hashCode(),
                stripe = h % lockCount,
                hash = h % bs.length;
        final Holder<V> holder = new Holder<>();
        return sizes.get(stripe) != 0 && ItemNode.search(bs[hash], k, holder) ? holder.get() : null;
    }

    public int size() {
        int sum = 0;
        for (int i = 0; i < lockCount; ++i) {
            sum += sizes.get(i);
        }
        return sum;
    }

    // Put v at key k, or update if already present
    public V put(K k, V v) {
        final int h = k.hashCode(),
                stripe = h % lockCount;
        final Holder<V> old = new Holder<>();

        synchronized (locks[stripe]) {
            // By locking a stripe we know that the buckets array won't change
            // as the array resizing needs the lock to proceed
            final int hash = h % buckets.length;
            final ItemNode<K, V> node = buckets[hash],
                    newNode = ItemNode.delete(node, k, old);
            buckets[hash] = new ItemNode<>(k, v, newNode);
            // Write for visibility; increment if k was not already in map
            sizes.getAndAdd(stripe, newNode == node ? 1 : 0);
            return old.get();
        }
    }

    // Put v at key k only if absent
    public V putIfAbsent(K k, V v) {
        final int h = k.hashCode(),
                stripe = h % lockCount;
        final Holder<V> old = new Holder<>();

        synchronized (locks[stripe]) {
            // By locking a stripe we know that the buckets array won't change
            // as the array resizing needs the lock to proceed
            final int hash = h % buckets.length;
            if (!ItemNode.search(buckets[hash], k, old)) {
                buckets[hash] = new ItemNode<>(k, v, buckets[hash]);
                // Write for visibility; increment as k was not already in map
                sizes.incrementAndGet(stripe);
            }
            return old.get();
        }
    }

    // Remove and return the value at key k if any, else return null
    public V remove(K k) {
        final int h = k.hashCode(),
                stripe = h % lockCount;
        final Holder<V> old = new Holder<>();

        synchronized (locks[stripe]) {
            // By locking a stripe we know that the buckets array won't change
            // as the array resizing needs the lock to proceed
            final int hash = h % buckets.length;
            final ItemNode<K, V> node = buckets[hash];
            buckets[hash] = ItemNode.delete(node, k, old);
            // True if the value was deleted
            if (node != buckets[hash])
                // Write for visibility; decrement as k was deleted from the map
                sizes.decrementAndGet(stripe);
            return old.get();
        }
    }

    // Iterate over the hashmap's entries one stripe at a time.
    public void forEach(Consumer<K, V> consumer) {
        final ItemNode<K, V>[] bs = buckets;
        for (int stripe = 0; stripe < lockCount; ++stripe) {
            if (sizes.get(stripe) != 0) {
                for (int hash = stripe; hash < bs.length; hash += lockCount) {
                    ItemNode<K, V> node = bs[hash];
                    while (node != null) {
                        consumer.accept(node.k, node.v);
                        node = node.next;
                    }
                }
            }
        }
    }

    // First lock all stripes.  Then double bucket table size, rehash,
    // and redistribute entries.  Since the number of stripes does not
    // change, and since buckets.length is a multiple of lockCount, a
    // key that belongs to stripe s because (k.hashCode() % N) %
    // lockCount == s will continue to belong to stripe s.  Hence the
    // sizes array need not be recomputed.

    public void reallocateBuckets() {
        lockAllAndThen(() -> {
            final ItemNode<K, V>[] bs = buckets;
            final ItemNode<K, V>[] newBuckets = makeBuckets(2 * bs.length);
            for (int hash = 0; hash < bs.length; hash++) {
                ItemNode<K, V> node = bs[hash];
                while (node != null) {
                    final int newHash = node.k.hashCode() % newBuckets.length;
                    newBuckets[newHash] = new ItemNode<>(node.k, node.v, newBuckets[newHash]);
                    node = node.next;
                }
            }
            buckets = newBuckets; // Visibility: buckets is volatile
        });
    }

    // Lock all stripes, perform action, then unlock all stripes
    private void lockAllAndThen(Runnable action) {
        lockAllAndThen(0, action);
    }

    private void lockAllAndThen(int nextStripe, Runnable action) {
        if (nextStripe >= lockCount)
            action.run();
        else
            synchronized (locks[nextStripe]) {
                lockAllAndThen(nextStripe + 1, action);
            }
    }

    static class ItemNode<K, V> {
        private final K k;
        private final V v;
        private final ItemNode<K, V> next;

        public ItemNode(K k, V v, ItemNode<K, V> next) {
            this.k = k;
            this.v = v;
            this.next = next;
        }

        // These work on immutable data only, no synchronization needed.

        public static <K, V> boolean search(ItemNode<K, V> node, K k, Holder<V> old) {
            while (node != null)
                if (k.equals(node.k)) {
                    if (old != null)
                        old.set(node.v);
                    return true;
                } else
                    node = node.next;
            return false;
        }

        public static <K, V> ItemNode<K, V> delete(ItemNode<K, V> node, K k, Holder<V> old) {
            if (node == null)
                return null;
            else if (k.equals(node.k)) {
                old.set(node.v);
                return node.next;
            } else {
                final ItemNode<K, V> newNode = delete(node.next, k, old);
                if (newNode == node.next)
                    return node;
                else
                    return new ItemNode<>(node.k, node.v, newNode);
            }
        }
    }

    // Object to hold a "by reference" parameter.  For use only on a
    // single thread, so no need for "volatile" or synchronization.

    static class Holder<V> {
        private V value;

        public V get() {
            return value;
        }

        public void set(V value) {
            this.value = value;
        }
    }
}

// ----------------------------------------------------------------------
// A wrapper around the Java class library's sophisticated
// ConcurrentHashMap<K,V>, making it implement OurMap<K,V>

class WrapConcurrentHashMap<K, V> implements OurMap<K, V> {
    final ConcurrentHashMap<K, V> underlying = new ConcurrentHashMap<>();

    public boolean containsKey(K k) {
        return underlying.containsKey(k);
    }

    public V get(K k) {
        return underlying.get(k);
    }

    public V put(K k, V v) {
        return underlying.put(k, v);
    }

    public V putIfAbsent(K k, V v) {
        return underlying.putIfAbsent(k, v);
    }

    public V remove(K k) {
        return underlying.remove(k);
    }

    public int size() {
        return underlying.size();
    }

    public void forEach(Consumer<K, V> consumer) {
        underlying.forEach(consumer::accept);
    }

    public void reallocateBuckets() {
    }
}
