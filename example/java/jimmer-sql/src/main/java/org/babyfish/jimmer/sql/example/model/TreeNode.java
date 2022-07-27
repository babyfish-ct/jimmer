package org.babyfish.jimmer.sql.example.model;

import org.babyfish.jimmer.sql.DeleteAction;
import org.babyfish.jimmer.sql.Key;
import org.babyfish.jimmer.sql.OnDelete;

import org.babyfish.jimmer.sql.*;

import javax.validation.constraints.Null;
import java.util.List;

@Entity
public interface TreeNode {

    @Id
    @Column(name = "NODE_ID")
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            sequenceName = "TREE_NODE_ID_SEQ"
    )
    long id();

    @Key
    String name();

    @Null // Null property, Java API requires this annotation, but kotlin API does not
    @Key
    @ManyToOne
    @OnDelete(DeleteAction.CASCADE)
    TreeNode parent();

    @OneToMany(mappedBy = "parent")
    List<TreeNode> childNodes();
}
