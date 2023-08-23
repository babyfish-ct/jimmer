package org.babyfish.jimmer.sql.ast.query;

import org.babyfish.jimmer.lang.NewChain;

public interface ConfigurableSubQuery<R> extends TypedSubQuery<R> {

    @NewChain
    ConfigurableSubQuery<R> limit(int limit);

    @NewChain
    ConfigurableSubQuery<R> offset(long offset);

    @NewChain
    ConfigurableSubQuery<R> limit(int limit, long offset);

    @NewChain
    ConfigurableSubQuery<R> distinct();
}
