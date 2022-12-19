package org.babyfish.jimmer.sql.example.dal;

import org.babyfish.jimmer.spring.repository.JRepository;
import org.babyfish.jimmer.sql.example.model.TreeNode;
import org.babyfish.jimmer.sql.example.model.TreeNodeTable;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.jetbrains.annotations.Nullable;
import org.springframework.util.StringUtils;

import java.util.List;

public interface TreeNodeRepository extends JRepository<TreeNode, Long> {

    TreeNodeTable table = TreeNodeTable.$;

    default List<TreeNode> findRootNodes(
            @Nullable String name,
            Fetcher<TreeNode> fetcher
    ) {
        return sql()
                .createQuery(table)
                .where(table.parent().isNull())
                .whereIf(StringUtils.hasText(name), table.name().ilike(name))
                .select(table.fetch(fetcher))
                .execute();
    }
}
