package org.babyfish.jimmer.sql.cache.spi;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.sql.cache.CacheTracker;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

public abstract class AbstractTrackingProducerBinder<K> extends AbstractBinder<K> {

    private final CacheTracker tracker;

    public AbstractTrackingProducerBinder(
            @Nullable ImmutableType type,
            @Nullable ImmutableProp prop,
            @Nullable CacheTracker tracker
    ) {
        super(type, prop);
        this.tracker = tracker;
    }

    @Override
    public final void deleteAllImpl(Collection<K> keys) {
        deleteAllKeys(keys);
        if (tracker == null) {
            return;
        }
        Set<?> ids = keys instanceof Set<?> ?
                (Set<?>) keys :
                new LinkedHashSet<Object>(keys);
        if (prop != null) {
            tracker.publisher().invalidate(
                    new CacheTracker.InvalidationEvent(
                            prop,
                            ids
                    )
            );
        } else {
            assert type != null;
            tracker.publisher().invalidate(
                    new CacheTracker.InvalidationEvent(
                            type,
                            ids
                    )
            );
        }
    }

    protected abstract void deleteAllKeys(Collection<K> keys);
}
