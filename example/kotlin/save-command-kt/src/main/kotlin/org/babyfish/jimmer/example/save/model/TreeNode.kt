package org.babyfish.jimmer.example.save.model

import org.babyfish.jimmer.sql.*

@Entity
interface TreeNode {

    @Id
    @Column(name = "node_id") // `identity(1, 1)` in database, so it is 1, 2, 3 ...
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long

    @Key
    val name: String

    @Key
    @ManyToOne
    @JoinColumn(name = "parent_id")
    @OnDissociate(DissociateAction.DELETE)
    val parentNode: TreeNode?

    @OneToMany(mappedBy = "parentNode")
    val childNodes: List<TreeNode>
}