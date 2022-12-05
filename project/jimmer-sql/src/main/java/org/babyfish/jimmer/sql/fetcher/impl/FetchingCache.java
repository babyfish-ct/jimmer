package org.babyfish.jimmer.sql.fetcher.impl;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.runtime.DraftContext;
import org.babyfish.jimmer.runtime.DraftSpi;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.sql.fetcher.Field;
import org.babyfish.jimmer.sql.fetcher.FieldFilter;
import org.babyfish.jimmer.sql.meta.ColumnDefinition;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class FetchingCache {

    private static final Object NULL = new Object();

    private Map<FieldKey, Map<Object, Object>> map = new HashMap<>();

    public Object createKey(Field field, ImmutableSpi owner) {
        ImmutableProp prop = field.getProp();
        if (prop.getStorage() instanceof ColumnDefinition) {
            Object fk = Ids.idOf((ImmutableSpi) owner.__get(prop.getId()));
            DraftContext ctx = owner instanceof DraftSpi ?
                    ((DraftSpi) owner).__draftContext() :
                    null;
            return new ForeignKey(fk, ctx);
        }
        return Ids.idOf(owner);
    }

    public Object get(Field field, Object key) {
        Map<Object, Object> subMap = map.get(new FieldKey((field)));
        if (subMap == null) {
            return null;
        }
        return subMap.get(key);
    }

    public void put(Field field, Object key, Object value) {
        Map<Object, Object> subMap = map.computeIfAbsent(
                new FieldKey((field)),
                it -> new HashMap<>()
        );
        subMap.put(key, value != null ? value : NULL);
    }

    public static Object unwrap(Object value) {
        return value == NULL ? null : value;
    }

    private static class FieldKey {

        private final ImmutableProp prop;

        private final FieldFilter<?> filter;

        private final int limit;

        private final int offset;

        FieldKey(Field field) {
            this(field.getProp(), field.getFilter(), field.getLimit(), field.getOffset());
        }

        FieldKey(ImmutableProp prop, FieldFilter<?> filter, int limit, int offset) {
            this.prop = prop;
            this.filter = filter;
            this.limit = limit;
            this.offset = offset;
        }

        @Override
        public int hashCode() {
            return Objects.hash(prop, filter, limit, offset);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            FieldKey fieldKey = (FieldKey) o;
            return limit == fieldKey.limit &&
                    offset == fieldKey.offset &&
                    prop.equals(fieldKey.prop) &&
                    Objects.equals(filter, fieldKey.filter);
        }

        @Override
        public String toString() {
            return "FieldKey{" +
                    "prop=" + prop +
                    ", filter=" + filter +
                    ", limit=" + limit +
                    ", offset=" + offset +
                    '}';
        }
    }

    private static class ForeignKey {

        private final Object raw;

        private final DraftContext ctx;

        private ForeignKey(Object raw, DraftContext ctx) {
            this.raw = raw;
            this.ctx = ctx;
        }

        @Override
        public int hashCode() {
            return Objects.hash(raw, ctx);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ForeignKey that = (ForeignKey) o;
            return Objects.equals(raw, that.raw) && ctx == that.ctx;
        }

        @Override
        public String toString() {
            return "ForeignKey{" +
                    "raw=" + raw +
                    "ctx=" + ctx +
                    '}';
        }
    }
}