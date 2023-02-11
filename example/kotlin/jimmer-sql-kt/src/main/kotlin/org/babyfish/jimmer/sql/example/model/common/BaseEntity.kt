package org.babyfish.jimmer.sql.example.model.common

import org.babyfish.jimmer.sql.MappedSuperclass
import java.time.LocalDateTime

/*
 * see CommonEntityDraftInterceptor
 */
@MappedSuperclass
interface BaseEntity {

    val createdTime: LocalDateTime

    val modifiedTime: LocalDateTime
}