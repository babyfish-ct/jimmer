package org.babyfish.jimmer.sql.kt.model.selfref

import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.Id
import org.babyfish.jimmer.sql.IdView
import org.babyfish.jimmer.sql.JoinColumn
import org.babyfish.jimmer.sql.ManyToOne
import org.babyfish.jimmer.sql.OneToMany

@Entity
interface SelfReferenceNode {

    @Id
    val id: Long

    val name: String

    @IdView("parent")
    val parentId: Long?

    @ManyToOne
    @JoinColumn(name = "PARENT_ID")
    val parent: SelfReferenceNode?

    @OneToMany(mappedBy = "parent")
    val childNodes: List<SelfReferenceNode>
}
