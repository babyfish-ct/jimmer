package org.babyfish.jimmer.example.kt.sql.model.common

import org.babyfish.jimmer.pojo.AutoScalarRule
import org.babyfish.jimmer.pojo.AutoScalarStrategy
import org.babyfish.jimmer.sql.MappedSuperclass
import java.time.LocalDateTime

/*
 * see CommonEntityDraftInterceptor
 */
@MappedSuperclass
@AutoScalarRule(AutoScalarStrategy.NONE)
interface BaseEntity {

    val createdTime: LocalDateTime

    val modifiedTime: LocalDateTime
}