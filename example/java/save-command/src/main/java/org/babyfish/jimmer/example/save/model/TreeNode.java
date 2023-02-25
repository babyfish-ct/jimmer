package org.babyfish.jimmer.example.save.model;

import org.babyfish.jimmer.sql.*;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@Entity
public interface TreeNode {

    @Id
    @Column(name = "node_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    long id();

    @Key
    String name();

    @Key
    @Nullable
    @JoinColumn(name = "parent_id")
    @ManyToOne
    TreeNode parentNode();

    @OneToMany(mappedBy = "parentNode")
    List<TreeNode> childNodes();
}
