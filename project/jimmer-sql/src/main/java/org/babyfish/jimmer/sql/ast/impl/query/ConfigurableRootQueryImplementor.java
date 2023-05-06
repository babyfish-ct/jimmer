package org.babyfish.jimmer.sql.ast.impl.query;

import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.ast.query.ConfigurableRootQuery;
import org.babyfish.jimmer.sql.ast.query.Order;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor;

import java.util.List;

public interface ConfigurableRootQueryImplementor<T extends Table<?>, R>
        extends ConfigurableRootQuery<T, R>, TypedRootQueryImplementor<R> {

    List<Order> getOrders();

    JSqlClientImplementor getSqlClient();
}
