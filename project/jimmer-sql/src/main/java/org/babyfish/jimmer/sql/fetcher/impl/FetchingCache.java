package org.babyfish.jimmer.sql.fetcher.impl;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.runtime.DraftContext;
import org.babyfish.jimmer.runtime.DraftSpi;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.sql.fetcher.Field;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class FetchingCache {

    private static final Object NULL = new Object();

    private final Map<FieldKey, Map<Object, Object>> map = new HashMap<>();

    public Object createKey(Field field, ImmutableSpi owner) {
        ImmutableProp prop = field.getProp();
        if (prop.isColumnDefinition()) {
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
        Object value = subMap.get(key);
        return value != NULL ? value : null;
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

        private final Field field;

        private FieldKey(Field field) {
            this.field = field;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            FieldKey fieldKey = (FieldKey) o;

            return field.equals(fieldKey.field);
        }

        @Override
        public int hashCode() {
            return field.hashCode();
        }

        @Override
        public String toString() {
            return "FieldKey{" +
                    "field=" + field +
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