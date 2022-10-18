package org.babyfish.jimmer.sql.model;

import org.babyfish.jimmer.sql.*;

import javax.validation.constraints.Null;
import java.util.List;

@Entity
public interface TreeNode {

    @Key
    String name();

    @Null
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
