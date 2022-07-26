package org.babyfish.jimmer.sql;

import org.babyfish.jimmer.sql.ast.Executable;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface ListLoader<S, T> {

    @NotNull
    default List<T> load(@NotNull S source) {
        return loadCommand(source).execute();
    }

    @NotNull
    default List<T> load(@NotNull S source, int limit, int offset) {
        return loadCommand(source, limit, offset).execute();
    }

    @NotNull
    default Executable<List<T>> loadCommand(@NotNull S source) {
        return loadCommand(source, Integer.MAX_VALUE, 0);
    }

    @NotNull
    Executable<List<T>> loadCommand(@NotNull S source, int limit, int offset);

    @NotNull
    default Map<S, List<T>> batchLoad(@NotNull Collection<S> sources) {
        return batchLoadCommand(sources).execute();
    }

    @NotNull
    Executable<Map<S, List<T>>> batchLoadCommand(@NotNull Collection<S> sources);
}
