package org.babyfish.jimmer.sql.kt.model.inheritance

import org.babyfish.jimmer.json.JsonFormat
import org.babyfish.jimmer.sql.Key
import org.babyfish.jimmer.sql.LogicalDeleted
import org.babyfish.jimmer.sql.MappedSuperclass
import java.time.LocalDateTime

@MappedSuperclass
interface NamedEntity {

    @Key
    val name: String

    @LogicalDeleted("true")
    val deleted: Boolean

    @get:JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    val createdTime: LocalDateTime

    @get:JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    val modifiedTime: LocalDateTime
}