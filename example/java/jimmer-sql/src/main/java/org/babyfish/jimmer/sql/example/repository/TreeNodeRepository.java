package org.babyfish.jimmer.sql.example.repository;

import org.babyfish.jimmer.View;
import org.babyfish.jimmer.spring.repository.JRepository;
import org.babyfish.jimmer.sql.example.model.TreeNode;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface TreeNodeRepository extends JRepository<TreeNode, Long> {

    /*
     * This approach is very special, the rest query methods of the project returns 'dynamic object + @FetchBy',
     * but it directly returns static types which should be generated at compilation-time.
     *
     * In fact, you can define a method as:
     * List<FlatTreeNodeView> findByNameLikeIgnoreCase(@Nullable String name)
     *
     * However, a better development experience will determine the shape of the data structure at the business layer,
     * not the data layer. So, let's define the parameter `viewType`
     */
    <V extends View<TreeNode>> List<V> findByNameLikeIgnoreCase(
            @Nullable String name,
            Class<V> viewType
    );

    List<TreeNode> findByParentIsNullAndName(
            @Nullable String name,
            Fetcher<TreeNode> fetcher
    );
}
