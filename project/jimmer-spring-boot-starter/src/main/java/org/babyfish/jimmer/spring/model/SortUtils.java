package org.babyfish.jimmer.spring.model;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.TypedProp;
import org.babyfish.jimmer.sql.ast.query.Order;
import org.babyfish.jimmer.sql.ast.query.OrderMode;
import org.springframework.data.domain.Sort;

import java.util.ArrayList;
import java.util.List;

public class SortUtils {

    private SortUtils() {}

    public static Sort toSort(String ... codes) {
        return Sort.by(
                Order.makeCustomOrders(
                        (path, orderMode, nullOrderMode) -> {
                            Sort.NullHandling nullHandling;
                            switch (nullOrderMode) {
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
                            return new Sort.Order(
                                    orderMode == OrderMode.DESC ? Sort.Direction.DESC : Sort.Direction.ASC,
                                    path,
                                    nullHandling
                            );
                        },
                        codes
                )
        );
    }

    public static Sort toSort(TypedProp.Scalar<?, ?> ... props) {
        List<Sort.Order> orders = new ArrayList<>();
        ImmutableType entityType = null;
        for (TypedProp.Scalar<?, ?> prop : props) {
            ImmutableProp ip = prop.unwrap();
            ImmutableType dt = prop.unwrap().getDeclaringType();
            if (dt.isEntity()) {
                if (entityType != null && entityType != dt) {
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
