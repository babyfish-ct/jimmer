package org.babyfish.jimmer.sql.model;

import javax.persistence.*;
import java.util.List;

@Entity
public interface TreeNode {

    @Id
    @Column(name = "NODE_ID", nullable = false)
    long id();

    String name();

    @ManyToOne
    TreeNode parent();

    @OneToMany(mappedBy = "parent")
    List<TreeNode> childNodes();
}
