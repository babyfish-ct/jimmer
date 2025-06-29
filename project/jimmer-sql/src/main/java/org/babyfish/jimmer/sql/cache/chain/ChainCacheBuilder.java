package org.babyfish.jimmer.sql.cache.chain;

import org.babyfish.jimmer.sql.cache.Cache;

import java.util.ArrayList;
import java.util.List;

public class ChainCacheBuilder<K, V> {

    private final List<Binder<K>> binders = new ArrayList<>();

    private Boolean hasParameterizedBinder = null;

    private boolean hasLockableBinder = false;

    private final Binder.TrackingMode trackingMode = Binder.TrackingMode.NONE;

    public ChainCacheBuilder<K, V> add(LoadingBinder<K, V> binder) {
        if (binder != null) {
            if (Boolean.TRUE.equals(hasParameterizedBinder)) {
                throw new IllegalStateException(
                        "Parameterized binder and normal binder cannot be mixed"
                );
            }
            hasParameterizedBinder = false;
            validateNewBinder(binder);
            binders.add(binder);
        }
        return this;
    }

    public ChainCacheBuilder<K, V> add(LoadingBinder.Parameterized<K, V> binder) {
        if (binder != null) {
            if (Boolean.FALSE.equals(hasParameterizedBinder)) {
                throw new IllegalStateException(
                        "Parameterized binder and normal binder cannot be mixed"
                );
            }
            hasParameterizedBinder = true;
            validateNewBinder(binder);
            binders.add(binder);
        }
        return this;
    }

    public ChainCacheBuilder<K, V> add(SimpleBinder<K, V> binder) {
        if (binder != null) {
            boolean isParameterized = binder instanceof SimpleBinder.Parameterized<?, ?>;
            if (hasParameterizedBinder != null && !hasParameterizedBinder.equals(isParameterized)) {
                throw new IllegalStateException(
                        "Parameterized binder and normal binder cannot be mixed"
                );
            }
            hasParameterizedBinder = isParameterized;
            validateNewBinder(binder);
            binders.add(binder);
        }
        return this;
    }

    private void validateNewBinder(Binder<K> binder) {
        if (binder instanceof LockedBinder) {
            this.hasLockableBinder = true;
        } else if (this.hasLockableBinder) {
            throw new IllegalStateException(
                    "Non-lockable binder cannot be added after lockable binder"
            );
        }
        switch (binder.tracingMode()) {
            case NONE:
            case CONSUMER:
                if (trackingMode != Binder.TrackingMode.NONE) {
                    throw new IllegalStateException(
                            "The binder with the tracking mode \"" +
                                    binder.tracingMode() +
                                    "\" cannot be added " +
                                    "after lockable binder with the tracking mode \"" +
                                    trackingMode.name() +
                                    "\""
                    );
                }
                break;
            case PRODUCER:
                if (trackingMode != Binder.TrackingMode.CONSUMER && trackingMode != Binder.TrackingMode.NONE) {
                    throw new IllegalStateException(
                            "The binder with the tracking mode \"" +
                                    binder.tracingMode() +
                                    "\" cannot be added " +
                                    "after lockable binder with the tracking mode \"" +
                                    trackingMode.name() +
                                    "\""
                    );
                }
                break;
        }
    }

    public Cache<K, V> build() {
        List<Binder<K>> binders = this.binders;
        if (binders.isEmpty()) {
            return null;
        }
        if (hasParameterizedBinder) {
            return new ParameterizedChainCacheImpl<>(binders);
        }
        return new ChainCacheImpl<>(binders);
    }
}
