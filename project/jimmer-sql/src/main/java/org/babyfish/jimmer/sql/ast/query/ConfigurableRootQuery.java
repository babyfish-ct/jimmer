package org.babyfish.jimmer.sql.ast.query;

import org.babyfish.jimmer.lang.NewChain;
import org.babyfish.jimmer.sql.ast.table.Table;

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

    @NewChain
    <X> ConfigurableRootQuery<T, X> reselect(
            BiFunction<MutableRootQuery<T>, T, ConfigurableRootQuery<T, X>> block
    );

    @NewChain
    ConfigurableRootQuery<T, R> distinct();

    @NewChain
    ConfigurableRootQuery<T, R> limit(int limit, int offset);

    @NewChain
    ConfigurableRootQuery<T, R> withoutSortingAndPaging();

    @NewChain
    default ConfigurableRootQuery<T, R> forUpdate() {
        return forUpdate(true);
    }

    @NewChain
    ConfigurableRootQuery<T, R> forUpdate(boolean forUpdate);
}
