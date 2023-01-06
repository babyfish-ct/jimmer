package org.babyfish.jimmer.spring.repository.support;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.spring.repository.SpringConnectionManager;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.ast.PropExpression;
import org.babyfish.jimmer.sql.ast.impl.table.TableImplementor;
import org.babyfish.jimmer.sql.ast.query.Order;
import org.babyfish.jimmer.sql.ast.query.OrderMode;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.ast.table.spi.PropExpressionImplementor;
import org.babyfish.jimmer.sql.ast.table.spi.TableProxy;
import org.babyfish.jimmer.sql.runtime.ConnectionManager;
import org.springframework.data.domain.Sort;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class Utils {

    private Utils() {}

    public static <E> Collection<E> toCollection(Iterable<E> iterable) {
        if (iterable instanceof Collection<?>) {
            return (Collection<E>) iterable;
        }
        if (iterable == null) {
            return Collections.emptyList();
        }
        List<E> list = new ArrayList<>();
        for (E e : iterable) {
            list.add(e);
        }
        return list;
    }

    public static void validateSqlClient(JSqlClient sqlClient) {
        if (!(sqlClient.getConnectionManager() instanceof SpringConnectionManager)) {
            throw new IllegalArgumentException(
                    "The connection manager of sql client must be instance of \"" +
                            SpringConnectionManager.class.getName() +
                            "\""
            );
        }
        ConnectionManager slaveConnectionManager = sqlClient.getSlaveConnectionManager(false);
        if (slaveConnectionManager != null && !(slaveConnectionManager instanceof SpringConnectionManager)) {
            throw new IllegalArgumentException(
                    "The slave connection manager of sql client must be null or instance of \"" +
                            SpringConnectionManager.class.getName() +
                            "\""
            );
        }
        if (sqlClient.getEntityManager() == null) {
            throw new IllegalArgumentException(
                    "The entity manager of of sql client must be specified, " +
                            "please specify it by the constant in the generated source code."
            );
        }
    }

    public static Sort toSort(List<Order> orders) {
        if (orders == null || orders.isEmpty()) {
            return Sort.unsorted();
        }
        List<Sort.Order> springOrders = new ArrayList<>(orders.size());
        for (Order order : orders) {
            if (order.getExpression() instanceof PropExpression<?>) {
                PropExpressionImplementor<?> propExpr = (PropExpressionImplementor<?>) order.getExpression();
                String prefix = prefix(propExpr.getTable());
                String path = propExpr.getPartial() != null ? propExpr.getPartial().path() : propExpr.getProp().getName();
                if (prefix != null) {
                    path = prefix + '.' + path;
                }
                Sort.NullHandling nullHandling;
                switch (order.getNullOrderMode()) {
                    case NULLS_FIRST:
                        nullHandling = Sort.NullHandling.NULLS_FIRST;
                        break;
                    case NULLS_LAST:
                        nullHandling = Sort.NullHandling.NULLS_LAST;
                        break;
                    default:
                        nullHandling = Sort.NullHandling.NATIVE;
                        break;
                }
                springOrders.add(
                        new Sort.Order(
                                order.getOrderMode() == OrderMode.DESC ?
                                        Sort.Direction.DESC :
                                        Sort.Direction.ASC,
                                path,
                                nullHandling
                        )
                );
            }
        }
        return Sort.by(springOrders);
    }

    private static String prefix(Table<?> table) {
        ImmutableProp prop = table instanceof TableProxy<?> ?
                ((TableProxy<?>) table).__prop() :
                ((TableImplementor<?>) table).getJoinProp();
        if (prop == null) {
            return null;
        }

        String name = prop.getName();

        boolean inverse = table instanceof TableProxy<?> ?
                ((TableProxy<?>) table).__isInverse() :
                ((TableImplementor<?>) table).isInverse();
        if (inverse) {
            name = "`←" + name + '`';
        }

        Table<?> parent = table instanceof TableProxy<?> ?
                ((TableProxy<?>) table).__parent() :
                ((TableImplementor<?>) table).getParent();
        if (parent == null) {
            return name;
        }
        String parentPrefix = prefix(parent);
        if (parentPrefix == null) {
            return name;
        }
        return parentPrefix + '.' + name;
    }
}
