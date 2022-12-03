package org.babyfish.jimmer.sql.model.embedded;

import org.babyfish.jimmer.sql.Entity;
import org.babyfish.jimmer.sql.Id;
import org.babyfish.jimmer.sql.ManyToMany;
import org.babyfish.jimmer.sql.PropOverride;

import java.util.List;

@Entity
public interface Product {

    @Id
    @PropOverride(prop = "alpha", columnName = "PRODUCT_ALPHA")
    @PropOverride(prop = "beta", columnName = "PRODUCT_BETA")
    ProductId id();

    String name();

    @ManyToMany(mappedBy = "products")
    List<OrderItem> orderItems();
}
