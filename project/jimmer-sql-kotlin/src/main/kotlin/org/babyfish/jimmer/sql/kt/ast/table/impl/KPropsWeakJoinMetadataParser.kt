package org.babyfish.jimmer.sql.kt.ast.table.impl

import org.babyfish.jimmer.lang.Generics
import org.babyfish.jimmer.sql.ast.table.spi.WeakJoinMetadata
import org.babyfish.jimmer.sql.ast.table.spi.WeakJoinMetadataParser
import org.babyfish.jimmer.sql.kt.ast.table.KBaseTable
import org.babyfish.jimmer.sql.kt.ast.table.KPropsWeakJoin
import java.lang.reflect.ParameterizedType

internal class KPropsWeakJoinMetadataParser : WeakJoinMetadataParser {

    override fun parse(weakJoinType: Class<*>): WeakJoinMetadata {
        val arguments = Generics.getTypeArguments(weakJoinType, KPropsWeakJoin::class.java)
        val sourceTableType = arguments[0] as ParameterizedType
        val targetTableType = arguments[1] as ParameterizedType
        val isSourceBaseTable = KBaseTable::class.java.isAssignableFrom(sourceTableType.rawType as Class<*>)
        val isTargetBaseTable = KBaseTable::class.java.isAssignableFrom(targetTableType.rawType as Class<*>)
        val sourceEntityType =
            if (isSourceBaseTable) {
                null
            } else {
                sourceTableType.actualTypeArguments[0] as Class<*>
            }
        val targetEntityType =
            if (isSourceBaseTable) {
                null
            } else {
                targetTableType.actualTypeArguments[1] as Class<*>
            }
        return WeakJoinMetadata(
            isSourceBaseTable,
            isTargetBaseTable,
            sourceEntityType,
            targetEntityType
        )
    }
}