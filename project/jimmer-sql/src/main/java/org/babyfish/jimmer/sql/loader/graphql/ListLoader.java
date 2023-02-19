package org.babyfish.jimmer.sql.loader.graphql;

import org.babyfish.jimmer.lang.NewChain;
import org.babyfish.jimmer.sql.ast.Executable;

import java.sql.Connection;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface ListLoader<S, T> {

    @NewChain
    ListLoader<S, T> forConnection(Connection con);

    default List<T> load(S source) {
        return loadCommand(source).execute();
    }

    default List<T> load(S source, int limit, int offset) {
        return loadCommand(source, limit, offset).execute();
    }

    default Executable<List<T>> loadCommand(S source) {
        return loadCommand(source, Integer.MAX_VALUE, 0);
    }

    Executable<List<T>> loadCommand(S source, int limit, int offset);

    default Map<S, List<T>> batchLoad(Collection<S> sources) {
        return batchLoadCommand(sources).execute();
    }

    Executable<Map<S, List<T>>> batchLoadCommand(Collection<S> sources);
}
