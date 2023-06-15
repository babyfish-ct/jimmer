package org.babyfish.jimmer.sql.kt.cache

import org.babyfish.jimmer.meta.ImmutableType
import org.babyfish.jimmer.sql.filter.impl.FilterRangeAware

interface KFilterRangeAwareCacheFactory : KCacheFactory, FilterRangeAware {

    override fun setAffectedTypes(types: Set<ImmutableType>)
}