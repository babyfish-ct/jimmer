package org.babyfish.jimmer.sql.ast.impl;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;

// Only for performance optimization
public interface TupleImplementor {

    int size();

    Object get(int index);

    TupleImplementor convert(BiFunction<Object, Integer, Object> block);

    default int copyTo(Object[] arr, int fromIndex) {
        int size = this.size();
        for (int i = 0; i < size; i++) {
            arr[fromIndex + i] = get(i);
        }
        return size;
    }

    static Collection<Object> projection(Collection<? extends TupleImplementor> tuples, int index) {
        if (tuples.isEmpty()) {
            return Collections.emptyList();
        }
        TupleImplementor tupleImplementor = tuples instanceof List<?> ?
                ((List<? extends TupleImplementor>)tuples).get(0) :
                tuples.iterator().next();
        if (index < 0 || index >= tupleImplementor.size()) {
            throw new IllegalArgumentException("Index out of range");
        }
        return new TupleProjectionCollection(tuples, index);
    }
}
