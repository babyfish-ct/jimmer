package org.babyfish.jimmer.benchmark.exposed

// Cannot use exposed-dto
// Exposed.Entity uses property delegate so that values are not passed immediately.
data class ExposedData(
    val id: Long,
    val value1: Int,
    val value2: Int,
    val value3: Int,
    val value4: Int,
    val value5: Int,
    val value6: Int,
    val value7: Int,
    val value8: Int,
    val value9: Int
)