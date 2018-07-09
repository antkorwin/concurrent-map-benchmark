package com.antkorwin.concurrenttests;

import com.antkorwin.xsync.springframework.util.ConcurrentReferenceHashMap;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import java.util.stream.IntStream;

/**
 * Created on 07.07.2018.
 *
 * There are benchmarks for testing a performance of the ConcurrentReferenceHashMap
 *
 * @author Korovin Anatoliy
 */
public class ConcurrentMapPerformanceTest {

    private static final int NUMBER_OF_KEYS = 10_000;


    @Benchmark
    public void synchronizedMap_put_get(Data data) throws Exception {
        avoidEliminatingAndAssert(data, data.synchronizedMap, this::putAndGet);
    }

    @Benchmark
    public void concurrentHashMap_put_get(Data data) throws Exception {
        avoidEliminatingAndAssert(data, data.concurrentHashMap, this::putAndGet);
    }

    @Benchmark
    public void concurrentReferenceHashMap_put_get(Data data) throws Exception {
        avoidEliminatingAndAssert(data, data.concurrentReferenceHashMap, this::compute);
    }

    @Benchmark
    public void synchronizedMap_compute(Data data) throws Exception {
        avoidEliminatingAndAssert(data, data.synchronizedMap, this::compute);
    }

    @Benchmark
    public void concurrentHashMap_compute(Data data) throws Exception {
        avoidEliminatingAndAssert(data, data.concurrentHashMap, this::compute);
    }

    @Benchmark
    public void concurrentReferenceHashMap_compute(Data data) throws Exception {
        avoidEliminatingAndAssert(data, data.concurrentReferenceHashMap, this::compute);
    }

    @Benchmark
    public void synchronizedMap_put(Data data) throws Exception {
        avoidEliminatingAndAssert(data, data.synchronizedMap, this::put);
    }

    @Benchmark
    public void concurrentHashMap_put(Data data) throws Exception {
        avoidEliminatingAndAssert(data, data.concurrentHashMap, this::put);
    }

    @Benchmark
    public void concurrentReferenceHashMap_put(Data data) throws Exception {
        avoidEliminatingAndAssert(data, data.concurrentReferenceHashMap, this::put);
    }


    private void avoidEliminatingAndAssert(Data data,
                                           Map<UUID, Integer> map,
                                           BiConsumer<Data, Map<UUID, Integer>> mapProcessor) {

        long before = data.sum.get();

        // Act
        mapProcessor.accept(data, map);

        // avoid an eliminating
        int rndIndex = data.random.nextInt(NUMBER_OF_KEYS);
        data.sum.addAndGet(map.get(data.keys[rndIndex]) % 2 + 1);

        // Assert
        Assertions.assertThat(data.sum.get()).isGreaterThan(before);
    }

    private void putAndGet(Data data, Map<UUID, Integer> map) {

        IntStream.range(0, NUMBER_OF_KEYS)
                 .boxed()
                 .forEach(i -> map.putIfAbsent(data.keys[i], data.random.nextInt(NUMBER_OF_KEYS)));

        IntStream.range(0, NUMBER_OF_KEYS)
                 .boxed()
                 .forEach(i -> map.put(data.keys[i], map.get(data.keys[NUMBER_OF_KEYS - 1 - i])));
    }

    private void compute(Data data, Map<UUID, Integer> map) {

        IntStream.range(0, NUMBER_OF_KEYS)
                 .boxed()
                 .forEach(i -> map.compute(data.keys[i],
                                           (k, v) -> (v == null) ? data.random.nextInt(NUMBER_OF_KEYS) : v));
    }

    private void put(Data data, Map<UUID, Integer> map) {
        IntStream.range(0, NUMBER_OF_KEYS)
                 .boxed()
                 .forEach(i -> map.putIfAbsent(data.keys[i], data.random.nextInt(NUMBER_OF_KEYS)));
    }


    @Test
    public void launchBenchmark() throws Exception {

        Options opt = new OptionsBuilder()
                // Specify which benchmarks to run.
                // You can be more specific if you'd like to run only one benchmark per test.
                .include(this.getClass().getName() + ".*")
                // Set the following options as needed
                .mode(Mode.All)
                .timeUnit(TimeUnit.MILLISECONDS)
                .warmupTime(TimeValue.seconds(2))
                .warmupIterations(1)
                .measurementTime(TimeValue.seconds(5))
                .measurementIterations(5)
                .threads(4)
                .forks(1)
                .shouldFailOnError(true)
                .shouldDoGC(true)
                .jvmArgs("-Xms7024m", "-Xmx7024m", "-verbose:gc")
                //.jvmArgs("-XX:+UnlockDiagnosticVMOptions", "-XX:+PrintInlining")
                //.addProfiler(WinPerfAsmProfiler.class)
                .build();

        new Runner(opt).run();
    }


    @State(Scope.Benchmark)
    public static class Data {

        UUID[] keys = new UUID[NUMBER_OF_KEYS];
        Random random = new Random();
        AtomicLong sum = new AtomicLong(0);

        Map<UUID, Integer> synchronizedMap = Collections.synchronizedMap(new WeakHashMap<>());
        Map<UUID, Integer> concurrentHashMap = new ConcurrentHashMap<>();
        Map<UUID, Integer> concurrentReferenceHashMap = new ConcurrentReferenceHashMap<>();

        @Setup
        public void setUp() {
            System.out.println("\nsetup start");
            for (int i = 0; i < NUMBER_OF_KEYS; i += 2) {
                keys[i] = UUID.randomUUID();
                keys[i + 1] = keys[i];
            }
            System.out.println("setup done");
        }

        @TearDown
        public void tearDown() {
            System.out.println("teardown: " + sum.get());
        }
    }
}
