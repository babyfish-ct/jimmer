package org.babyfish.jimmer.sql.example.model;

import org.babyfish.jimmer.sql.CascadeAction;
import org.babyfish.jimmer.sql.Key;
import org.babyfish.jimmer.sql.OnCascade;

import javax.persistence.*;
import java.util.List;

@Entity
public interface TreeNode {

    @Id
    @Column(name = "NODE_ID", nullable = false)
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "sequence:tree_node_id_seq"
    )
    long id();

    @Key
    String name();

    @ManyToOne
    @OnCascade(CascadeAction.DELETE)
    TreeNode parent();

    @OneToMany(mappedBy = "parent")
    List<TreeNode> childNodes();
}
