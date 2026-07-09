package org.babyfish.jimmer.sql.kt.model.generic

import org.babyfish.jimmer.sql.GeneratedValue
import org.babyfish.jimmer.sql.GenerationType
import org.babyfish.jimmer.sql.Id
import org.babyfish.jimmer.sql.MappedSuperclass

@MappedSuperclass
interface KBaseId {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long
}
