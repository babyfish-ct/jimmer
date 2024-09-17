package org.babyfish.jimmer.sql.kt.model.embedded

import org.babyfish.jimmer.sql.Default
import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.Id

@Entity
interface Dependency {

    @Id
    val id: DependencyId

    val version: String

    @Default("COMPILE")
    val scope: DependencyScope
}


