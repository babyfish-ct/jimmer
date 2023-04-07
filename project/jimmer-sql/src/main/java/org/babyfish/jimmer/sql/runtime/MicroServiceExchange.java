package org.babyfish.jimmer.sql.runtime;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.sql.ast.tuple.Tuple2;
import org.babyfish.jimmer.sql.fetcher.Fetcher;

import java.util.Collection;
import java.util.List;

public interface MicroServiceExchange {

    List<ImmutableSpi> findByIds(
            String microServiceName,
            Collection<?> ids,
            Fetcher<?> fetcher
    ) throws Exception;

    List<Tuple2<Object, ImmutableSpi>> findByAssociatedIds(
            String microServiceName,
            ImmutableProp prop,
            Collection<?> targetIds,
            Fetcher<?> fetcher
    ) throws Exception;
}
