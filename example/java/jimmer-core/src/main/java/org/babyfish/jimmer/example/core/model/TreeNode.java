package org.babyfish.jimmer.example.core.model;

import org.babyfish.jimmer.Immutable;

import javax.validation.constraints.Null;
import java.util.List;

@Immutable
public interface TreeNode {

    String name();

    @Null // Nullable property, Java-API needs it, but kotlin-API does not.
    TreeNode parent();

    List<TreeNode> childNodes();
}
