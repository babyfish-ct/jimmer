package org.babyfish.jimmer.sql.ast.impl;

import org.babyfish.jimmer.sql.ast.Expression;

public interface ExpressionSpi<T> extends Expression<T> {

    Class<T> getType();
}
