package org.babyfish.jimmer.sql.kt.model.o2o

import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.GeneratedValue
import org.babyfish.jimmer.sql.GenerationType
import org.babyfish.jimmer.sql.Id
import org.babyfish.jimmer.sql.Key
import org.babyfish.jimmer.sql.KeyUniqueConstraint
import org.babyfish.jimmer.sql.OneToOne

@Entity
@KeyUniqueConstraint
interface Contact {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long

    val email: String

    val address: String

    @OneToOne
    @Key
    val customer: Customer
}