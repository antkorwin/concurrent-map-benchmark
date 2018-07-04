package com.antkorwin.concurrenttests;

import com.antkorwin.xsync.XMutex;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.springframework.util.ConcurrentReferenceHashMap;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.toList;

/**
 * Created by Korovin Anatolii on 03.07.2018.
 *
 * @author Korovin Anatolii
 * @version 1.0
 */
public class ConcurrentTest {

    public static final int ITERATION_NUMBER = 10000;

    @Test
    public void testConcurrencyThroughExecutorService() throws InterruptedException {
        // Arrange
        NonAtomicInt nonAtomicInt = new NonAtomicInt(0);
        ExecutorService executorService = Executors.newFixedThreadPool(10);

        // Act
        IntStream.range(0, ITERATION_NUMBER)
                 .boxed()
                 .forEach(i -> {
                     executorService.submit(nonAtomicInt::increment);
                 });

        executorService.awaitTermination(5, TimeUnit.SECONDS);

        // Assert
        Assertions.assertThat(nonAtomicInt.getValue())
                  .isNotEqualTo(ITERATION_NUMBER);
    }

    @Test
    public void testConcurrencyThroughParallelStream() {
        // Arrange
        NonAtomicInt nonAtomicInt = new NonAtomicInt(0);

        // Act
        IntStream.range(0, ITERATION_NUMBER)
                 .boxed()
                 .parallel()
                 .forEach(i -> nonAtomicInt.increment());

        // Assert
        Assertions.assertThat(nonAtomicInt.getValue())
                  .isNotEqualTo(ITERATION_NUMBER);
    }


    @Test
    public void testWithSync() {
        // Arrange
        NonAtomicInt nonAtomicInt = new NonAtomicInt(0);

        // Act
        IntStream.range(0, ITERATION_NUMBER)
                 .boxed()
                 .parallel()
                 .forEach(i -> {
                     synchronized ("123"){
                         nonAtomicInt.increment();
                     }
                 });

        // Assert
        Assertions.assertThat(nonAtomicInt.getValue())
                  .isEqualTo(ITERATION_NUMBER);
    }

    @Test(timeout = 10000)
    public void testWithDeadlock() {
        // Arrange
        // Act
        // Asserts
    }


    @Test
    public void testXXX() {
        // Arrange
        ConcurrentMap<UUID, XMutex<UUID>> cmap =
                new ConcurrentReferenceHashMap<>(16, ConcurrentReferenceHashMap.ReferenceType.WEAK);
        // WEAK не проходит этот тест если делать по 100 запусков теста.

        //new ConcurrentHashMap<>();

        // Act
        String strId = UUID.randomUUID().toString();
        UUID id1 = UUID.fromString(strId);
        UUID id2 = UUID.fromString(strId);
        UUID id3 = UUID.fromString(strId);

        Assertions.assertThat(id1 != id2).isTrue();
        Assertions.assertThat(id2 != id3).isTrue();

        XMutex<UUID> f1 = cmap.compute(id1, (k, v) -> {
            if (v != null) {
                return v;
            } else {
                System.out.println("F1");
                return new XMutex<>(k);
            }
        });

        List<XMutex<UUID>> results = new ArrayList<>();

        for (int i = 0; i < ITERATION_NUMBER; i++) {
            XMutex<UUID> fN = cmap.compute(id3, (k, v) -> {
                if (v != null) {
                    return v;
                } else {
                    System.out.println("F2");
                    return new XMutex<>(k);
                }
            });
            results.add(fN);

            //XMutex<UUID> mutex = cmap.get(id3);

//            Assertions.assertThat(fN == mutex).isTrue();
//            Assertions.assertThat(f1 == fN).isTrue();

//            if (f1 != fN) {
//                Assertions.assertThat(f1 == fN).isTrue();
//            }
        }


        List<XMutex<UUID>> res2 = results.stream()
                                         .filter(r -> r != f1)
                                         //.map(System::identityHashCode)
                                         .collect(toList());

        res2.forEach(System.out::println);
        //results.forEach(System.out::println);
        System.out.println(f1 + " " + System.identityHashCode(f1));
        System.out.println(id3);

        Assertions.assertThat(res2).hasSize(0);
        Assertions.assertThat(cmap.size()).isEqualTo(1);
        // Asserts
        List<XMutex<UUID>> res3 = results.stream()
                                         .filter(r -> !r.equals(f1))
                                         .collect(toList());
        Assertions.assertThat(res3).hasSize(0);
    }
}
