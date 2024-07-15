package org.babyfish.jimmer.sql.cache;

import org.babyfish.jimmer.sql.TransientResolver;
import org.babyfish.jimmer.sql.ast.table.Props;
import org.babyfish.jimmer.sql.cache.spi.PropCacheInvalidators;
import org.babyfish.jimmer.sql.event.AssociationEvent;
import org.babyfish.jimmer.sql.event.EntityEvent;
import org.babyfish.jimmer.sql.filter.CacheableFilter;
import org.babyfish.jimmer.sql.filter.FilterArgs;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.Map;
import java.util.SortedMap;

public class PropCacheInvalidatorTest {

    @Test
    public void test() {

        Assertions.assertFalse(
                PropCacheInvalidators.isGetAffectedSourceIdsOverridden(
                        new A(),
                        EntityEvent.class,
                        null
                )
        );
        Assertions.assertFalse(
                PropCacheInvalidators.isGetAffectedSourceIdsOverridden(
                        new A(),
                        AssociationEvent.class,
                        null
                )
        );

        Assertions.assertTrue(
                PropCacheInvalidators.isGetAffectedSourceIdsOverridden(
                        new B(),
                        EntityEvent.class,
                        null
                )
        );
        Assertions.assertFalse(
                PropCacheInvalidators.isGetAffectedSourceIdsOverridden(
                        new B(),
                        AssociationEvent.class,
                        null
                )
        );

        Assertions.assertFalse(
                PropCacheInvalidators.isGetAffectedSourceIdsOverridden(
                        new C(),
                        EntityEvent.class,
                        null
                )
        );
        Assertions.assertTrue(
                PropCacheInvalidators.isGetAffectedSourceIdsOverridden(
                        new C(),
                        AssociationEvent.class,
                        null
                )
        );

        Assertions.assertTrue(
                PropCacheInvalidators.isGetAffectedSourceIdsOverridden(
                        new D(),
                        EntityEvent.class,
                        null
                )
        );
        Assertions.assertTrue(
                PropCacheInvalidators.isGetAffectedSourceIdsOverridden(
                        new D(),
                        AssociationEvent.class,
                        null
                )
        );

        Assertions.assertFalse(
                PropCacheInvalidators.isGetAffectedSourceIdsOverridden(
                        new E(),
                        EntityEvent.class,
                        null
                )
        );
        Assertions.assertFalse(
                PropCacheInvalidators.isGetAffectedSourceIdsOverridden(
                        new E(),
                        AssociationEvent.class,
                        null
                )
        );

        Assertions.assertTrue(
                PropCacheInvalidators.isGetAffectedSourceIdsOverridden(
                        new F(),
                        EntityEvent.class,
                        null
                )
        );
        Assertions.assertFalse(
                PropCacheInvalidators.isGetAffectedSourceIdsOverridden(
                        new F(),
                        AssociationEvent.class,
                        null
                )
        );

        Assertions.assertFalse(
                PropCacheInvalidators.isGetAffectedSourceIdsOverridden(
                        new G(),
                        EntityEvent.class,
                        null
                )
        );
        Assertions.assertTrue(
                PropCacheInvalidators.isGetAffectedSourceIdsOverridden(
                        new G(),
                        AssociationEvent.class,
                        null
                )
        );

        Assertions.assertTrue(
                PropCacheInvalidators.isGetAffectedSourceIdsOverridden(
                        new H(),
                        EntityEvent.class,
                        null
                )
        );
        Assertions.assertTrue(
                PropCacheInvalidators.isGetAffectedSourceIdsOverridden(
                        new H(),
                        AssociationEvent.class,
                        null
                )
        );
    }

    private static class A implements CacheableFilter<Props> {

        @Override
        public SortedMap<String, Object> getParameters() {
            return null;
        }

        @Override
        public boolean isAffectedBy(EntityEvent<?> e) {
            return false;
        }

        @Override
        public void filter(FilterArgs<Props> args) {

        }
    }

    private static class B implements CacheableFilter<Props> {

        @Override
        public SortedMap<String, Object> getParameters() {
            return null;
        }

        @Override
        public boolean isAffectedBy(EntityEvent<?> e) {
            return false;
        }

        @Override
        public void filter(FilterArgs<Props> args) {

        }

        @Nullable
        @Override
        public Collection<?> getAffectedSourceIds(@NotNull EntityEvent<?> e) {
            return null;
        }
    }

    private static class C implements CacheableFilter<Props> {

        @Override
        public SortedMap<String, Object> getParameters() {
            return null;
        }

        @Override
        public boolean isAffectedBy(EntityEvent<?> e) {
            return false;
        }

        @Override
        public void filter(FilterArgs<Props> args) {

        }

        @Nullable
        @Override
        public Collection<?> getAffectedSourceIds(@NotNull AssociationEvent e) {
            return null;
        }
    }

    private static class D implements CacheableFilter<Props> {

        @Override
        public SortedMap<String, Object> getParameters() {
            return null;
        }

        @Override
        public boolean isAffectedBy(EntityEvent<?> e) {
            return false;
        }

        @Override
        public void filter(FilterArgs<Props> args) {

        }

        @Nullable
        @Override
        public Collection<?> getAffectedSourceIds(@NotNull EntityEvent<?> e) {
            return null;
        }

        @Nullable
        @Override
        public Collection<Object> getAffectedSourceIds(@NotNull AssociationEvent e) {
            return null;
        }
    }

    private static class E implements TransientResolver<Long, Long> {

        @Override
        public Map<Long, Long> resolve(Collection<Long> longs) {
            return null;
        }
    }

    private static class F implements TransientResolver<Long, Long> {

        @Override
        public Map<Long, Long> resolve(Collection<Long> longs) {
            return null;
        }

        @Nullable
        @Override
        public Collection<?> getAffectedSourceIds(@NotNull EntityEvent<?> e) {
            return null;
        }
    }

    private static class G implements TransientResolver<Long, Long> {

        @Override
        public Map<Long, Long> resolve(Collection<Long> longs) {
            return null;
        }

        @Nullable
        @Override
        public Collection<Object> getAffectedSourceIds(@NotNull AssociationEvent e) {
            return null;
        }
    }

    private static class H implements TransientResolver<Long, Long> {

        @Override
        public Map<Long, Long> resolve(Collection<Long> longs) {
            return null;
        }

        @Nullable
        @Override
        public Collection<?> getAffectedSourceIds(@NotNull EntityEvent<?> e) {
            return null;
        }

        @Nullable
        @Override
        public Collection<Object> getAffectedSourceIds(@NotNull AssociationEvent e) {
            return null;
        }
    }
}
