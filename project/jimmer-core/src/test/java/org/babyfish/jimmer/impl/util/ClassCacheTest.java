package org.babyfish.jimmer.impl.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

public class ClassCacheTest {

    @Test
    public void testValue() {
        AtomicInteger invocationCount = new AtomicInteger();
        ClassCache<Object> cache = new ClassCache<>(type -> {
            invocationCount.incrementAndGet();
            return new Object();
        });

        Object stringValue = cache.get(String.class);
        Assertions.assertSame(stringValue, cache.get(String.class));
        Assertions.assertNotSame(stringValue, cache.get(Integer.class));
        Assertions.assertEquals(2, invocationCount.get());
    }

    @Test
    public void testNullValue() {
        AtomicInteger invocationCount = new AtomicInteger();
        ClassCache<Object> cache = new ClassCache<>(type -> {
            invocationCount.incrementAndGet();
            return null;
        });

        Assertions.assertNull(cache.get(String.class));
        Assertions.assertNull(cache.get(String.class));
        Assertions.assertEquals(1, invocationCount.get());
    }
}
