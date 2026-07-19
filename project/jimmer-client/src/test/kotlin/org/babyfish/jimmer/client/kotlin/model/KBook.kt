package org.babyfish.jimmer.client.kotlin.model

import org.babyfish.jimmer.client.kotlin.KBaseEntityWithId
import org.babyfish.jimmer.jackson.JsonConverter
import org.babyfish.jimmer.jackson.LongListToStringListConverter
import org.babyfish.jimmer.jackson.LongToStringConverter
import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.IdView
import org.babyfish.jimmer.sql.ManyToMany
import org.babyfish.jimmer.sql.ManyToOne
import java.math.BigDecimal

/**
 * The book object
 *
 * @property price The price of this book
 * @property name The name of this book,
 *  <p>Together with `edition`, this property forms the key of the book</p>
 * @property edition The edition of this book,
 *  <p>Together with `name`, this property forms the key of the book</p>
 */
@Entity
interface KBook : KBaseEntityWithId {

    /**
     * The name of this book,
     * <p>Together with `edition`, this property forms the key of the book</p>
     */
    val name: String?

    /**
     *
     */
    val edition: Int

    val price: BigDecimal?

    /**
     * The bookstore to which the current book belongs, null is allowed
     */
    @ManyToOne
    val store: KBookStore?

    /**
     * All authors involved in writing the work
     */
    @ManyToMany
    val authors: List<KAuthor>

    /**
     * The id view of `Book.store`
     */
    @IdView
    val storeId: Long?

    /**
     * The id view of `Book.authors`
     */
    @IdView("authors")
    val authorIds: List<Long>
}
