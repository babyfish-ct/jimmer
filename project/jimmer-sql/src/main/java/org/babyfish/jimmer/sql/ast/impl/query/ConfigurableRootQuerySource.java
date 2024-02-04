package org.babyfish.jimmer.sql.ast.impl.query;

import org.babyfish.jimmer.sql.ast.query.Order;
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor;

import java.util.List;

public interface ConfigurableRootQuerySource {

    List<Order> getOrders();

    int getLimit();

    JSqlClientImplementor getSqlClient();
}
