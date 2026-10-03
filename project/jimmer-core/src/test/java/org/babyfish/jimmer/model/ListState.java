package org.babyfish.jimmer.model;

import org.babyfish.jimmer.Immutable;
import org.jspecify.annotations.Nullable;

import java.util.List;

@Immutable
public interface ListState {
    List<String> left();
    List<String> right();
    List<TreeNode> nodes();
    @Nullable
    List<String> optional();
}
