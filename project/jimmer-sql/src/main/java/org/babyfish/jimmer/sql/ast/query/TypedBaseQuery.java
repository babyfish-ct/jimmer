package org.babyfish.jimmer.sql.ast.query;

import org.babyfish.jimmer.sql.ast.impl.query.MergedBaseQueryImpl;
import org.babyfish.jimmer.sql.ast.impl.query.RecursiveBaseQueryCreator;
import org.babyfish.jimmer.sql.ast.table.BaseTable;
import org.babyfish.jimmer.sql.ast.table.RecursiveRef;

public interface TypedBaseQuery<T extends BaseTable> {

    @SafeVarargs
    static <T extends BaseTable> TypedBaseQuery<T> union(TypedBaseQuery<T> ... queries) {
        return MergedBaseQueryImpl.of("union", queries);
    }

    @SafeVarargs
    static <T extends BaseTable> TypedBaseQuery<T> unionAll(TypedBaseQuery<T> ... queries) {
        return MergedBaseQueryImpl.of("union all", queries);
    }

    @SafeVarargs
    static <T extends BaseTable> TypedBaseQuery<T> unionAllRecursively(
            TypedBaseQuery<T> query,
            RecursiveBaseQueryCreator<T>... recursiveBaseQueryCreators
    ) {
        return MergedBaseQueryImpl.of(query, recursiveBaseQueryCreators);
    }

    @SafeVarargs
    static <T extends BaseTable> TypedBaseQuery<T> minus(TypedBaseQuery<T> ... queries) {
        return MergedBaseQueryImpl.of("minus", queries);
    }

    @SafeVarargs
    static <T extends BaseTable> TypedBaseQuery<T> intersect(TypedBaseQuery<T> ... queries) {
        return MergedBaseQueryImpl.of("intersect", queries);
    }

    T asBaseTable();

    T asCteBaseTable();
}
