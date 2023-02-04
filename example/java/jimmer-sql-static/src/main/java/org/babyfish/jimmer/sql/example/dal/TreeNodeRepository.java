package org.babyfish.jimmer.sql.example.dal;

import org.babyfish.jimmer.Static;
import org.babyfish.jimmer.spring.repository.JRepository;
import org.babyfish.jimmer.sql.example.model.TreeNode;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface TreeNodeRepository extends JRepository<TreeNode, Long> {

    <S extends Static<TreeNode>> List<S> findByParentIsNullAndName(
            @Nullable String name,
            Class<S> staticType
    );
}
