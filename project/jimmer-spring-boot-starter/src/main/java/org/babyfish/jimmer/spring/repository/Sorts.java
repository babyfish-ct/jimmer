package org.babyfish.jimmer.spring.repository;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.TypedProp;
import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.query.Order;
import org.babyfish.jimmer.sql.ast.table.Props;
import org.springframework.data.domain.Sort;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Sorts {

    private static final TypedProp.Scalar<?, ?>[] EMPTY_PROPS = new TypedProp.Scalar<?, ?>[0];

    private static final Order[] EMPTY_ORDERS = new Order[0];

    private Sorts() {}

    public static TypedProp.Scalar<?, ?>[] toTypedProps(Class<?> type, Sort sort) {
        ImmutableType immutableType = ImmutableType.get(type);
        List<TypedProp.Scalar<?, ?>> props = new ArrayList<>();
        for (Sort.Order order : sort) {
            TypedProp.Scalar<?, ?> prop = TypedProp.scalar(immutableType.getProp(order.getProperty()));
            props.add(order.isDescending() ? prop.desc() : prop);
        }
        return props.toArray(EMPTY_PROPS);
    }

    public static Order[] toOrders(Props table, Sort sort) {
        if (sort == null || sort.isEmpty()) {
            return EMPTY_ORDERS;
        }
        List<Order> astOrders = new ArrayList<>();
        for (Sort.Order order : sort) {
            Expression<?> expr = table.get(order.getProperty());
            Order astOrder = order.isDescending() ? expr.desc() : expr.asc();
            switch (order.getNullHandling()) {
                case NULLS_FIRST:
                    astOrder = astOrder.nullsFirst();
                    break;
                case NULLS_LAST:
                    astOrder = astOrder.nullsLast();
                    break;
            }
            astOrders.add(astOrder);
        }
        return astOrders.toArray(EMPTY_ORDERS);
    }

    public static Sort toSort(TypedProp.Scalar<?, ?> ... props) {
        List<Sort.Order> orders = new ArrayList<>();
        ImmutableType entityType = null;
        for (TypedProp.Scalar<?, ?> prop : props) {
            ImmutableProp ip = prop.unwrap();
            ImmutableType dt = prop.unwrap().getDeclaringType();
            if (dt.isEntity()) {
                if (entityType != null) {
                    throw new IllegalArgumentException("props do not belong to one entity type");
                }
                entityType = dt;
            }
            Sort.Order order = new Sort.Order(
                    prop.isDesc() ? Sort.Direction.DESC : Sort.Direction.ASC,
                    ip.getName(),
                    prop.isNullsFirst() ? Sort.NullHandling.NULLS_FIRST :
                            prop.isNullsLast() ? Sort.NullHandling.NULLS_LAST :
                                    Sort.NullHandling.NATIVE
            );
            orders.add(order);
        }
        return Sort.by(orders);
    }
}
