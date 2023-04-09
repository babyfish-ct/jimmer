package org.babyfish.jimmer.sql.kt.model.microservice

import org.babyfish.jimmer.sql.*

@Entity(microServiceName = "order-item-service")
@Table(name = "MS_ORDER_ITEM")
interface OrderItem {
    
    @Id
    val id: Long

    val name: String

    @ManyToOne
    val order: Order?

    @ManyToMany
    @JoinTable(
        name = "MS_ORDER_ITEM_PRODUCT_MAPPING",
        joinColumnName = "ORDER_ITEM_ID",
        inverseJoinColumnName = "PRODUCT_ID"
    )
    val products: List<Product>
}
