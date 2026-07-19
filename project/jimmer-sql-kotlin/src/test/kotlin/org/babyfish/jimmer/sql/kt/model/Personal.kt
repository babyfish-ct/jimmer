package org.babyfish.jimmer.sql.kt.model

import org.babyfish.jimmer.jackson.JsonConverter
import org.babyfish.jimmer.jackson.LongToStringConverter
import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.Id

@Entity
interface Personal {
    @Id
    @JsonConverter(LongToStringConverter::class)
    val id: Long

    @JsonConverter(PersonalPhoneConverter::class)
    val phone: String
}