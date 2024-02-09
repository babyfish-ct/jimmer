package org.babyfish.jimmer.example.kt.core.model

import org.babyfish.jimmer.Immutable
import java.time.LocalDateTime
import javax.validation.constraints.Size

@Immutable
interface Author {

    @get:Size(max = 50)
    val name: String

    val lastModifiedTime: LocalDateTime

    val books: List<Book>
}