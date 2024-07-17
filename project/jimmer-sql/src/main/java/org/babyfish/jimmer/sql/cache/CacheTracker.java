package org.babyfish.jimmer.sql.cache;

import org.babyfish.jimmer.impl.util.Classes;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

public interface CacheTracker {

    void addInvalidateListener(InvalidationListener listener);

    void removeInvalidateListener(InvalidationListener listener);

    void addReconnectListener(ReconnectListener listener);

    void removeReconnectListener(ReconnectListener listener);

    Firer firer();

    Publisher publisher();

    @FunctionalInterface
    interface InvalidationListener {
        void onInvalidate(InvalidateEvent event);
    }

    interface ReconnectListener {
        void onReconnect();
    }

    interface Firer {

        void invalidate(InvalidateEvent event);

        void reconnect();
    }

    interface Publisher {

        void invalidate(InvalidateEvent event);
    }

    class InvalidateEvent {

        private final ImmutableType type;

        private final ImmutableProp prop;

        private final Collection<?> ids;

        public InvalidateEvent(ImmutableType type, Collection<?> ids) {
            Class<?> expectedIdType = Classes.boxTypeOf(type.getIdProp().getReturnClass());
            for (Object id : ids) {
                if (id == null || !expectedIdType.isAssignableFrom(id.getClass())) {
                    throw new IllegalArgumentException(
                            "The type of id \"" +
                                    id +
                                    "\" does not match the id property \"" +
                                    type.getIdProp() +
                                    "\""
                    );
                }
            }
            this.type = type;
            this.prop = null;
            this.ids = ids;
        }

        public InvalidateEvent(ImmutableProp prop, Collection<?> ids) {
            ImmutableType type = prop.getDeclaringType();
            ImmutableProp idProp = type.getIdProp();
            Class<?> expectedIdType = Classes.boxTypeOf(idProp.getReturnClass());
            for (Object id : ids) {
                if (id == null || !expectedIdType.isAssignableFrom(id.getClass())) {
                    throw new IllegalArgumentException(
                            "The id \"" +
                                    id +
                                    "\" does not match the id property \"" +
                                    idProp +
                                    "\" of declaring type"
                    );
                }
            }
            this.type = type;
            this.prop = prop;
            this.ids = ids;
        }

        @NotNull
        public ImmutableType getType() {
            return type;
        }

        @Nullable
        public ImmutableProp getProp() {
            return prop;
        }

        @NotNull
        public Collection<?> getIds() {
            return ids;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            if (prop != null) {
                builder.append(prop);
            } else {
                builder.append(type);
            }
            builder.append('[');
            boolean addComma = false;
            for (Object id : ids) {
                if (addComma) {
                    builder.append(", ");
                } else {
                    addComma = true;
                }
                builder.append(id);
            }
            builder.append(']');
            return builder.toString();
        }
    }
}
