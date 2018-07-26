package com.antkorwin.concurrenttests;

import com.antkorwin.xsync.springframework.util.ConcurrentReferenceHashMap;
import org.junit.Test;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.IntStream;

/**
 * Created on 09.07.2018.
 *
 * Micro-benchmark for concurrent map
 *
 * @author Korovin Anatoliy
 */
public class ConcurrentMapMicroBenchmark {

    private static final int NUMBER_OF_KEYS = 1_000;

    @Benchmark
    @OperationsPerInvocation(NUMBER_OF_KEYS)
    public void compute_ConcurrentHashMap(Data data, Blackhole bh) {
        for (int i = 0; i < NUMBER_OF_KEYS; i++) {
            bh.consume(data.concurrentHashMap.compute(data.keys[i], computeFunc(data)));
        }
    }

    @Benchmark
    @OperationsPerInvocation(NUMBER_OF_KEYS)
    public void compute_ConcurrentReferenceHashMap(Data data, Blackhole bh) {
        for (int i = 0; i < NUMBER_OF_KEYS; i++) {
            bh.consume(data.concurrentReferenceHashMap.compute(data.keys[i], computeFunc(data)));
        }
    }

    @Benchmark
    @OperationsPerInvocation(NUMBER_OF_KEYS)
    public void compute_SynchronizedMap(Data data, Blackhole bh) {
        for (int i = 0; i < NUMBER_OF_KEYS; i++) {
            bh.consume(data.synchronizedMap.compute(data.keys[i], computeFunc(data)));
        }
    }

    @Benchmark
    @OperationsPerInvocation(NUMBER_OF_KEYS)
    public void computeIfAbsent_ConcurrentHashMap(Data data, Blackhole bh) {
        for (int i = 0; i < NUMBER_OF_KEYS; i++) {
            bh.consume(data.concurrentHashMap.computeIfAbsent(data.keys[i], computeIfAbsentFunc(data)));
        }
    }


    private BiFunction<UUID, Integer, Integer> computeFunc(Data data) {
        return (k, v) -> (v == null) ? data.random.nextInt(NUMBER_OF_KEYS) : v;
    }

    private Function<? super UUID, ? extends Integer> computeIfAbsentFunc(Data data) {
        return k -> data.random.nextInt(NUMBER_OF_KEYS);
    }


    @Test
    public void launchBenchmark() throws Exception {

        Options opt = new OptionsBuilder()
                // Specify which benchmarks to run.
                // You can be more specific if you'd like to run only one benchmark per test.
                .include(this.getClass().getName() + ".*")
                // Set the following options as needed
                .mode(Mode.All)
                .timeUnit(TimeUnit.NANOSECONDS)
                .warmupTime(TimeValue.seconds(2))
                .warmupIterations(1)
                .measurementTime(TimeValue.seconds(5))
                .measurementIterations(5)
                .threads(8)
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

        Map<UUID, Integer> synchronizedMap = Collections.synchronizedMap(new WeakHashMap<>());
        Map<UUID, Integer> concurrentHashMap = new ConcurrentHashMap<>();
        Map<UUID, Integer> concurrentReferenceHashMap = new ConcurrentReferenceHashMap<>();

        @Setup
        public void setUp() {
            System.out.println("\nsetUp");
            for (int i = 0; i < NUMBER_OF_KEYS; i += 2) {
                keys[i] = UUID.randomUUID();
                keys[i + 1] = keys[i];
            }

            IntStream.range(0, NUMBER_OF_KEYS)
                     .boxed()
                     .forEach(i -> {
                         concurrentHashMap.putIfAbsent(keys[i], random.nextInt(NUMBER_OF_KEYS));
                         concurrentReferenceHashMap.putIfAbsent(keys[i], random.nextInt(NUMBER_OF_KEYS));
                         synchronizedMap.putIfAbsent(keys[i], random.nextInt(NUMBER_OF_KEYS));
                     });
        }
    }
}
