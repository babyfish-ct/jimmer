package org.babyfish.jimmer.sql.kt.model.embedded

import org.babyfish.jimmer.Formula
import org.babyfish.jimmer.sql.*

@Entity
interface Machine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long

    val detail: MachineDetail

    @Formula(dependencies = ["detail.factories"])
    val factoryCount: Int
        get() = detail.factories.size

    @Formula(dependencies = ["detail.factories"])
    val factoryNames: List<String>
        get() = detail.factories.keys.toList()
}

