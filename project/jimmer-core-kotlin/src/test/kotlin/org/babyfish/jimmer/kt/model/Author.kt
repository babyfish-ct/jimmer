package org.babyfish.jimmer.kt.model

import org.babyfish.jimmer.Immutable
import javax.validation.constraints.Email
import javax.validation.constraints.NotBlank
import javax.validation.constraints.Size

@Immutable
interface Author {

    @get:NotBlank
    @get:Size(min = 1, max = 50)
    val firstName: String

    @get:NotBlank
    @get:Size(min = 1, max = 50)
    val lastName: String

    val fullName: String
        get() = "$firstName $lastName"

    val gender: Gender

    @get:Email
    val email: String

    val books: List<Book>
}