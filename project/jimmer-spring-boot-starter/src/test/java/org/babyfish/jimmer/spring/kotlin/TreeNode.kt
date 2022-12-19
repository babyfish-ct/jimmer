package org.babyfish.jimmer.spring.kotlin

import org.babyfish.jimmer.sql.Column
import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.Id
import org.babyfish.jimmer.sql.ManyToOne

@Entity
interface TreeNode {

    @Id
    @Column(name = "NODE_ID")
    val id: Long

    val name: String

    @ManyToOne
    val parent: TreeNode?
}