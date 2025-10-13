package org.babyfish.jimmer.sql.fetcher.impl;

import kotlin.collections.ArrayDeque;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.sql.fetcher.Fetcher;

import java.util.Collections;
import java.util.List;
import java.util.function.BiPredicate;

public class FetcherFactory {

    private FetcherFactory() {}

    public static <E> Fetcher<E> filter(
            Fetcher<E> self,
            BiPredicate<ImmutableType, List<ImmutableProp>> typePredicate,
            BiPredicate<ImmutableProp, List<ImmutableProp>> propPredicate
    ) {
        if (typePredicate == null && propPredicate == null) {
            return self;
        }
        return filterImpl((FetcherImpl<E>) self, typePredicate, propPredicate, new ArrayDeque<>());
    }

    private static <E> FetcherImpl<E> filterImpl(
            FetcherImpl<E> self,
            BiPredicate<ImmutableType, List<ImmutableProp>> typePredicate,
            BiPredicate<ImmutableProp, List<ImmutableProp>> propPredicate,
            ArrayDeque<ImmutableProp> path
    ) {
        if (self == null) {
            return null;
        }
        List<ImmutableProp> immutablePath = Collections.unmodifiableList(path);
        if (typePredicate != null && !typePredicate.test(self.getImmutableType(), immutablePath)) {
            return null;
        }
        FetcherImpl<E> filteredPrevFetcher = filterImpl(self.prev, typePredicate, propPredicate, path);
        if (!self.negative && !self.prop.isId()) {
            if (propPredicate != null && !propPredicate.test(self.prop, immutablePath)) {
                return filteredPrevFetcher;
            }
            FetcherImpl<?> childFetcher = self.childFetcher;
            if (childFetcher != null) {
                path.addLast(self.prop);
                FetcherImpl<?> filteredChildFetcher = filterImpl(childFetcher, typePredicate, propPredicate, path);
                path.removeLast();
                if (filteredChildFetcher == null) {
                    return filteredPrevFetcher;
                }
                return new FetcherImpl<>(filteredPrevFetcher, self, filteredChildFetcher);
            }
        }
        return new FetcherImpl<>(filteredPrevFetcher, self, self.childFetcher);
    }

    public static <E> Fetcher<E> excludeMicroServiceNameExceptRoot(
            Fetcher<E> fetcher,
            String microServiceName
    ) {
        return filter(
                fetcher,
                (type, path) -> path.isEmpty() || !type.getMicroServiceName().equals(microServiceName),
                null
        );
    }
}
