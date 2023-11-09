package org.babyfish.jimmer.sql.example.model;

import org.babyfish.jimmer.sql.Key;

import org.babyfish.jimmer.sql.*;
import org.babyfish.jimmer.sql.example.model.common.BaseEntity;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@Entity
public interface TreeNode extends BaseEntity {

    @Id
    @Column(name = "NODE_ID")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    long id();

    @Key // ❶
    String name();

    @Nullable // ❷ Null property, Java API requires this annotation, but kotlin API does not
    @Key // ❸
    @ManyToOne // ❹
    @OnDissociate(DissociateAction.DELETE) // ❺
    TreeNode parent();

    @OneToMany(mappedBy = "parent", orderedProps = @OrderedProp("name")) // ❻
    List<TreeNode> childNodes();
}

/*----------------Documentation Links----------------
❶ ❸ https://babyfish-ct.github.io/jimmer/docs/mapping/advanced/key
❷ https://babyfish-ct.github.io/jimmer/docs/mapping/base/nullity
❹ https://babyfish-ct.github.io/jimmer/docs/mapping/base/association/many-to-one

❺ https://babyfish-ct.github.io/jimmer/docs/mapping/advanced/on-dissociate
  https://babyfish-ct.github.io/jimmer/docs/mutation/save-command/dissociation
  https://babyfish-ct.github.io/jimmer/docs/mutation/delete-command

❻ https://babyfish-ct.github.io/jimmer/docs/mapping/base/association/one-to-many
---------------------------------------------------*/
