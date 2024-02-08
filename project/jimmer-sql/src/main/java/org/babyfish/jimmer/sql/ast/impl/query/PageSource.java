package org.babyfish.jimmer.sql.ast.impl.query;

import org.babyfish.jimmer.sql.ast.query.MutableRootQuery;
import org.babyfish.jimmer.sql.ast.query.Order;
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor;

import java.util.List;

public interface PageSource {

    int getPageIndex();

    int getPageSize();

    List<Order> getOrders();

    JSqlClientImplementor getSqlClient();

    static PageSource of(int pageIndex, int pageSize, AbstractMutableQueryImpl query) {
        return new PageSource() {
            @Override
            public int getPageIndex() {
                return pageIndex;
            }

            @Override
            public int getPageSize() {
                return pageSize;
            }

            @Override
            public List<Order> getOrders() {
                return query.getOrders();
            }

            @Override
            public JSqlClientImplementor getSqlClient() {
                return query.getSqlClient();
            }
        };
    }
}
