package org.babyfish.jimmer.impl.util;

import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.model.Book;
import org.babyfish.jimmer.model.BookStore;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

public class TypeCacheTest {

    @Test
    public void testCanonicalTypes() {
        AtomicInteger invocationCount = new AtomicInteger();
        TypeCache<Object> cache = new TypeCache<>(type -> {
            invocationCount.incrementAndGet();
            return new Object();
        });
        ImmutableType bookType = ImmutableType.get(Book.class);
        ImmutableType storeType = ImmutableType.get(BookStore.class);

        Object bookValue = cache.get(bookType);
        Assertions.assertSame(bookValue, cache.get(bookType));
        Assertions.assertNotSame(bookValue, cache.get(storeType));
        Assertions.assertEquals(2, invocationCount.get());
    }

    @Test
    public void testNullValue() {
        AtomicInteger invocationCount = new AtomicInteger();
        TypeCache<Object> cache = new TypeCache<>(type -> {
            invocationCount.incrementAndGet();
            return null;
        }, true);
        ImmutableType bookType = ImmutableType.get(Book.class);

        Assertions.assertNull(cache.get(bookType));
        Assertions.assertNull(cache.get(bookType));
        Assertions.assertEquals(1, invocationCount.get());
    }
}
