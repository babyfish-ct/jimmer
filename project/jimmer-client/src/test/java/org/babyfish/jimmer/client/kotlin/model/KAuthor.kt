package org.babyfish.jimmer.client.kotlin.model

import org.babyfish.jimmer.jackson.JsonConverter
import org.babyfish.jimmer.jackson.LongToStringConverter
import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.Id
import org.babyfish.jimmer.sql.ManyToMany

/**
 * The author object
 *
 * @property firstName The first name of this author
 *  <p>Together with `lastName`, this property forms the key of the book</p>
 * @property lastName The last name of this author
 *  <p>Together with `firstName`, this property forms the key of the book</p>
 */
@Entity
interface KAuthor {

    /**
     * The id is long, but the client type is string
     * because JS cannot retain large long values
     */
    @Id
    @JsonConverter(LongToStringConverter::class)
    val id: Long

    val firstName: String

    val lastName: String

    val gender: KGender

    /**
     * All the books I have written
     */
    @ManyToMany(mappedBy = "authors")
    val books: List<KBook>
}
