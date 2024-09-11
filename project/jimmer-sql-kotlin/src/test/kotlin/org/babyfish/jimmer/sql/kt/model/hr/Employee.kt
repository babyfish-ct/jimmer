package org.babyfish.jimmer.sql.kt.model.hr

import org.babyfish.jimmer.jackson.JsonConverter
import org.babyfish.jimmer.jackson.LongToStringConverter
import org.babyfish.jimmer.sql.*
import java.util.*

@Entity
interface Employee {

    @Id
    @JsonConverter(LongToStringConverter::class)
    val id: Long

    val name: String

    @ManyToOne
    @OnDissociate(DissociateAction.DELETE)
    val department: Department?

    @LogicalDeleted
    val deletedUUID: UUID?
}