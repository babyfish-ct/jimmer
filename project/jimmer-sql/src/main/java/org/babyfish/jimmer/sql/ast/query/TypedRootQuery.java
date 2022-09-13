package org.babyfish.jimmer.sql.ast.query;

import org.babyfish.jimmer.sql.ast.Executable;
import org.babyfish.jimmer.sql.runtime.ExecutionException;
import org.jetbrains.annotations.Nullable;

import javax.validation.constraints.NotNull;
import java.sql.Connection;
import java.util.List;

public interface TypedRootQuery<R> extends Executable<List<R>> {

    TypedRootQuery<R> union(TypedRootQuery<R> other);

    TypedRootQuery<R> unionAll(TypedRootQuery<R> other);

    TypedRootQuery<R> minus(TypedRootQuery<R> other);

    TypedRootQuery<R> intersect(TypedRootQuery<R> other);

    @NotNull
    default R fetchOne() {
        return fetchOne(null);
    }

    @NotNull
    default R fetchOne(Connection con) {
        List<R> list = execute(con);
        if (list.isEmpty()) {
            throw new ExecutionException("No data is returned");
        }
        if (list.size() > 1) {
            throw new ExecutionException("Too much data is returned");
        }
        return list.get(0);
    }

    @Nullable
    default R fetchOneOrNull() {
        return fetchOneOrNull(null);
    }

    @Nullable
    default R fetchOneOrNull(Connection con) {
        List<R> list = execute(con);
        if (list.isEmpty()) {
            return null;
        }
        if (list.size() > 1) {
            throw new ExecutionException("Too much data is returned");
        }
        return list.get(0);
    }
}
