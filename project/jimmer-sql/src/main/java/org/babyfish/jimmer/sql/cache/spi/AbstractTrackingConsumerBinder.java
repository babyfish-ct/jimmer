package org.babyfish.jimmer.sql.cache.spi;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.sql.cache.CacheTracker;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

public abstract class AbstractTrackingConsumerBinder<K> extends AbstractBinder<K> {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(AbstractTrackingConsumerBinder.class);

    public AbstractTrackingConsumerBinder(
            @Nullable ImmutableType type,
            @Nullable ImmutableProp prop,
            @Nullable CacheTracker tracker
    ) {
        super(type, prop);
        if (tracker != null) {
            tracker.addInvalidateListener(new InvalidateListenerImpl());
            tracker.addReconnectListener(new ReconnectListenerImpl());
        }
    }

    protected abstract void invalidateAll();

    private class InvalidateListenerImpl implements CacheTracker.InvalidationListener {

        @SuppressWarnings("unchecked")
        @Override
        public void onInvalidate(CacheTracker.InvalidationEvent event) {
            if (type == event.getType() && prop == event.getProp()) {
                deleteAll((Collection<K>) event.getIds(), null);
            }
        }
    }

    private class ReconnectListenerImpl implements CacheTracker.ReconnectListener {

        @Override
        public void onReconnect() {
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info(
                        "Cache.DeleteAll > {}",
                        AbstractTrackingConsumerBinder.this.toString(true)
                );
            }
            invalidateAll();
        }
    }
}
