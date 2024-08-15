package org.babyfish.jimmer.sql.ast.impl.mutation.save;

import org.babyfish.jimmer.sql.ast.tuple.Tuple2;

import java.util.Arrays;
import java.util.Collection;

public class RetainIdPairs implements IdPairs.Retain {

    private final IdPairs raw;

    private RetainIdPairs(IdPairs raw) {
        this.raw = raw;
    }

    @SuppressWarnings("unchecked")
    static RetainIdPairs of(Tuple2<?, ?> ... tuples) {
        return new RetainIdPairs(
                IdPairs.of(
                        Arrays.asList((Tuple2<Object, Object>[]) tuples)
                )
        );
    }

    @Override
    public Collection<Tuple2<Object, Object>> tuples() {
        return raw.tuples();
    }

    @Override
    public Collection<Tuple2<Object, Collection<Object>>> entries() {
        return raw.entries();
    }

    @Override
    public boolean isEmpty() {
        return raw.isEmpty();
    }
}
