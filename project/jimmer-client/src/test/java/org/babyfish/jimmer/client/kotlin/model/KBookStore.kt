package org.babyfish.jimmer.client.kotlin.model

import org.babyfish.jimmer.client.Doc
import org.babyfish.jimmer.client.java.model.Book
import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.Id
import org.babyfish.jimmer.sql.OneToMany

@Entity
interface KBookStore {

    @Id
    val id: Long

    val name: String?

    @Doc("All books available in this bookstore")
    @OneToMany(mappedBy = "store")
    val books: List<KBook>
}