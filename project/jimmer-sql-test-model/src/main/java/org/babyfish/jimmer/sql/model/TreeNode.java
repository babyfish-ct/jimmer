package org.babyfish.jimmer.sql.model;

import org.babyfish.jimmer.sql.*;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * The entity of recursive tree node
 */
@Entity
@KeyUniqueConstraint(noMoreUniqueConstraints = true, isNullNotDistinct = true)
public interface TreeNode {

    @Key
    String name();

    @Nullable
    @ManyToOne
    @Key
    @OnDissociate(DissociateAction.DELETE)
    TreeNode parent();

    @OneToMany(mappedBy = "parent", orderedProps = @OrderedProp("id"))
    List<TreeNode> childNodes();

    /*
     * Set the id to be last field,
     * to test whether apt always generate it at first
     * (hashCode/equals requires it)
     */
    @Id
    @Column(name = "NODE_ID")
    @GeneratedValue(sequenceName = "tree_node_id_seq")
    long id();
}
