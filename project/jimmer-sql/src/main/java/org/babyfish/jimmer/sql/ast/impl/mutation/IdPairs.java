package org.babyfish.jimmer.sql.ast.impl.mutation;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.sql.ast.tuple.Tuple2;

import java.util.Arrays;
import java.util.Collection;

interface IdPairs {

    Collection<Tuple2<Object, Object>> tuples();

    Collection<Tuple2<Object, Collection<Object>>> entries();

    boolean isEmpty();

    static Retain retain(Collection<? extends ImmutableSpi> rows, ImmutableProp prop) {
        return new EntityIdPairsImpl(rows, prop);
    }

    static Retain retain(IdPairs idPairs) {
        if (idPairs instanceof Retain) {
            return (Retain) idPairs;
        }
        return new Retain() {
            @Override
            public Collection<Tuple2<Object, Object>> tuples() {
                return idPairs.tuples();
            }

            @Override
            public Collection<Tuple2<Object, Collection<Object>>> entries() {
                return idPairs.entries();
            }

            @Override
            public boolean isEmpty() {
                return idPairs.isEmpty();
            }
        };
    }

    static IdPairs of(Collection<Tuple2<Object, Object>> tuples) {
        return new TupleIdPairsImpl(tuples);
    }

    @SafeVarargs
    static IdPairs of(Tuple2<Object, Object>... tuples) {
        return new TupleIdPairsImpl(Arrays.asList(tuples));
    }

    interface Retain extends IdPairs {}
}

