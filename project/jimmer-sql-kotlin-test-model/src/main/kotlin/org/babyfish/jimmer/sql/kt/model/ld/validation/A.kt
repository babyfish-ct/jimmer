package org.babyfish.jimmer.sql.kt.model.ld.validation

import org.babyfish.jimmer.sql.DatabaseValidationIgnore
import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.Id
import org.babyfish.jimmer.sql.LogicalDeleted

@Entity
@DatabaseValidationIgnore
interface A {

    @Id
    val id: Long

    @LogicalDeleted("true")
    val deleted: Boolean
}