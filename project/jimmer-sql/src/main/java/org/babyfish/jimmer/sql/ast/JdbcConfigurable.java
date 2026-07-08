package org.babyfish.jimmer.sql.ast;

import org.babyfish.jimmer.lang.OldChain;
import org.jetbrains.annotations.Nullable;

public interface JdbcConfigurable<S extends JdbcConfigurable<S>> {

    @OldChain
    S jdbcFetchSize(@Nullable Integer fetchSize);

    @OldChain
    S jdbcQueryTimeout(@Nullable Integer queryTimeout);
}
