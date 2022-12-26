package org.babyfish.jimmer.spring.model;

import org.babyfish.jimmer.meta.TypedProp;
import org.springframework.data.domain.Sort;

import java.util.*;

public class Sorts {

    private Sorts() {}

    @SafeVarargs
    public static <S, T> Sort by(TypedProp.Scalar<S, T> ... props) {
        if (props == null || props.length == 0) {
            return Sort.unsorted();
        }
        List<Sort.Order> orders = new ArrayList<>(props.length);
        for (TypedProp.Scalar<S, T> prop : props) {
            Sort.Order order = new Sort.Order(
                    prop.isDesc() ? Sort.Direction.DESC : Sort.Direction.ASC,
                    prop.unwrap().getName(),
                    prop.isNullsFirst() ? Sort.NullHandling.NULLS_FIRST :
                            prop.isNullsLast() ? Sort.NullHandling.NULLS_LAST :
                                    Sort.NullHandling.NATIVE
            );
            orders.add(order);
        }
        return Sort.by(orders);
    }
}
