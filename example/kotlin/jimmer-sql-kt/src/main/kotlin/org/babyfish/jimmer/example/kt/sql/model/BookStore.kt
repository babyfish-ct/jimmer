package org.babyfish.jimmer.example.kt.sql.model

import org.babyfish.jimmer.sql.*

@Entity
interface BookStore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long

    @Key
    val name: String
    
    val website: String?

    @OneToMany(mappedBy = "store")
    val books: List<Book>
}