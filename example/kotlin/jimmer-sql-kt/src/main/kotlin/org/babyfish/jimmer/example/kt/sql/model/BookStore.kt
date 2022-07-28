package org.babyfish.jimmer.example.kt.sql.model

import org.babyfish.jimmer.sql.*
import org.babyfish.jimmer.sql.meta.UUIDIdGenerator
import java.util.*

@Entity
interface BookStore {

    @Id
    @GeneratedValue(strategy = GenerationType.USER, generatorType = UUIDIdGenerator::class)
    val id: UUID

    @Key
    val name: String
    
    val website: String?

    @OneToMany(mappedBy = "store")
    val books: List<Book>
}