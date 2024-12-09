package org.babyfish.jimmer.kt.model

import com.fasterxml.jackson.annotation.JsonFormat
import org.babyfish.jimmer.Immutable
import java.math.BigDecimal
import java.time.LocalDateTime
import javax.validation.constraints.NotBlank
import javax.validation.constraints.Positive
import javax.validation.constraints.Size

@Immutable
interface Book {

    @get:NotBlank
    @get:Size(min = 1, max = 50)
    val name: String

    val edition: Int

    @get:Positive
    val price: PriceType

    @get:JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    val createdTime: LocalDateTime

    val store: BookStore?

    val authors: List<Author>
}

typealias PriceType = BigDecimal