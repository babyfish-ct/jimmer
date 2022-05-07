package org.babyfish.jimmer.sql.ast.query;

import org.babyfish.jimmer.sql.ast.query.selectable.RootSelectable;

import java.util.function.Function;

public interface ConfigurableTypedRootQuery<R> extends TypedRootQuery<R> {

    <X> ConfigurableTypedRootQuery<X> reselect(
            Function<RootSelectable, ConfigurableTypedRootQuery<X>> block
    );

    ConfigurableTypedRootQuery<R> distinct();

    ConfigurableTypedRootQuery<R> limit(int limit, int offset);

    ConfigurableTypedRootQuery<R> withoutSortingAndPaging();

    ConfigurableTypedRootQuery<R> forUpdate();
}
