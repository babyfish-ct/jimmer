package org.babyfish.jimmer.sql2.example.dal;

import org.babyfish.jimmer.spring.repository.JRepository;
import org.babyfish.jimmer.sql2.example.model.TreeNode;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface TreeNodeRepository extends JRepository<TreeNode, Long> {

    List<TreeNode> findByParentIsNullAndName(
            @Nullable String name,
            Fetcher<TreeNode> fetcher
    );
}
