package org.babyfish.jimmer.sql.cache;

import org.babyfish.jimmer.sql.ast.table.Props;
import org.babyfish.jimmer.sql.cache.impl.PropCacheInvalidators;
import org.babyfish.jimmer.sql.event.AssociationEvent;
import org.babyfish.jimmer.sql.event.EntityEvent;
import org.babyfish.jimmer.sql.filter.CacheableFilter;
import org.babyfish.jimmer.sql.filter.FilterArgs;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.SortedMap;

public class PropCacheInvalidatorTest {

    @Test
    public void test() {

        Assertions.assertFalse(
                PropCacheInvalidators.isGetAffectedSourceIdsOverridden(
                        new A(),
                        EntityEvent.class
                )
        );
        Assertions.assertFalse(
                PropCacheInvalidators.isGetAffectedSourceIdsOverridden(
                        new A(),
                        AssociationEvent.class
                )
        );

        Assertions.assertTrue(
                PropCacheInvalidators.isGetAffectedSourceIdsOverridden(
                        new B(),
                        EntityEvent.class
                )
        );
        Assertions.assertFalse(
                PropCacheInvalidators.isGetAffectedSourceIdsOverridden(
                        new B(),
                        AssociationEvent.class
                )
        );

        Assertions.assertFalse(
                PropCacheInvalidators.isGetAffectedSourceIdsOverridden(
                        new C(),
                        EntityEvent.class
                )
        );
        Assertions.assertTrue(
                PropCacheInvalidators.isGetAffectedSourceIdsOverridden(
                        new C(),
                        AssociationEvent.class
                )
        );

        Assertions.assertTrue(
                PropCacheInvalidators.isGetAffectedSourceIdsOverridden(
                        new D(),
                        EntityEvent.class
                )
        );
        Assertions.assertTrue(
                PropCacheInvalidators.isGetAffectedSourceIdsOverridden(
                        new D(),
                        AssociationEvent.class
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
}
