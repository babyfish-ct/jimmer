package org.babyfish.jimmer.sql.ast.query;

import org.babyfish.jimmer.sql.ast.query.selectable.RootSelectable;
import org.babyfish.jimmer.sql.ast.table.Table;

import java.util.function.BiFunction;

public interface ConfigurableTypedRootQuery<T extends Table<?>, R> extends TypedRootQuery<R> {

    <X> ConfigurableTypedRootQuery<T, X> reselect(
            BiFunction<MutableRootQuery<T>, T, ConfigurableTypedRootQuery<T, X>> block
    );

    ConfigurableTypedRootQuery<T, R> distinct();

    ConfigurableTypedRootQuery<T, R> limit(int limit, int offset);

    ConfigurableTypedRootQuery<T, R> withoutSortingAndPaging();

    ConfigurableTypedRootQuery<T, R> forUpdate();
}
