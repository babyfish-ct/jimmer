package org.babyfish.jimmer.example.cloud.kt.model

import org.babyfish.jimmer.example.cloud.kt.model.common.BaseEntity
import org.babyfish.jimmer.sql.*

@Entity(microServiceName = "store-service")
interface BookStore : BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long

    @Key
    val name: String

    val website: String?

    @OneToMany(mappedBy = "store")
    val books: List<Book>
}
