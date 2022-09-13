package org.babyfish.jimmer.sql.kt.model

import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.Id
import org.babyfish.jimmer.sql.Key
import org.babyfish.jimmer.sql.ManyToMany
import org.babyfish.jimmer.sql.Transient
import javax.validation.constraints.NotBlank

@Entity
interface Author {

    @Id
    val id: Long

    @Key
    val firstName: @NotBlank String

    @Key
    val lastName: @NotBlank String

    val gender: Gender

    @ManyToMany(mappedBy = "authors")
    val books: List<Book>

    @Transient
    val organization: Organization
}