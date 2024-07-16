package org.babyfish.jimmer.sql.cache;

import org.babyfish.jimmer.impl.util.Classes;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.TargetLevel;
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

        private final Collection<?> ids;

        public InvalidationEvent(ImmutableType type, Collection<?> ids) {
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

        public InvalidationEvent(ImmutableProp prop, Collection<?> ids) {
            if (!prop.isAssociation(TargetLevel.ENTITY)) {
                throw new IllegalArgumentException(
                        "The prop \"" +
                                prop +
                                "\" is not association property"
                );
            }
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
            if (prop != null) {
                return prop.toString() + ids;
            }
            return type.toString() + ids;
        }
    }
}
