package org.babyfish.jimmer.sql.kt.model.flat

import org.babyfish.jimmer.sql.DatabaseValidationIgnore
import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.Id
import org.babyfish.jimmer.sql.ManyToOne
import org.babyfish.jimmer.sql.Table

@Entity
@Table(name = "FLAT_STREET")
@DatabaseValidationIgnore
interface Street {

    @Id
    val id: Long

    val streetName: String

    @ManyToOne
    val city: City
}