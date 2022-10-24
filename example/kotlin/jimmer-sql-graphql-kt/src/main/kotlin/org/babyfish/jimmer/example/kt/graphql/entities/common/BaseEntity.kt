package org.babyfish.jimmer.example.kt.graphql.entities.common

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