package org.babyfish.jimmer.sql.kt.filter.impl

import org.babyfish.jimmer.impl.util.InvocationDelegate
import org.babyfish.jimmer.sql.filter.AssociationIntegrityAssuranceFilter
import org.babyfish.jimmer.sql.filter.CacheableFilter
import org.babyfish.jimmer.sql.filter.Filter
import org.babyfish.jimmer.sql.filter.ShardingFilter
import org.babyfish.jimmer.sql.filter.impl.FilterWrapper
import org.babyfish.jimmer.sql.kt.filter.KAssociationIntegrityAssuranceFilter
import org.babyfish.jimmer.sql.kt.filter.KCacheableFilter
import org.babyfish.jimmer.sql.kt.filter.KShardingFilter
import org.babyfish.jimmer.sql.kt.filter.KFilter
import java.lang.reflect.Proxy

fun KFilter<*>.toJavaFilter(): Filter<*> {
    val coreFilter = FilterWrapper.unwrap(this)
    if (coreFilter is Filter<*>) {
        return coreFilter
    }
    val javaFilter = when {
        this is KCacheableFilter ->
            JavaCacheableFilter(coreFilter as KCacheableFilter<*>)
        else ->
            JavaFilter(coreFilter as KFilter<*>)
    }
    if (this !is KShardingFilter && this !is KAssociationIntegrityAssuranceFilter) {
        return javaFilter
    }
    val interfaces = mutableListOf<Class<*>>()
    interfaces.add(Filter::class.java)
    if (this is KCacheableFilter) {
        interfaces.add(CacheableFilter::class.java)
    }
    if (this is KShardingFilter) {
        interfaces.add(ShardingFilter::class.java)
    }
    if (this is KAssociationIntegrityAssuranceFilter) {
        interfaces.add(AssociationIntegrityAssuranceFilter::class.java)
    }
    return Proxy.newProxyInstance(
        Filter::class.java.classLoader,
        interfaces.toTypedArray(),
        InvocationDelegate(javaFilter)
    ) as Filter<*>
}
