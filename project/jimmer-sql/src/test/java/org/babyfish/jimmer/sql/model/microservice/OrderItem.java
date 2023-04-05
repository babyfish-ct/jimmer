package org.babyfish.jimmer.sql.model.microservice;

import org.babyfish.jimmer.sql.Entity;
import org.babyfish.jimmer.sql.Id;
import org.babyfish.jimmer.sql.ManyToOne;
import org.babyfish.jimmer.sql.Table;

@Entity(microServiceName = "item-service")
@Table(name = "MS_ORDER_ITEM")
public interface OrderItem {

    @Id
    long id();

    String name();

    @ManyToOne
    Order order();
}
