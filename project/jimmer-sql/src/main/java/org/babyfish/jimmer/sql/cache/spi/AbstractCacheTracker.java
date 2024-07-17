package org.babyfish.jimmer.sql.cache.spi;

import net.bytebuddy.implementation.bytecode.Throw;
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

    protected abstract void publishInvalidationEvent(InvalidateEvent event);

    private class FirerImpl implements Firer {

        @Override
        public void invalidate(InvalidateEvent message) {
            Rethrow rethrow = new Rethrow();
            for (InvalidationListener listener : invalidationListeners) {
                try {
                    listener.onInvalidate(message);
                } catch (RuntimeException | Error ex) {
                    rethrow.set(ex);
                }
            }
            rethrow.execute();
        }

        @Override
        public void reconnect() {
            Rethrow rethrow = new Rethrow();
            for (ReconnectListener listener : reconnectListeners) {
                try {
                    listener.onReconnect();
                } catch (RuntimeException | Error ex) {
                    rethrow.set(ex);
                }
            }
            rethrow.execute();
        }
    }

    private class PublisherImpl implements Publisher {

        @Override
        public void invalidate(InvalidateEvent event) {
            publishInvalidationEvent(event);
        }
    }

    private static class Rethrow {

        private Throwable ex;

        public void set(Throwable ex) {
            if (this.ex != null) {
                return;
            }
            if (!(ex instanceof RuntimeException) && !(ex instanceof Error)) {
                throw new IllegalArgumentException("ex is neither RuntimeException nor Error");
            }
            this.ex = ex;
        }

        public void execute() {
            if (ex instanceof RuntimeException) {
                throw (RuntimeException) ex;
            }
            if (ex instanceof Error) {
                throw (Error) ex;
            }
        }
    }
}
