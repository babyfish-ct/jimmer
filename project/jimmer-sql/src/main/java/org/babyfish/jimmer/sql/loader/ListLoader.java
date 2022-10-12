package org.babyfish.jimmer.sql.loader;

import org.babyfish.jimmer.lang.NewChain;
import org.babyfish.jimmer.sql.ast.Executable;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.fetcher.FieldFilter;

import java.sql.Connection;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface ListLoader<SE, TE, TT extends Table<TE>> {

    @NewChain
    ListLoader<SE, TE, TT> forConnection(Connection con);

    @NewChain
    ListLoader<SE, TE, TT> forFilter(FieldFilter<TT> filter);

    default List<TE> load(SE source) {
        return loadCommand(source).execute();
    }

    default List<TE> load(SE source, int limit, int offset) {
        return loadCommand(source, limit, offset).execute();
    }

    default Executable<List<TE>> loadCommand(SE source) {
        return loadCommand(source, Integer.MAX_VALUE, 0);
    }

    Executable<List<TE>> loadCommand(SE source, int limit, int offset);

    default Map<SE, List<TE>> batchLoad(Collection<SE> sources) {
        return batchLoadCommand(sources).execute();
    }

    Executable<Map<SE, List<TE>>> batchLoadCommand(Collection<SE> sources);
}
