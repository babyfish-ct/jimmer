package org.babyfish.jimmer.sql.kt.model.microservice

import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.Id
import org.babyfish.jimmer.sql.OneToMany
import org.babyfish.jimmer.sql.Table

@Entity(microServiceName = "order-service")
@Table(name = "MS_ORDER")
interface Order {

    @Id
    val id: Long

    val name: String

    @OneToMany(mappedBy = "order")
    val orderItems: List<OrderItem>
}