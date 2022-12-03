package org.babyfish.jimmer.sql.model.embedded;

import org.babyfish.jimmer.sql.*;

import java.util.List;

@Entity
@Table(name = "ORDER_")
public interface Order {

    @Id
    @PropOverride(prop = "x", columnName = "ORDER_X")
    @PropOverride(prop = "y", columnName = "ORDER_Y")
    OrderId id();

    String name();

    @OneToMany(mappedBy = "order")
    List<OrderItem> orderItems();
}
