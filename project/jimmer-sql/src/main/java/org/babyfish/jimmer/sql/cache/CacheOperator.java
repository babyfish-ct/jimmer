package org.babyfish.jimmer.sql.cache;

import org.babyfish.jimmer.sql.JSqlClient;

import java.util.Collection;

public interface CacheOperator {

    default void initialize(JSqlClient sqlClient) {}

    void delete(LocatedCache<Object, ?> cache, Object key, Object reason);

    void deleteAll(LocatedCache<Object, ?> cache, Collection<Object> keys, Object reason);

    static boolean isSuspending() {
        return Suspending.LOCAL.get() != null;
    }

    static void suspending(Runnable block) {
        if (Suspending.LOCAL.get() != null) {
            block.run();
        } else {
            Suspending.LOCAL.set(Suspending.INSTANCE);
            try {
                block.run();
            } finally {
                Suspending.LOCAL.remove();
            }
        }
    }
}

class Suspending {
    static final ThreadLocal<Suspending> LOCAL = new ThreadLocal<>();
    static final Suspending INSTANCE = new Suspending();
    private Suspending() {}
}
