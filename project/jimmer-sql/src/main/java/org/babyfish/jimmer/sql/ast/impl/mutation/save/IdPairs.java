package org.babyfish.jimmer.sql.ast.impl.mutation.save;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.sql.ast.tuple.Tuple2;

import java.util.Arrays;
import java.util.Collection;

interface IdPairs {

    Collection<Tuple2<Object, Object>> tuples();

    Collection<Tuple2<Object, Collection<Object>>> entries();

    static IdPairs of(Collection<ImmutableSpi> rows, ImmutableProp prop) {
        return new EntityIdPairsImpl(rows, prop);
    }

    static IdPairs of(Collection<Tuple2<Object, Object>> tuples) {
        return new TupleIdPairsImpl(tuples);
    }

    @SafeVarargs
    static IdPairs of(Tuple2<Object, Object>... tuples) {
        return new TupleIdPairsImpl(Arrays.asList(tuples));
    }
}

