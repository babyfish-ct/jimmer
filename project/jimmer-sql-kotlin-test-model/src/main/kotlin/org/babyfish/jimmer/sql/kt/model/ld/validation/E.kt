package org.babyfish.jimmer.sql.kt.model.ld.validation

import org.babyfish.jimmer.sql.*

@Entity
@DatabaseValidationIgnore
interface E {

    @Id
    val id: Long

    @Default("NEW")
    @LogicalDeleted("DELETED")
    val state: State
}