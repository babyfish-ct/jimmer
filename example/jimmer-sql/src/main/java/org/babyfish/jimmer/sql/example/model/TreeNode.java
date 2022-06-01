package org.babyfish.jimmer.sql.example.model;

import org.babyfish.jimmer.sql.DeleteAction;
import org.babyfish.jimmer.sql.Key;
import org.babyfish.jimmer.sql.OnDelete;

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
    @OnDelete(DeleteAction.CASCADE)
    TreeNode parent();

    @OneToMany(mappedBy = "parent")
    List<TreeNode> childNodes();
}
