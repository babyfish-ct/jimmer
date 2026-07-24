package org.babyfish.jimmer.sql.kt.model.flat

import org.babyfish.jimmer.sql.DatabaseValidationIgnore
import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.Id
import org.babyfish.jimmer.sql.ManyToOne
import org.babyfish.jimmer.sql.Table

@Entity
@Table(name = "FLAT_PROVINCE")
@DatabaseValidationIgnore
interface Province {

    @Id
    val id: Long

    val provinceName: String

    @ManyToOne
    val country: Country
}