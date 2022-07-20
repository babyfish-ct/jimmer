package org.babyfish.jimmer.kt.model

import org.babyfish.jimmer.Immutable

@Immutable
interface BookStore {
    val name: String
    val books: List<Book>
}