package org.babyfish.jimmer.example.save.model;

import org.babyfish.jimmer.sql.*;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@Entity
public interface TreeNode {

    @Id
    @Column(name = "node_id")
    // `identity(1, 1)` in database, so it is 1, 2, 3 ...
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    long id();

    @Key
    String name();

    @Key
    @Nullable
    @ManyToOne
    @JoinColumn(name = "parent_id")
    @OnDissociate(DissociateAction.DELETE)
    TreeNode parentNode();

    @OneToMany(mappedBy = "parentNode")
    List<TreeNode> childNodes();
}
