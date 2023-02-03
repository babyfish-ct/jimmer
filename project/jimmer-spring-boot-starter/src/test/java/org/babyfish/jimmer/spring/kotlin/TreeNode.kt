package org.babyfish.jimmer.spring.kotlin

import org.babyfish.jimmer.sql.*

@Entity
interface TreeNode {

    @Id
    @Column(name = "NODE_ID")
    val id: Long

    val name: String

    @ManyToOne
    val parent: TreeNode?

    @OneToMany(mappedBy = "parent")
    val childNodes: List<TreeNode>
}