package org.babyfish.jimmer.kt.model

import org.babyfish.jimmer.Immutable

@Immutable
interface Author {
    val firstName: String
    val lastName: String
    val gender: Gender
    val books: List<Book>
}