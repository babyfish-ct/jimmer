package org.babyfish.jimmer.sql.event.impl;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

public class EvictContext {

    private static final ThreadLocal<EvictContext> LOCAL = new ThreadLocal<>();

    private final Set<EvictItem> items = new HashSet<>();

    private final Set<ImmutableProp> disabledAssociations = new HashSet<>();

    public static void execute(Runnable block) {
        EvictContext ctx = LOCAL.get();
        if (ctx != null) {
            block.run();
        } else {
            ctx = new EvictContext();
            LOCAL.set(ctx);
            try {
                block.run();
            } finally {
                LOCAL.remove();
            }
        }
    }

    @Nullable
    public static EvictContext get() {
        return LOCAL.get();
    }

    public boolean add(ImmutableType type, Object id) {
        return items.add(new EvictItem(type, id));
    }

    public boolean add(ImmutableProp prop, Object id) {
        if (disabledAssociations.contains(prop)) {
            return false;
        }
        return items.add(new EvictItem(prop, id));
    }

    public void disable(ImmutableProp prop) {
        this.disabledAssociations.add(prop);
        ImmutableProp opposite = prop.getOpposite();
        if (opposite != null) {
            this.disabledAssociations.add(opposite);
        }
    }

    public boolean isAllowed(ImmutableProp prop) {
        return !disabledAssociations.contains(prop);
    }

    private static class EvictItem {

        final Object sourceId;
        final Object meta;

        EvictItem(ImmutableType type, Object sourceId) {
            this.meta = type;
            this.sourceId = sourceId;
        }

        EvictItem(ImmutableProp prop, Object sourceId) {
            this.meta = prop;
            this.sourceId = sourceId;
        }

        @Override
        public int hashCode() {
            int result = sourceId.hashCode();
            result = 31 * result + meta.hashCode();
            return result;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            EvictItem evictItem = (EvictItem) o;

            if (!sourceId.equals(evictItem.sourceId)) return false;
            return meta.equals(evictItem.meta);
        }

        @Override
        public String toString() {
            return "EvictItem{" +
                    "sourceId=" + sourceId +
                    ", meta=" + meta +
                    '}';
        }
    }
}
