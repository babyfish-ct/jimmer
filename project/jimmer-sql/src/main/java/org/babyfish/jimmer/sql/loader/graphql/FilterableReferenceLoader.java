package org.babyfish.jimmer.sql.loader.graphql;

import org.babyfish.jimmer.lang.NewChain;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.fetcher.FieldFilter;

import java.sql.Connection;

public interface FilterableReferenceLoader<SE, TE, TT extends Table<TE>> extends ReferenceLoader<SE, TE> {

    @Override
    @NewChain
    FilterableReferenceLoader<SE, TE, TT> forConnection(Connection con);

    @NewChain
    FilterableReferenceLoader<SE, TE, TT> forFilter(FieldFilter<TT> filter);
}
