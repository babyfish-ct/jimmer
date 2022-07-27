package org.babyfish.jimmer.sql.kt.model

import org.babyfish.jimmer.sql.*

@Entity
interface TreeNode {

    @Key
    val name: String

    @ManyToOne
    @Key
    @OnDelete(DeleteAction.CASCADE)
    val parent: TreeNode?

    @OneToMany(mappedBy = "parent")
    val childNodes: List<TreeNode>

    /*
     * Set the id to be last field,
     * to test whether apt always generate it at first
     * (hashCode/equals requires it)
     */
    @Id
    @Column(name = "NODE_ID")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, sequenceName = "tree_node_id_seq")
    val id: Long
}