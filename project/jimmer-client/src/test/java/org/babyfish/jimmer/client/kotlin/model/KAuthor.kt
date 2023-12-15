package org.babyfish.jimmer.client.kotlin.model

import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.Id
import org.babyfish.jimmer.sql.ManyToMany

@Entity
interface KAuthor {

    @Id
    val id: Long

    val firstName: String

    val lastName: String

    val gender: KGender?

    /**
     * All the books I have written
     */
    @ManyToMany(mappedBy = "authors")
    val books: List<KBook>
}
