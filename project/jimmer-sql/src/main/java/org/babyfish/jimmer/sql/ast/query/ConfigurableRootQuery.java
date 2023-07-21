package org.babyfish.jimmer.sql.ast.query;

import org.babyfish.jimmer.lang.NewChain;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.util.function.BiFunction;

public interface ConfigurableRootQuery<T extends Table<?>, R> extends TypedRootQuery<R> {

    default int count() {
        return count(null);
    }

    default int count(Connection con) {
        return reselect((q, t) -> q.select(t.count()))
            .withoutSortingAndPaging()
            .execute(con)
            .get(0)
            .intValue();
    }

    default boolean exists() {
        return exists(null);
    }

    default boolean exists(Connection con) {
        return limit(1, 0).execute(con).size() != 0;
    }

    @NewChain
    <X> ConfigurableRootQuery<T, X> reselect(
            BiFunction<MutableRootQuery<T>, T, ConfigurableRootQuery<T, X>> block
    );

    @NewChain
    ConfigurableRootQuery<T, R> distinct();

    @NewChain
    default ConfigurableRootQuery<T, R> limit(int limit) {
        return limit(limit, null);
    }

    @NewChain
    default ConfigurableRootQuery<T, R> offset(int offset) {
        return limit(null, offset);
    }

    @NewChain
    ConfigurableRootQuery<T, R> limit(@Nullable Integer limit, @Nullable Integer offset);

    @NewChain
    ConfigurableRootQuery<T, R> withoutSortingAndPaging();

    /**
     * @return If the original query does not have `order by` clause, returns null
     */
    @NewChain
    @Nullable
    ConfigurableRootQuery<T, R> reverseSorting();

    @NewChain
    default ConfigurableRootQuery<T, R> forUpdate() {
        return forUpdate(true);
    }

    @NewChain
    ConfigurableRootQuery<T, R> forUpdate(boolean forUpdate);
}
