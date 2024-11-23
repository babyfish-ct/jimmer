package org.babyfish.jimmer.sql.kt.model.embedded

import org.babyfish.jimmer.Formula
import org.babyfish.jimmer.sql.*

@Entity
interface Machine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long

    /**
     * Embedded property
     */
    val location: Location

    val detail: MachineDetail

    @Formula(dependencies = ["detail.factories"])
    val factoryCount: Int
        get() = detail.factories.size

    @Formula(dependencies = ["detail.factories"])
    val factoryNames: Set<String>
        get() = detail.factories.keys.toSet()
}

