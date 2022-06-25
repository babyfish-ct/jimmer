package org.babyfish.jimmer.sql.cache;

import java.util.*;
import java.util.stream.Collectors;

final class CompositeCacheBinder implements CacheBinder {
    
    private final Set<CacheBinder> subBinders;

    private Collection<CacheImplementation<?>> implementations;

    private CompositeCacheBinder(Set<CacheBinder> subBinders) {
        this.subBinders = subBinders;
    }
    
    static CacheBinder of(Collection<? extends CacheBinder> binders) {
        if (binders == null || binders.isEmpty()) {
            return null;
        }
        Set<CacheBinder> subBinders = binders
                .stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (subBinders.isEmpty()) {
            return null;
        }
        if (subBinders.size() == 1) {
            return subBinders.iterator().next();
        }
        return new CompositeCacheBinder(Collections.unmodifiableSet(subBinders));
    }

    @SuppressWarnings("unchecked")
    public <V> Collection<CacheImplementation<V>> toImplementations() {
        Collection<CacheImplementation<?>> collection = implementations;
        if (collection == null) {
            boolean needConnect = false;
            for (CacheBinder subBinder : subBinders) {
                if (!(subBinder instanceof CacheImplementation<?>)) {
                    needConnect = true;
                    break;
                }
            }
            if (needConnect) {
                collection = new ArrayList<>();
                collectImplementations(collection);
            } else {
                collection = (Collection<CacheImplementation<?>>) (Collection<?>) subBinders;
            }
        }
        return (Collection<CacheImplementation<V>>) (Collection<?>) collection;
    }

    private void collectImplementations(
            Collection<CacheImplementation<?>> outCollection
    ) {
        for (CacheBinder subBinder : subBinders) {
            if (subBinder instanceof CacheImplementation<?>) {
                outCollection.add((CacheImplementation<?>) subBinder);
            } else {
                ((CompositeCacheBinder) subBinder).collectImplementations(outCollection);
            }
        }
    }
}
