package org.babyfish.jimmer.kt.model

import org.babyfish.jimmer.Immutable
import javax.validation.constraints.Pattern

@Immutable
interface BookStore {

    @get:Pattern(regexp = "[^\\d]+\\S+")
    val name: String

    val books: List<Book>
}