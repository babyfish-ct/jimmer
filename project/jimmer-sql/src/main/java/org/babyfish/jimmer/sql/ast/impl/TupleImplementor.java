package org.babyfish.jimmer.sql.ast.impl;

import java.util.Collection;
import java.util.function.BiFunction;

// Only for performance optimization
public interface TupleImplementor {

    int size();

    Object get(int index);

    TupleImplementor convert(BiFunction<Object, Integer, Object> block);

    static Collection<Object> projection(Collection<? extends TupleImplementor> tuples, int index) {
        if (index < 0 || index >= tuples.size()) {
            throw new IllegalArgumentException("Index out of range");
        }
        return new TupleProjectionCollection(tuples, index);
    }
}
