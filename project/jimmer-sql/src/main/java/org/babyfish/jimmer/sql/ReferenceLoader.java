package org.babyfish.jimmer.sql;

import org.babyfish.jimmer.sql.ast.Executable;

import java.util.Collection;
import java.util.Map;

public interface ReferenceLoader<S, T> {

    default T load(S source) {
        return loadCommand(source).execute();
    }

    Executable<T> loadCommand(S source);

    default Map<S, T> batchLoad(Collection<S> sources) {
        return batchLoadCommand(sources).execute();
    }

    Executable<Map<S, T>> batchLoadCommand(Collection<S> sources);
}
