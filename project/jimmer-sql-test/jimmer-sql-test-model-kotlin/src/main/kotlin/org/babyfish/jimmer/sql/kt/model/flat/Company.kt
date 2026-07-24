package org.babyfish.jimmer.sql.kt.model.flat

import org.babyfish.jimmer.sql.DatabaseValidationIgnore
import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.Id
import org.babyfish.jimmer.sql.Key
import org.babyfish.jimmer.sql.ManyToOne
import org.babyfish.jimmer.sql.Table

@Entity
@Table(name = "FLAT_COMPANY")
@DatabaseValidationIgnore
interface Company {

    @Id
    val id: Long

    @Key
    val companyName: String

    @Key
    @Key(group = "2")
    @ManyToOne
    val street: Street

    @Key(group = "2")
    val value: Int
}