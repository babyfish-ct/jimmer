package org.babyfish.jimmer.sql.kt.filter.impl

import org.babyfish.jimmer.sql.ast.table.Props
import org.babyfish.jimmer.sql.filter.ShardingFilter
import org.babyfish.jimmer.sql.kt.filter.KShardingFilter

internal class KtShardingFilter<E: Any>(
    javaFilter: ShardingFilter<Props>
) : KtFilter<E>(javaFilter), KShardingFilter<E>