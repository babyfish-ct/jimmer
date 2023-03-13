package org.babyfish.jimmer.sql.ast.impl;

import org.babyfish.jimmer.sql.ast.Selection;

public interface TupleExpressionImplementor<T extends TupleImplementor> extends ExpressionImplementor<T> {

    int size();

    Selection<?> get(int index);
}
