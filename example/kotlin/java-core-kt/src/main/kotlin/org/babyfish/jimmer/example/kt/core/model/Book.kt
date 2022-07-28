package org.babyfish.jimmer.example.kt.core.model

import org.babyfish.jimmer.Immutable
import javax.validation.constraints.Size

@Immutable
interface Book {

    @get:Size(max = 50)
    val name: String

    val store: BookStore?

    val price: Int

    val authors: List<Author>
}