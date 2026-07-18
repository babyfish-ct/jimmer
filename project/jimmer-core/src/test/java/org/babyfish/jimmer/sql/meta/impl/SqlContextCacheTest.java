package org.babyfish.jimmer.sql.meta.impl;

import org.babyfish.jimmer.sql.meta.LogicalDeletedValueGenerator;
import org.babyfish.jimmer.sql.meta.MetadataStrategy;
import org.babyfish.jimmer.sql.meta.SqlContext;
import org.babyfish.jimmer.sql.meta.UserIdGenerator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class SqlContextCacheTest {

    @Test
    public void testPrimarySecondaryAndUnwrappedValues() {
        AtomicInteger invocationCount = new AtomicInteger();
        SqlContextCache<Object> cache = new SqlContextCache<>(context -> {
            invocationCount.incrementAndGet();
            return new Object();
        });
        Context primaryContext = new Context(1);
        Context secondaryContext = new Context(2);

        Object primaryValue = cache.get(primaryContext);
        Assertions.assertSame(primaryValue, cache.get(new Context(1)));

        Object secondaryValue = cache.get(secondaryContext);
        Assertions.assertSame(secondaryValue, cache.get(new Context(2)));
        Assertions.assertSame(secondaryValue, cache.get(new Context(3, secondaryContext)));
        Assertions.assertNotSame(primaryValue, secondaryValue);
        Assertions.assertEquals(2, invocationCount.get());
    }

    @Test
    public void testSecondaryMapReset() {
        AtomicInteger invocationCount = new AtomicInteger();
        SqlContextCache<Object> cache = new SqlContextCache<>(context -> {
            invocationCount.incrementAndGet();
            return new Object();
        });
        cache.get(new Context(0));

        List<Context> secondaryContexts = new ArrayList<>();
        for (int i = 1; i <= 513; i++) {
            Context context = new Context(i);
            secondaryContexts.add(context);
            cache.get(context);
        }
        Assertions.assertEquals(514, invocationCount.get());

        cache.get(new Context(514));
        Assertions.assertEquals(515, invocationCount.get());

        cache.get(secondaryContexts.get(0));
        Assertions.assertEquals(516, invocationCount.get());
    }

    private static class Context implements SqlContext {

        final int id;

        final SqlContext unwrapped;

        Context(int id) {
            this(id, null);
        }

        Context(int id, SqlContext unwrapped) {
            this.id = id;
            this.unwrapped = unwrapped;
        }

        @SuppressWarnings("unchecked")
        @Override
        public <T extends SqlContext> T unwrap() {
            return (T) unwrapped;
        }

        @Override
        public UserIdGenerator<?> getUserIdGenerator(String ref) {
            return null;
        }

        @Override
        public UserIdGenerator<?> getUserIdGenerator(Class<?> userIdGeneratorType) {
            return null;
        }

        @Override
        public LogicalDeletedValueGenerator<?> getLogicalDeletedValueGenerator(String ref) {
            return null;
        }

        @Override
        public LogicalDeletedValueGenerator<?> getLogicalDeletedValueGenerator(
                Class<?> logicalDeletedValueGeneratorType
        ) {
            return null;
        }

        @Override
        public MetadataStrategy getMetadataStrategy() {
            return null;
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
            if (!(o instanceof Context)) {
                return false;
            }
            Context context = (Context) o;
            return id == context.id;
        }
    }
}
