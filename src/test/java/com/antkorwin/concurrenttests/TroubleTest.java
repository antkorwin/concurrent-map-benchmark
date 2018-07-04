package com.antkorwin.concurrenttests;

import com.antkorwin.xsync.XMutex;
import com.antkorwin.xsync.XMutexFactory;
import com.antkorwin.xsync.springframework.util.ConcurrentReferenceHashMap;
import org.assertj.core.api.Assertions;
import org.junit.Test;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.toList;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.equalTo;

/**
 * Created by Korovin Anatolii on 03.07.2018.
 *
 * @author Korovin Anatolii
 * @version 1.0
 */
public class TroubleTest {

    public static final int TIMEOUT_FOR_PREVENTION_OF_DEADLOCK = 30000;
    private static final int NUMBER_OF_MUTEXES = 100000;
    private static final int NUMBER_OF_ITERATIONS = NUMBER_OF_MUTEXES * 100;
    private static final String ID_STRING = "c117c526-606e-41b6-8197-1a6ba779f69b";

    @Test(timeout = TIMEOUT_FOR_PREVENTION_OF_DEADLOCK)
    public void testConcurrency() {
        // Arrange
        XMutexFactory<UUID> mutexFactory = new XMutexFactory<>();

        List<UUID> ids = IntStream.range(0, NUMBER_OF_MUTEXES)
                                  .boxed()
                                  .map(i -> UUID.randomUUID())
                                  .collect(toList());

        Set<XMutex<UUID>> results = createConcurrentSet();
        Set<Integer> setOfHash = createConcurrentSet();

        // Act
        IntStream.range(0, NUMBER_OF_ITERATIONS)
                 .boxed()
                 .parallel()
                 .forEach(i -> {
                     UUID uuid = ids.get(i % NUMBER_OF_MUTEXES);
                     XMutex<UUID> mutex = mutexFactory.getMutex(uuid);
                     results.add(mutex);
                     setOfHash.add(System.identityHashCode(mutex));
                 });

        await().atMost(10, TimeUnit.SECONDS)
               .until(results::size, equalTo(NUMBER_OF_MUTEXES));

        await().atMost(10, TimeUnit.SECONDS)
               .until(setOfHash::size, equalTo(NUMBER_OF_MUTEXES));

        // Asserts
        Assertions.assertThat(results).hasSize(NUMBER_OF_MUTEXES);
        Assertions.assertThat(setOfHash).hasSize(NUMBER_OF_MUTEXES);

        await().atMost(10, TimeUnit.SECONDS)
               .until(mutexFactory::size, equalTo((long) NUMBER_OF_MUTEXES));

        Assertions.assertThat(mutexFactory.size()).isEqualTo(NUMBER_OF_MUTEXES);
    }

    private <TypeT> Set<TypeT> createConcurrentSet() {
        return Collections.newSetFromMap(new ConcurrentHashMap<TypeT, Boolean>());
    }

    @Test(timeout = TIMEOUT_FOR_PREVENTION_OF_DEADLOCK)
    public void executor() {
        // Arrange
        ExecutorService executorService = Executors.newFixedThreadPool(10);

        XMutexFactory<UUID> mutexFactory = new XMutexFactory<>();
        int numberOfMutexes = 10;
        int numberOfIterations = 100;

        List<UUID> ids = IntStream.range(0, numberOfMutexes)
                                  .boxed()
                                  .map(i -> UUID.randomUUID())
                                  .collect(toList());

        Set<XMutex<UUID>> results = createConcurrentSet();

        // Act

        IntStream.range(0, numberOfIterations)
                 .boxed()
                 .forEach(i -> executorService.submit(() -> {
                     UUID uuid = ids.get(i % numberOfMutexes);
                     XMutex<UUID> mutex = mutexFactory.getMutex(uuid);
                     results.add(mutex);
                 }));

        // Asserts
        await().atMost(10, TimeUnit.SECONDS)
               .until(results::size, equalTo(numberOfMutexes));

        Assertions.assertThat(results).hasSize(numberOfMutexes);

        await().atMost(10, TimeUnit.SECONDS)
               .until(mutexFactory::size, equalTo((long) numberOfMutexes));

        Assertions.assertThat(mutexFactory.size()).isEqualTo(numberOfMutexes);
    }


    @Test
    public void testALotOfHashCodes() {
        // Arrange
        XMutexFactory<UUID> mutexFactory =
                new XMutexFactory<UUID>(16,
                                        ConcurrentReferenceHashMap.ReferenceType.WEAK);

        Set<XMutex<UUID>> resultReferences = createConcurrentSet();
        UUID key = UUID.fromString(ID_STRING);

        XMutex<UUID> firstMutex = mutexFactory.getMutex(key);

        // Act
        for (int i = 0; i < NUMBER_OF_ITERATIONS; i++) {
            XMutex<UUID> mutex = mutexFactory.getMutex(UUID.fromString(ID_STRING));
            // and now, we save reference in the set of mutexes,
            // because the GC can delete a unused references:
            resultReferences.add(mutex);
            // Assert
            Assertions.assertThat(mutex == firstMutex).isTrue();
        }

        // Assertions
        Assertions.assertThat(resultReferences).hasSize(1);
    }
}
