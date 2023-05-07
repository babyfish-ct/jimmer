package org.babyfish.jimmer.sql.example.model.common

import com.fasterxml.jackson.annotation.JsonFormat
import org.babyfish.jimmer.sql.MappedSuperclass
import java.time.LocalDateTime

/*
 * see CommonEntityDraftInterceptor
 */
@MappedSuperclass
interface BaseEntity {

    @get:JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    val createdTime: LocalDateTime

    @get:JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    val modifiedTime: LocalDateTime
}