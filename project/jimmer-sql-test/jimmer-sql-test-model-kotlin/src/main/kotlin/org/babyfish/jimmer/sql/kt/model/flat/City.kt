package org.babyfish.jimmer.sql.kt.model.flat

import org.babyfish.jimmer.sql.DatabaseValidationIgnore
import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.Id
import org.babyfish.jimmer.sql.ManyToOne
import org.babyfish.jimmer.sql.Table

@Entity
@Table(name = "FLAT_CITY")
@DatabaseValidationIgnore
interface City {

    @Id
    val id: Long

    val cityName: String

    @ManyToOne
    val province: Province
}