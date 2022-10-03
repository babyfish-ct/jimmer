package org.babyfish.jimmer.sql.example.model;

import org.babyfish.jimmer.sql.Key;

import org.babyfish.jimmer.sql.*;

import javax.validation.constraints.Null;
import java.util.List;

@Entity
public interface TreeNode extends CommonEntity {

    @Id
    @Column(name = "NODE_ID")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    long id();

    @Key
    String name();

    @Null // Null property, Java API requires this annotation, but kotlin API does not
    @Key
    @ManyToOne
    @OnDissociate(DissociateAction.DELETE)
    TreeNode parent();

    @OneToMany(mappedBy = "parent")
    List<TreeNode> childNodes();
}
