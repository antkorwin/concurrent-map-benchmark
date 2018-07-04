package com.antkorwin.concurrenttests;

import com.antkorwin.xsync.XMutex;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;
import org.springframework.util.ConcurrentReferenceHashMap;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

/**
 * Created by Korovin Anatolii on 04.07.2018.
 *
 * @author Korovin Anatolii
 * @version 1.0
 */
public class BenchmarkingTest {

    private static final int ITERATION_NUMBER = 100;
    private static final int NUMBER_OF_MUTEXES = 30;


    @Benchmark
    public void testSynchronizedMap(Data data) {

        // Arrange
        Map<UUID, XMutex<UUID>> map = Collections.synchronizedMap(new WeakHashMap<>());

        // Act & Assert
        testAndAssertMap(map, data);
    }


    @Benchmark
    public void testConcurrentHashMap(Data data) {
        // Arrange
        Map<UUID, XMutex<UUID>> map = new ConcurrentHashMap<>();

        // Act & Assert
        testAndAssertMap(map, data);
    }


    @Benchmark
    public void testConcurrentReferenceHashMap(Data data) {
        // Arrange
        Map<UUID, XMutex<UUID>> map = new ConcurrentReferenceHashMap<>();

        // Act & Assert
        testAndAssertMap(map, data);
    }

    @Test
    public void launchBenchmark() throws Exception {

        Options opt = new OptionsBuilder()
                // Specify which benchmarks to run.
                // You can be more specific if you'd like to run only one benchmark per test.
                .include(this.getClass().getName() + ".*")
                // Set the following options as needed
                .mode(Mode.All)
                .timeUnit(TimeUnit.MICROSECONDS)
                .warmupTime(TimeValue.seconds(3))
                .warmupIterations(3)
                .measurementTime(TimeValue.seconds(10))
                .measurementIterations(10)
                .threads(4)
                .forks(1)
                .shouldFailOnError(true)
                .shouldDoGC(true)
                //.jvmArgs("-XX:+UnlockDiagnosticVMOptions", "-XX:+PrintInlining")
                //.addProfiler(WinPerfAsmProfiler.class)
                .build();

        new Runner(opt).run();
    }

    private void testAndAssertMap(Map<UUID, XMutex<UUID>> map, Data data) {

        Set<XMutex<UUID>> mutexes = IntStream.range(0, ITERATION_NUMBER)
                                             .boxed()
                                             .parallel()
                                             .map(i -> {
                                                 return map.compute(data.ids.get(i % NUMBER_OF_MUTEXES), (k, v) -> {
                                                     if (v != null) {
                                                         return v;
                                                     } else {
                                                         //System.out.println("PUT");
                                                         return new XMutex<>(k);
                                                     }
                                                 });
                                             })
                                             .collect(toSet());

        Assertions.assertThat(mutexes).hasSize(NUMBER_OF_MUTEXES);
        Assertions.assertThat(map).hasSize(NUMBER_OF_MUTEXES);
    }

    @State(Scope.Benchmark)
    public static class Data {

        public List<UUID> ids;

        @Setup
        public void setUp() {
            System.out.println("setup");
            ids = IntStream.range(0, NUMBER_OF_MUTEXES)
                           .boxed()
                           .map(i -> UUID.randomUUID())
                           .collect(toList());
        }
    }

    @AllArgsConstructor
    @Getter
    class NonAtomicInt {
        private int value;

        public void increment() {
            this.value++;
        }
    }
}
