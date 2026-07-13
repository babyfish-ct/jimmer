package org.babyfish.jimmer.sql.kt.model.inheritance.joinedtable.inverse

import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.GeneratedValue
import org.babyfish.jimmer.sql.GenerationType
import org.babyfish.jimmer.sql.Id
import org.babyfish.jimmer.sql.OneToOne
import org.babyfish.jimmer.sql.Table

@Entity
@Table(name = "JOINED_CITIZEN")
interface KCitizen {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long

    val name: String

    @OneToOne(mappedBy = "citizen")
    val passport: KPassport?
}
