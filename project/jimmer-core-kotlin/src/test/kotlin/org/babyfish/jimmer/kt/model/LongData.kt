package org.babyfish.jimmer.kt.model

import org.babyfish.jimmer.Immutable
import org.babyfish.jimmer.jackson.JsonConverter
import org.babyfish.jimmer.jackson.LongConverter
import org.babyfish.jimmer.jackson.LongListConverter

@Immutable
interface LongData {

    @JsonConverter(LongConverter::class)
    val nonNullValue: Long

    @JsonConverter(LongConverter::class)
    val nullableValue: Long?

    @JsonConverter(LongListConverter::class)
    val values: List<Long>
}