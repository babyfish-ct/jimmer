package org.babyfish.jimmer.sql.loader.graphql;

import org.babyfish.jimmer.lang.NewChain;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.fetcher.FieldFilter;

import java.sql.Connection;

public interface FilterableListLoader<SE, TE, TT extends Table<TE>> extends ListLoader<SE, TE> {

    @Override
    @NewChain
    FilterableListLoader<SE, TE, TT> forConnection(Connection con);

    @NewChain
    FilterableListLoader<SE, TE, TT> forFilter(FieldFilter<TT> filter);
}
