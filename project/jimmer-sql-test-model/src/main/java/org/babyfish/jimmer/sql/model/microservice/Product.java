package org.babyfish.jimmer.sql.model.microservice;

import org.babyfish.jimmer.sql.Entity;
import org.babyfish.jimmer.sql.Id;
import org.babyfish.jimmer.sql.ManyToMany;
import org.babyfish.jimmer.sql.Table;

import java.util.List;

@Entity(microServiceName = "product-service")
@Table(name = "MS_PRODUCT")
public interface Product {

    @Id
    long id();

    String name();

    @ManyToMany(mappedBy = "products")
    List<OrderItem> orderItems();
}
