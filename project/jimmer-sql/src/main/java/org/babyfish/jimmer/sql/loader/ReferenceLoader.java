package org.babyfish.jimmer.sql.loader;

import org.babyfish.jimmer.lang.NewChain;
import org.babyfish.jimmer.sql.ast.Executable;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.fetcher.FieldFilter;

import java.sql.Connection;
import java.util.Collection;
import java.util.Map;

public interface ReferenceLoader<SE, TE, TT extends Table<TE>> {

    @NewChain
    ReferenceLoader<SE, TE, TT> forConnection(Connection con);

    @NewChain
    ReferenceLoader<SE, TE, TT> forFilter(FieldFilter<TT> filter);

    default TE load(SE source) {
        return loadCommand(source).execute();
    }

    Executable<TE> loadCommand(SE source);

    default Map<SE, TE> batchLoad(Collection<SE> sources) {
        return batchLoadCommand(sources).execute();
    }

    Executable<Map<SE, TE>> batchLoadCommand(Collection<SE> sources);
}
