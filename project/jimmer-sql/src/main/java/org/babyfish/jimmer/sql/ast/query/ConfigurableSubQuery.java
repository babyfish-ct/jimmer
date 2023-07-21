package org.babyfish.jimmer.sql.ast.query;

import org.babyfish.jimmer.lang.NewChain;
import org.jetbrains.annotations.Nullable;

public interface ConfigurableSubQuery<R> extends TypedSubQuery<R> {

    @NewChain
    default ConfigurableSubQuery<R> limit(int limit) {
        return limit(limit, null);
    }

    @NewChain
    default ConfigurableSubQuery<R> offset(int offset) {
        return limit(null, offset);
    }

    @NewChain
    ConfigurableSubQuery<R> limit(@Nullable Integer limit, @Nullable Integer offset);

    @NewChain
    ConfigurableSubQuery<R> distinct();
}
