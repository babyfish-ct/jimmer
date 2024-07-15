package org.babyfish.jimmer.sql.cache;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.TargetLevel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface CacheTracker {

    void addInvalidationListener(InvalidationListener listener);

    void removeInvalidationListener(InvalidationListener listener);

    void addReconnectListener(ReconnectListener listener);

    void removeReconnectListener(ReconnectListener listener);

    Firer firer();

    Publisher publisher();

    @FunctionalInterface
    interface InvalidationListener {
        void onInvalidate(InvalidationEvent event);
    }

    interface ReconnectListener {
        void onReconnect();
    }

    interface Firer {

        void invalidate(InvalidationEvent event);

        void reconnect();
    }

    interface Publisher {

        void invalidate(InvalidationEvent event);
    }

    class InvalidationEvent {

        private final ImmutableType type;

        private final ImmutableProp prop;

        private final Object id;

        public InvalidationEvent(ImmutableType type, Object id) {
            Class<?> idType = type.getIdProp().getReturnClass();
            if (id == null || !idType.isAssignableFrom(id.getClass())) {
                throw new IllegalArgumentException(
                        "The id \"" +
                                id +
                                "\" does not match the id property \"" +
                                type.getIdProp() +
                                "\""
                );
            }
            this.type = type;
            this.prop = null;
            this.id = id;
        }

        public InvalidationEvent(ImmutableProp prop, Object id) {
            if (!prop.isAssociation(TargetLevel.ENTITY)) {
                throw new IllegalArgumentException(
                        "The prop \"" +
                                prop +
                                "\" is not association property"
                );
            }
            ImmutableType type = prop.getDeclaringType();
            ImmutableProp idProp = type.getIdProp();
            if (id == null || !idProp.getReturnClass().isAssignableFrom(id.getClass())) {
                throw new IllegalArgumentException(
                        "The id \"" +
                                id +
                                "\" does not match the id property \"" +
                                idProp +
                                "\" of declaring type"
                );
            }
            this.type = type;
            this.prop = prop;
            this.id = id;
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
        public Object getId() {
            return id;
        }
    }
}
