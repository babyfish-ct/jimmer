package org.babyfish.jimmer.sql.loader.graphql;

import org.babyfish.jimmer.lang.NewChain;
import org.babyfish.jimmer.sql.ast.Executable;

import java.sql.Connection;
import java.util.Collection;
import java.util.Map;

public interface ValueLoader<S, V> {

    @NewChain
    ValueLoader<S, V> forConnection(Connection con);

    default V load(S source) {
        return loadCommand(source).execute();
    }

    Executable<V> loadCommand(S source);

    default Map<S, V> batchLoad(Collection<S> sources) {
        return batchLoadCommand(sources).execute();
    }

    Executable<Map<S, V>> batchLoadCommand(Collection<S> sources);
}
