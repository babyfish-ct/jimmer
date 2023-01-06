package org.babyfish.jimmer.sql.ast.impl.query;

import org.babyfish.jimmer.sql.ast.query.ConfigurableRootQuery;
import org.babyfish.jimmer.sql.ast.query.Order;
import org.babyfish.jimmer.sql.ast.table.Table;

import java.util.List;

public interface ConfigurableRootQueryImplementor<T extends Table<?>, R>
        extends ConfigurableRootQuery<T, R>, TypedRootQueryImplementor<R> {

    List<Order> getOrders();
}
