package org.babyfish.jimmer.sql.loader;

import org.babyfish.jimmer.lang.NewChain;
import org.babyfish.jimmer.sql.ast.Executable;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.fetcher.FieldFilter;

import java.sql.Connection;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface FilterableListLoader<SE, TE, TT extends Table<TE>> extends ListLoader<SE, TE> {

    @Override
    @NewChain
    FilterableListLoader<SE, TE, TT> forConnection(Connection con);

    @NewChain
    FilterableListLoader<SE, TE, TT> forFilter(FieldFilter<TT> filter);
}
