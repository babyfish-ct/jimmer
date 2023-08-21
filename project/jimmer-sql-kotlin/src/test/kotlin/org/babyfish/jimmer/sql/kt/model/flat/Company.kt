package org.babyfish.jimmer.sql.kt.model.flat

import org.babyfish.jimmer.sql.DatabaseValidationIgnore
import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.Id
import org.babyfish.jimmer.sql.ManyToOne
import org.babyfish.jimmer.sql.Table

@Entity
@Table(name = "FLAT_COMPANY")
@DatabaseValidationIgnore
interface Company {

    @Id
    val id: Long

    val companyName: String

    @ManyToOne
    val street: Street

    val value: Int
}