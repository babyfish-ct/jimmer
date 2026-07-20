package org.babyfish.jimmer.sql.model.issue1252;

import org.babyfish.jimmer.sql.*;
import org.jspecify.annotations.Nullable;

import java.util.List;

@Entity
@Table(name = "TREE_NODE_2")
@DatabaseValidationIgnore
public interface TreeNode2 {

    @Key
    String name();

    @Nullable
    @ManyToOne
    @Key
    @OnDissociate(DissociateAction.DELETE)
    TreeNode2 parent();

    @OneToMany(mappedBy = "parent", orderedProps = @OrderedProp("name"))
    List<TreeNode2> childNodes();

    @Id
    @Column(name = "NODE_ID")
    @GeneratedValue(sequenceName = "tree_node_id_seq")
    long id();
}
