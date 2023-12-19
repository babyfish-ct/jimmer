package org.babyfish.jimmer.kt.model

import org.babyfish.jimmer.Immutable
import org.babyfish.jimmer.jackson.JsonConverter
import org.babyfish.jimmer.jackson.LongToStringConverter
import org.babyfish.jimmer.jackson.LongListToStringListConverter

@Immutable
interface LongData {

    @JsonConverter(LongToStringConverter::class)
    val nonNullValue: Long

    @JsonConverter(LongToStringConverter::class)
    val nullableValue: Long?

    @JsonConverter(LongListToStringListConverter::class)
    val values: List<Long>
}