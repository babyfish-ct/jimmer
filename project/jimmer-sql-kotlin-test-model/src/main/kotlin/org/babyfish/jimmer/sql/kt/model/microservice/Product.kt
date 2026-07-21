package org.babyfish.jimmer.sql.kt.model.microservice

import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.Id
import org.babyfish.jimmer.sql.ManyToMany
import org.babyfish.jimmer.sql.Table

@Entity(microServiceName = "product-service")
@Table(name = "MS_PRODUCT")
interface Product {
    
    @Id
    val id: Long
    
    val name: String

    @ManyToMany(mappedBy = "products")
    val orderItems: List<OrderItem>
}