package org.babyfish.jimmer.sql.ast.impl;

import org.babyfish.jimmer.meta.ImmutableProp;

public interface LiteralExpressionImplementor<T> {

    T getValue();

    /**
     * Retains the property context used to select its scalar provider when rendering this literal.
     */
    void bindProp(ImmutableProp prop);

    /**
     * Retains property contexts for tuple items; non-property items have null entries.
     */
    void bindProps(ImmutableProp[] props);
}
