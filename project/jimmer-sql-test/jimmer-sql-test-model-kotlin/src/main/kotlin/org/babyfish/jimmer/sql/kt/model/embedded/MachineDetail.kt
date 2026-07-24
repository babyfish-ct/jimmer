package org.babyfish.jimmer.sql.kt.model.embedded

import org.babyfish.jimmer.sql.Column
import org.babyfish.jimmer.sql.Embeddable
import org.babyfish.jimmer.sql.Serialized

@Embeddable
interface MachineDetail {

    @Column(name = "factory_map")
    @Serialized
    val factories: Map<String, String>

    @Column(name = "patent_map")
    @Serialized
    val patents: Map<String, String>
}