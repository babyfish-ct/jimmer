package org.babyfish.jimmer.client.kotlin.model

import org.babyfish.jimmer.sql.MappedSuperclass
import java.time.LocalDateTime

@MappedSuperclass
interface KBaseEntity {

    /**
     * Created time
     */
    val createdTime: LocalDateTime

    /**
     * Modified time
     */
    val modifiedTime: LocalDateTime
}