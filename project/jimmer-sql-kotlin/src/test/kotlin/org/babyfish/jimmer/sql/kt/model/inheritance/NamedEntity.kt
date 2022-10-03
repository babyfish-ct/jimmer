package org.babyfish.jimmer.sql.kt.model.inheritance

import org.babyfish.jimmer.sql.Key
import org.babyfish.jimmer.sql.MappedSuperclass
import java.time.LocalDateTime

@MappedSuperclass
interface NamedEntity {

    @Key
    val name: String

    val createdTime: LocalDateTime

    val modifiedTime: LocalDateTime
}