package org.babyfish.jimmer.sql.example.kt.repository

import org.babyfish.jimmer.View
import org.babyfish.jimmer.spring.repository.KRepository
import org.babyfish.jimmer.sql.example.kt.model.TreeNode
import org.babyfish.jimmer.sql.fetcher.Fetcher
import kotlin.reflect.KClass

interface TreeNodeRepository : KRepository<TreeNode, Long> { // ❶

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
    fun <V: View<TreeNode>> findByNameLikeIgnoreCase( // ❷
        name: String?,
        viewType: KClass<V> // ❸
    ): List<V>

    fun findByParentIsNullAndName( // ❹
        name: String?,
        fetcher: Fetcher<TreeNode>?
    ): List<TreeNode>
}

/*----------------Documentation Links----------------
❶ https://babyfish-ct.github.io/jimmer/docs/spring/repository/concept
❷ ❹ https://babyfish-ct.github.io/jimmer/docs/spring/repository/abstract

❸ https://babyfish-ct.github.io/jimmer/docs/spring/repository/dto
  https://babyfish-ct.github.io/jimmer/docs/query/object-fetcher/dto
---------------------------------------------------*/
