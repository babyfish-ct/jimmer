package org.babyfish.jimmer.sql.loader;

import org.babyfish.jimmer.lang.NewChain;
import org.babyfish.jimmer.sql.ast.Executable;

import java.sql.Connection;
import java.util.Collection;
import java.util.Map;

public interface ReferenceLoader<S, T> {

    @NewChain
    ReferenceLoader<S, T> forConnection(Connection con);

    default T load(S source) {
        return loadCommand(source).execute();
    }

    Executable<T> loadCommand(S source);

    default Map<S, T> batchLoad(Collection<S> sources) {
        return batchLoadCommand(sources).execute();
    }

    Executable<Map<S, T>> batchLoadCommand(Collection<S> sources);
}
