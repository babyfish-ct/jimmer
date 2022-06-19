package org.babyfish.jimmer.sql.model;

import org.babyfish.jimmer.sql.DeleteAction;
import org.babyfish.jimmer.sql.Key;
import org.babyfish.jimmer.sql.OnDelete;

import javax.persistence.*;
import java.util.List;

@Entity
public interface TreeNode {

    @Key
    String name();

    @ManyToOne
    @Key
    @OnDelete(DeleteAction.CASCADE)
    TreeNode parent();

    @OneToMany(mappedBy = "parent")
    List<TreeNode> childNodes();

    /*
     * Set the id to be last field,
     * to test whether apt always generate it at first
     * (hashCode/equals requires it)
     */
    @Id
    @Column(name = "NODE_ID", nullable = false)
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "sequence:tree_node_id_seq"
    )
    long id();
}
