package org.babyfish.jimmer.sql.example.kt.model

import org.babyfish.jimmer.sql.*
import org.babyfish.jimmer.sql.example.kt.model.common.BaseEntity

@Entity
interface TreeNode : BaseEntity {

    @Id
    @Column(name = "NODE_ID")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long

    @Key // ❶
    val name: String

    @Key // ❷
    @ManyToOne // ❸
    @OnDissociate(DissociateAction.DELETE) // ❹
    val parent: TreeNode?

    @OneToMany(mappedBy = "parent", orderedProps = [OrderedProp("name")]) // ❺
    val childNodes: List<TreeNode>
}

/*----------------Documentation Links----------------
❶ ❷ https://babyfish-ct.github.io/jimmer/docs/mapping/advanced/key
❸ https://babyfish-ct.github.io/jimmer/docs/mapping/base/association/many-to-one

❹ https://babyfish-ct.github.io/jimmer/docs/mapping/advanced/on-dissociate
  https://babyfish-ct.github.io/jimmer/docs/mutation/save-command/dissociation
  https://babyfish-ct.github.io/jimmer/docs/mutation/delete-command

❺ https://babyfish-ct.github.io/jimmer/docs/mapping/base/association/one-to-many
---------------------------------------------------*/