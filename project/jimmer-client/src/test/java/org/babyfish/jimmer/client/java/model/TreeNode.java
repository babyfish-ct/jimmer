package org.babyfish.jimmer.client.java.model;

import org.babyfish.jimmer.sql.Entity;
import org.babyfish.jimmer.sql.Id;
import org.babyfish.jimmer.sql.ManyToOne;
import org.babyfish.jimmer.sql.OneToMany;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@Entity
public interface TreeNode {

    @Id
    long id();

    String name();

    @ManyToOne
    @Nullable
    TreeNode parent();

    @OneToMany(mappedBy = "parent")
    List<TreeNode> childNodes();
}
