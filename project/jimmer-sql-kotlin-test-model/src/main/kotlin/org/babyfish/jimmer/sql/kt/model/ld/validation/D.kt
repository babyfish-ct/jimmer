package org.babyfish.jimmer.sql.kt.model.ld.validation

import org.babyfish.jimmer.sql.*

@Entity
@DatabaseValidationIgnore
interface D {

    @Id
    val id: Long

    @Default("1")
    @LogicalDeleted("2")
    val state: Int
}