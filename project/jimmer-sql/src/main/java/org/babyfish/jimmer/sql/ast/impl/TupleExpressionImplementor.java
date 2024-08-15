package org.babyfish.jimmer.sql.ast.impl;

import org.babyfish.jimmer.sql.ast.Selection;
import org.babyfish.jimmer.sql.ast.impl.render.AbstractSqlBuilder;
import org.jetbrains.annotations.NotNull;

public interface TupleExpressionImplementor<T extends TupleImplementor> extends ExpressionImplementor<T> {

    int size();

    Selection<?> get(int index);

    void renderTo(@NotNull AbstractSqlBuilder<?> builder, boolean ignoreBrackets);
}
