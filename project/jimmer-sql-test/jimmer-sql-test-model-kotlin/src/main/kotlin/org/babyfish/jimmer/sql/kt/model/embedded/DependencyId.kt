package org.babyfish.jimmer.sql.kt.model.embedded

import org.babyfish.jimmer.sql.Embeddable

@Embeddable
interface DependencyId {
    val groupId: String
    val artifactId: String
}