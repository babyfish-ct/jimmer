package org.babyfish.jimmer.sql;

import org.babyfish.jimmer.sql.ast.Executable;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Map;

public interface ReferenceLoader<S, T> {

    @NotNull
    default T load(@NotNull S source) {
        return loadCommand(source).execute();
    }

    @NotNull
    Executable<T> loadCommand(@NotNull S source);

    @NotNull
    default Map<S, T> batchLoad(@NotNull Collection<S> sources) {
        return batchLoadCommand(sources).execute();
    }

    @NotNull
    Executable<Map<S, T>> batchLoadCommand(@NotNull Collection<S> sources);
}
