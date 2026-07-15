package org.babyfish.jimmer.kt.model

import org.babyfish.jimmer.Immutable
import org.babyfish.jimmer.json.JsonConverter
import org.babyfish.jimmer.json.LongToStringConverter
import org.babyfish.jimmer.json.LongListToStringListConverter

@Immutable
interface LongData {

    @JsonConverter(LongToStringConverter::class)
    val nonNullValue: Long

    @JsonConverter(LongToStringConverter::class)
    val nullableValue: Long?

    @JsonConverter(LongListToStringListConverter::class)
    val values: List<Long>
}
