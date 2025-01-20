package org.babyfish.jimmer.sql.ast.query;

import org.babyfish.jimmer.lang.NewChain;
import org.jetbrains.annotations.Nullable;

public interface ConfigurableSubQuery<R> extends TypedSubQuery<R> {

    @NewChain
    ConfigurableSubQuery<R> limit(int limit);

    @NewChain
    ConfigurableSubQuery<R> offset(long offset);

    @NewChain
    ConfigurableSubQuery<R> limit(int limit, long offset);

    @NewChain
    ConfigurableSubQuery<R> distinct();

    /**
     * Set the hint
     * @param hint Optional hint, both <b>/&#42;+ sth &#42;/</b> and <b>sth</b> are OK.
     * @return A new query object
     */
    ConfigurableSubQuery<R> hint(@Nullable String hint);
}
