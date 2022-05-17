package org.babyfish.jimmer.sql.ast.query;

import org.babyfish.jimmer.lang.NewChain;
import org.babyfish.jimmer.sql.ast.query.selectable.RootSelectable;
import org.babyfish.jimmer.sql.ast.table.Table;

import java.util.function.BiFunction;

public interface ConfigurableTypedRootQuery<T extends Table<?>, R> extends TypedRootQuery<R> {

    @NewChain
    <X> ConfigurableTypedRootQuery<T, X> reselect(
            BiFunction<MutableRootQuery<T>, T, ConfigurableTypedRootQuery<T, X>> block
    );

    @NewChain
    ConfigurableTypedRootQuery<T, R> distinct();

    @NewChain
    ConfigurableTypedRootQuery<T, R> limit(int limit, int offset);

    @NewChain
    ConfigurableTypedRootQuery<T, R> withoutSortingAndPaging();

    @NewChain
    ConfigurableTypedRootQuery<T, R> forUpdate();
}
