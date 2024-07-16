package org.babyfish.jimmer.sql.cache.spi;

import org.babyfish.jimmer.sql.cache.CacheTracker;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public abstract class AbstractCacheTracker implements CacheTracker {

    protected final List<InvalidationListener> invalidationListeners =
            new CopyOnWriteArrayList<>();

    protected final List<ReconnectListener> reconnectListeners =
            new CopyOnWriteArrayList<>();

    private final Firer firer = new FirerImpl();

    @Override
    public void addInvalidateListener(InvalidationListener listener) {
        if (listener != null) {
            invalidationListeners.add(listener);
        }
    }

    @Override
    public void removeInvalidateListener(InvalidationListener listener) {
        if (listener != null) {
            invalidationListeners.remove(listener);
        }
    }

    @Override
    public void addReconnectListener(ReconnectListener listener) {
        if (listener != null) {
            reconnectListeners.add(listener);
        }
    }

    @Override
    public void removeReconnectListener(ReconnectListener listener) {
        if (listener != null) {
            reconnectListeners.remove(listener);
        }
    }

    @Override
    public Firer firer() {
        return firer;
    }

    @Override
    public Publisher publisher() {
        return new PublisherImpl();
    }

    protected abstract void publishInvalidationEvent(InvalidationEvent event);

    private class FirerImpl implements Firer {

        @Override
        public void invalidate(InvalidationEvent message) {
            for (InvalidationListener listener : invalidationListeners) {
                listener.onInvalidate(message);
            }
        }

        @Override
        public void reconnect() {
            for (ReconnectListener listener : reconnectListeners) {
                listener.onReconnect();
            }
        }
    }

    private class PublisherImpl implements Publisher {

        @Override
        public void invalidate(InvalidationEvent event) {
            publishInvalidationEvent(event);
        }
    }
}
