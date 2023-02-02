package org.babyfish.jimmer.sql.kt.model.inheritance

import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.GeneratedValue
import org.babyfish.jimmer.sql.GenerationType
import org.babyfish.jimmer.sql.Id

@Entity
interface Administrator : AdministratorBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long
}