package org.babyfish.jimmer.client.kotlin

import org.babyfish.jimmer.client.kotlin.model.KBaseEntity
import org.babyfish.jimmer.jackson.JsonConverter
import org.babyfish.jimmer.jackson.LongToStringConverter
import org.babyfish.jimmer.sql.Id
import org.babyfish.jimmer.sql.MappedSuperclass

@MappedSuperclass
interface KBaseEntityWithId : KBaseEntity {

    /**
     * The id is long, but the client type is string
     * because JS cannot retain large long values
     */
    @Id
    @JsonConverter(LongToStringConverter::class)
    val id: Long
}