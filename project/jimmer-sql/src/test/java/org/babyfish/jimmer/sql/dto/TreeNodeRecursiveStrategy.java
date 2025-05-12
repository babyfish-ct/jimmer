package org.babyfish.jimmer.sql.dto;

import org.babyfish.jimmer.sql.fetcher.RecursionStrategy;
import org.babyfish.jimmer.sql.model.TreeNode;
import org.jetbrains.annotations.NotNull;

public class TreeNodeRecursiveStrategy implements RecursionStrategy<TreeNode> {

    @Override
    public boolean isRecursive(@NotNull Args<TreeNode> args) {
        return !args.getEntity().name().equals("Clothing");
    }
}
