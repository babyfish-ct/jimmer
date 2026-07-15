package org.babyfish.jimmer.impl.util;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.model.Book;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class PropCacheTest {

    @Test
    public void testCanonicalProps() {
        AtomicInteger invocationCount = new AtomicInteger();
        PropCache<Object> cache = new PropCache<>(prop -> {
            invocationCount.incrementAndGet();
            return new Object();
        });
        ImmutableType bookType = ImmutableType.get(Book.class);
        ImmutableProp nameProp = bookType.getProp("name");
        ImmutableProp priceProp = bookType.getProp("price");

        Object nameValue = cache.get(nameProp);
        Assertions.assertSame(nameValue, cache.get(nameProp));
        Assertions.assertNotSame(nameValue, cache.get(priceProp));
        Assertions.assertEquals(2, invocationCount.get());
    }

    @Test
    public void testNullValue() {
        AtomicInteger invocationCount = new AtomicInteger();
        PropCache<Object> cache = new PropCache<>(prop -> {
            invocationCount.incrementAndGet();
            return null;
        }, true);
        ImmutableProp nameProp = ImmutableType.get(Book.class).getProp("name");

        Assertions.assertNull(cache.get(nameProp));
        Assertions.assertNull(cache.get(nameProp));
        Assertions.assertEquals(1, invocationCount.get());
    }

    @Test
    public void testConcurrentValue() throws Exception {
        int threadCount = 8;
        AtomicInteger invocationCount = new AtomicInteger();
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch start = new CountDownLatch(1);
        PropCache<Object> cache = new PropCache<>(prop -> {
            invocationCount.incrementAndGet();
            ready.countDown();
            try {
                if (!start.await(10, TimeUnit.SECONDS)) {
                    throw new AssertionError("Timed out waiting for concurrent creators");
                }
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new AssertionError(ex);
            }
            return new Object();
        });
        ImmutableProp nameProp = ImmutableType.get(Book.class).getProp("name");
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        try {
            List<Future<Object>> futures = new ArrayList<>();
            for (int i = 0; i < threadCount; i++) {
                futures.add(executor.submit(() -> cache.get(nameProp)));
            }
            Assertions.assertTrue(ready.await(10, TimeUnit.SECONDS));
            start.countDown();
            Object value = futures.get(0).get(10, TimeUnit.SECONDS);
            for (Future<Object> future : futures) {
                Assertions.assertSame(value, future.get(10, TimeUnit.SECONDS));
            }
            Assertions.assertEquals(threadCount, invocationCount.get());
        } finally {
            start.countDown();
            executor.shutdownNow();
        }
    }
}
