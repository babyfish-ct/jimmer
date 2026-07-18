package org.babyfish.jimmer.sql.meta.impl;

import org.babyfish.jimmer.sql.meta.MetadataStrategy;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class MetaCacheTest {

    @Test
    public void testPrimaryAndSecondaryValues() {
        AtomicInteger invocationCount = new AtomicInteger();
        MetaCache<Object> cache = new MetaCache<>(strategy -> {
            invocationCount.incrementAndGet();
            return new Object();
        });

        Object primaryValue = cache.get(new Strategy(1));
        Assertions.assertSame(primaryValue, cache.get(new Strategy(1)));

        Object secondaryValue = cache.get(new Strategy(2));
        Assertions.assertSame(secondaryValue, cache.get(new Strategy(2)));
        Assertions.assertNotSame(primaryValue, secondaryValue);
        Assertions.assertEquals(2, invocationCount.get());
    }

    @Test
    public void testConcurrentSecondaryValue() throws Exception {
        int threadCount = 8;
        AtomicInteger invocationCount = new AtomicInteger();
        CountDownLatch creatorStarted = new CountDownLatch(1);
        CountDownLatch creatorCanFinish = new CountDownLatch(1);
        MetaCache<Object> cache = new MetaCache<>(strategy -> {
            if (((Strategy) strategy).id == 2) {
                invocationCount.incrementAndGet();
                creatorStarted.countDown();
                try {
                    if (!creatorCanFinish.await(10, TimeUnit.SECONDS)) {
                        throw new AssertionError("Timed out waiting for concurrent callers");
                    }
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    throw new AssertionError(ex);
                }
            }
            return new Object();
        });
        cache.get(new Strategy(1));

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        try {
            List<Future<Object>> futures = new ArrayList<>();
            for (int i = 0; i < threadCount; i++) {
                futures.add(executor.submit(() -> cache.get(new Strategy(2))));
            }
            Assertions.assertTrue(creatorStarted.await(10, TimeUnit.SECONDS));
            creatorCanFinish.countDown();
            Object value = futures.get(0).get(10, TimeUnit.SECONDS);
            for (Future<Object> future : futures) {
                Assertions.assertSame(value, future.get(10, TimeUnit.SECONDS));
            }
            Assertions.assertEquals(1, invocationCount.get());
        } finally {
            creatorCanFinish.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    public void testMaxCount() {
        AtomicInteger invocationCount = new AtomicInteger();
        MetaCache<Object> cache = new MetaCache<>(strategy -> {
            invocationCount.incrementAndGet();
            return new Object();
        }, 2);

        cache.get(new Strategy(1));
        Object secondaryValue = cache.get(new Strategy(2));
        cache.get(new Strategy(3));

        IllegalStateException ex = Assertions.assertThrows(
                IllegalStateException.class,
                () -> cache.get(new Strategy(4))
        );
        Assertions.assertEquals(
                "Too many root sql clients are created, is it a bug?",
                ex.getMessage()
        );
        Assertions.assertSame(secondaryValue, cache.get(new Strategy(2)));
        Assertions.assertEquals(3, invocationCount.get());
    }

    private static class Strategy extends MetadataStrategy {

        final int id;

        Strategy(int id) {
            super(null, null, null, null, null, null);
            this.id = id;
        }

        @Override
        public int hashCode() {
            return id;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof Strategy)) {
                return false;
            }
            Strategy strategy = (Strategy) o;
            return id == strategy.id;
        }
    }
}
