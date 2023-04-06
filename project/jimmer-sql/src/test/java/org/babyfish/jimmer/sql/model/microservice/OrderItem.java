package org.babyfish.jimmer.sql.model.microservice;

import org.babyfish.jimmer.sql.*;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@Entity(microServiceName = "order-item-service")
@Table(name = "MS_ORDER_ITEM")
public interface OrderItem {

    @Id
    long id();

    String name();

    @ManyToOne
    @Nullable
    Order order();

    @ManyToMany
    @JoinTable(
            name = "MS_ORDER_ITEM_PRODUCT_MAPPING",
            joinColumnName = "ORDER_ITEM_ID",
            inverseJoinColumnName = "PRODUCT_ID"
    )
    List<Product> products();
}
