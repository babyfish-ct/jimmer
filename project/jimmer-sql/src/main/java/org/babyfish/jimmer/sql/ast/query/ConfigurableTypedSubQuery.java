package org.babyfish.jimmer.sql.ast.query;

import org.babyfish.jimmer.lang.NewChain;

public interface ConfigurableTypedSubQuery<R> extends TypedSubQuery<R> {

    @NewChain
    ConfigurableTypedSubQuery<R> limit(int limit, int offset);

    @NewChain
    ConfigurableTypedSubQuery<R> distinct();
}
