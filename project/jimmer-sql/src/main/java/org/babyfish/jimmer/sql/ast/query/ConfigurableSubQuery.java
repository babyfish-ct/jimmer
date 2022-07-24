package org.babyfish.jimmer.sql.ast.query;

import org.babyfish.jimmer.lang.NewChain;

public interface ConfigurableSubQuery<R> extends TypedSubQuery<R> {

    @NewChain
    ConfigurableSubQuery<R> limit(int limit, int offset);

    @NewChain
    ConfigurableSubQuery<R> distinct();
}
