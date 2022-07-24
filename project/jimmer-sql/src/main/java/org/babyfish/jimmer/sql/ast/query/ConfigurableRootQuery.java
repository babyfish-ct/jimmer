package org.babyfish.jimmer.sql.ast.query;

import org.babyfish.jimmer.lang.NewChain;
import org.babyfish.jimmer.sql.ast.table.Table;

import java.util.function.BiFunction;

public interface ConfigurableRootQuery<T extends Table<?>, R> extends TypedRootQuery<R> {

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
    ConfigurableRootQuery<T, R> forUpdate();
}
