package org.babyfish.jimmer.sql.example.repository

import org.babyfish.jimmer.View
import org.babyfish.jimmer.spring.repository.KRepository
import org.babyfish.jimmer.sql.example.model.TreeNode
import org.babyfish.jimmer.sql.fetcher.Fetcher
import kotlin.reflect.KClass

interface TreeNodeRepository : KRepository<TreeNode, Long> {

    /*
     * This approach is very special, the rest query methods of the project returns 'dynamic object + @FetchBy',
     * but it directly returns static types which should be generated at compilation-time.
     *
     * In fact, you can also define this method as:
     * List<FlatTreeNodeView> findByNameLikeIgnoreCase(@Nullable String name)
     *
     * However, a better development experience is to determine the shape of the data structure
     * at the business layer, not the data layer. So, let's define the parameter `viewType`
     */
    fun <V: View<TreeNode>> findByNameLikeIgnoreCase(
        name: String?,
        viewType: KClass<V>
    ): List<V>

    fun findByParentIsNullAndName(
        name: String?,
        fetcher: Fetcher<TreeNode>?
    ): List<TreeNode>
}