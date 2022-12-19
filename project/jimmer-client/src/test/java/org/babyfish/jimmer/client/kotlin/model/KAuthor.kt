package org.babyfish.jimmer.client.kotlin.model

import org.babyfish.jimmer.client.Doc
import org.babyfish.jimmer.client.java.model.Gender
import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.Id
import org.babyfish.jimmer.sql.ManyToMany

@Entity
interface KAuthor {

    @Id
    val id: Long

    val firstName: String

    val lastName: String

    val gender: Gender?

    @Doc("All the books i have written")
    @ManyToMany(mappedBy = "authors")
    val books: List<KBook>
}
