package org.babyfish.jimmer.sql.cache.spi;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.sql.cache.CacheTracker;
import org.babyfish.jimmer.sql.cache.chain.Binder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;

public abstract class AbstractBinder<K> implements Binder<K> {

    protected final ImmutableType type;

    protected final ImmutableProp prop;

    public AbstractBinder(
            @Nullable ImmutableType type,
            @Nullable ImmutableProp prop,
            @Nullable CacheTracker tracker
    ) {
        if ((type == null) == (prop == null)) {
            throw new IllegalArgumentException("The nullity of type and prop must be different");
        }
        if (prop == null) {
            this.type = type;
            this.prop = null;
        } else {
            this.type = prop.getDeclaringType();
            this.prop = prop;
        }
        if (tracker != null) {
            tracker.addInvalidationListener(new InvalidationListenerImpl());
            tracker.addReconnectListener(this::invalidateAll);
        }
    }

    @Override
    public final @NotNull ImmutableType type() {
        return type;
    }

    @Override
    public final @Nullable ImmutableProp prop() {
        return prop;
    }

    protected abstract void invalidateAll();

    private class InvalidationListenerImpl implements CacheTracker.InvalidationListener {

        @SuppressWarnings("unchecked")
        @Override
        public void onInvalidate(CacheTracker.InvalidationEvent event) {
            if (type == event.getType() && prop == event.getProp()) {
                deleteAll(Collections.singleton((K)event.getId()), null);
            }
        }
    }
}
