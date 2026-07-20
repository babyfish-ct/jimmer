package org.babyfish.jimmer.sql.kt.model.classic.author

import org.babyfish.jimmer.Formula
import org.babyfish.jimmer.sql.*
import org.babyfish.jimmer.sql.kt.model.classic.book.Book
import org.babyfish.jimmer.sql.kt.model.Organization
import javax.validation.constraints.NotBlank

/**
 * The Author entity
 */
@Entity
interface Author {

    /**
     * The id property
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long

    /**
     * The firstName property
     */
    @Key
    val firstName: @NotBlank String

    /**
     * The lastName property
     */
    @Key
    val lastName: @NotBlank String

    /**
     * The fullName property
     */
    @Formula(dependencies = ["firstName", "lastName"])
    val fullName: String
        get() = "$firstName $lastName"

    @Formula(sql = "length(%alias.FIRST_NAME) + length(%alias.LAST_NAME)")
    val fullNameLength: Int

    /**
     * The fullName2 property
     */
    @Formula(sql = "concat(%alias.FIRST_NAME, ' ', %alias.LAST_NAME)")
    val fullName2: String

    /**
     * The gender property
     */
    val gender: Gender

    /**
     * The books property
     */
    @ManyToMany(mappedBy = "authors")
    val books: List<Book>

    /**
     * The organization property
     */
    @Transient
    val organization: Organization

    fun unusedFun() {}
}