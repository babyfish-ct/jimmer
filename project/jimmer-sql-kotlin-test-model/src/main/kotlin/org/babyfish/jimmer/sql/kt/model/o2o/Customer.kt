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
interface Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long

    @Key
    val name: String

    @OneToOne(mappedBy = "customer")
    val contact: Contact?
}