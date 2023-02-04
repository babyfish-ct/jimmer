package org.babyfish.jimmer.sql.example.dal

import org.babyfish.jimmer.Static
import org.babyfish.jimmer.spring.repository.KRepository
import org.babyfish.jimmer.sql.example.model.TreeNode
import kotlin.reflect.KClass

interface TreeNodeRepository : KRepository<TreeNode, Long> {

    fun <S: Static<TreeNode>> findByParentIsNullAndName(
        name: String?,
        staticType: KClass<S>
    ): List<S>
}