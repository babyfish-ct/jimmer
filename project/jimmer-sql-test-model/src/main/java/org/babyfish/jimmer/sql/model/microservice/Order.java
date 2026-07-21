package org.babyfish.jimmer.sql.model.microservice;

import org.babyfish.jimmer.sql.Entity;
import org.babyfish.jimmer.sql.Id;
import org.babyfish.jimmer.sql.OneToMany;
import org.babyfish.jimmer.sql.Table;

import java.util.List;

@Entity(microServiceName = "order-service")
@Table(name = "MS_ORDER")
public interface Order {

    @Id
    long id();

    String name();

    @OneToMany(mappedBy = "order")
    List<OrderItem> orderItems();
}