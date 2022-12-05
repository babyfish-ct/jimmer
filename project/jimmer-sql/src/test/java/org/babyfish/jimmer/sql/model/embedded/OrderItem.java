package org.babyfish.jimmer.sql.model.embedded;

import org.babyfish.jimmer.sql.*;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@Entity
public interface OrderItem {

    @Id
    @PropOverride(prop = "a", columnName = "ORDER_ITEM_A")
    @PropOverride(prop = "b", columnName = "ORDER_ITEM_B")
    @PropOverride(prop = "c", columnName = "ORDER_ITEM_C")
    OrderItemId id();

    String name();

    @ManyToOne
    @JoinColumn(name = "FK_ORDER_X", referencedColumnName = "ORDER_X")
    @JoinColumn(name = "FK_ORDER_Y", referencedColumnName = "ORDER_Y")
    @OnDissociate(DissociateAction.DELETE)
    @Nullable
    Order order();

    @ManyToMany
    @JoinTable(
            name = "ORDER_ITEM_PRODUCT_MAPPING",
            joinColumns = {
                    @JoinColumn(name = "FK_ORDER_ITEM_A", referencedColumnName = "ORDER_ITEM_A"),
                    @JoinColumn(name = "FK_ORDER_ITEM_B", referencedColumnName = "ORDER_ITEM_B"),
                    @JoinColumn(name = "FK_ORDER_ITEM_C", referencedColumnName = "ORDER_ITEM_C")
            },
            inverseColumns = {
                    @JoinColumn(name = "FK_PRODUCT_ALPHA", referencedColumnName = "PRODUCT_ALPHA"),
                    @JoinColumn(name = "FK_PRODUCT_BETA", referencedColumnName = "PRODUCT_BETA")
            }
    )
    List<Product> products();
}
