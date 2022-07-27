package org.babyfish.jimmer.example.core.model;

import org.babyfish.jimmer.Immutable;

import java.util.List;

@Immutable
public interface TreeNode {
    String name();
    TreeNode parent();
    List<TreeNode> childNodes();
}
