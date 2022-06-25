package org.babyfish.jimmer.sql.cache;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

public interface CacheBinder {

    static CacheBinder of(CacheBinder ... binders) {
        return CompositeCacheBinder.of(Arrays.asList(binders));
    }

    static CacheBinder of(Collection<? extends CacheBinder> binders) {
        return CompositeCacheBinder.of(binders);
    }

    @SuppressWarnings("unchecked")
    static <V> Collection<CacheImplementation<V>> toImplementations(CacheBinder cacheBinder) {
        if (cacheBinder instanceof CacheImplementation<?>) {
            return Collections.singletonList((CacheImplementation<V>) cacheBinder);
        }
        return ((CompositeCacheBinder)cacheBinder).toImplementations();
    }
}
