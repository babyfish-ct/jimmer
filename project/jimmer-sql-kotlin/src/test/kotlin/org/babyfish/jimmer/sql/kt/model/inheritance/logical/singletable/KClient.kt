package org.babyfish.jimmer.sql.kt.model.inheritance.logical.singletable

import org.babyfish.jimmer.sql.DiscriminatorColumn
import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.Id
import org.babyfish.jimmer.sql.Inheritance
import org.babyfish.jimmer.sql.InheritanceType
import org.babyfish.jimmer.sql.LogicalDeleted
import org.babyfish.jimmer.sql.Table

@Entity
@Table(name = "LOGICAL_CLIENT")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "CLIENT_TYPE")
interface KClient {

    @Id
    val id: Long

    val name: String

    @LogicalDeleted("true")
    val deleted: Boolean
}
