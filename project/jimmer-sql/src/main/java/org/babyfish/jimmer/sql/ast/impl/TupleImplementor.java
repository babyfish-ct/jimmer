package org.babyfish.jimmer.sql.ast.impl;

import java.util.function.BiFunction;

// Only for performance optimization
public interface TupleImplementor {

    int size();

    Object get(int index);

    TupleImplementor convert(BiFunction<Object, Integer, Object> block);
}
