package org.babyfish.jimmer.sql.kt.filter.impl

import org.babyfish.jimmer.impl.util.InvocationDelegate
import org.babyfish.jimmer.sql.ast.table.Props
import org.babyfish.jimmer.sql.filter.AssociationIntegrityAssuranceFilter
import org.babyfish.jimmer.sql.filter.CacheableFilter
import org.babyfish.jimmer.sql.filter.ShardingFilter
import org.babyfish.jimmer.sql.filter.Filter
import org.babyfish.jimmer.sql.filter.impl.FilterWrapper
import org.babyfish.jimmer.sql.kt.filter.KAssociationIntegrityAssuranceFilter
import org.babyfish.jimmer.sql.kt.filter.KCacheableFilter
import org.babyfish.jimmer.sql.kt.filter.KFilter
import org.babyfish.jimmer.sql.kt.filter.KShardingFilter
import java.lang.reflect.Proxy

@Suppress("UNCHECKED_CAST")
fun <E: Any> Filter<Props>.toKtFilter(): KFilter<E> {
    val coreFilter = FilterWrapper.unwrap(this)
    if (coreFilter is KFilter<*>) {
        return coreFilter as KFilter<E>
    }
    val ktFilter = when {
        this is CacheableFilter ->
            KtCacheableFilter<E>(coreFilter as CacheableFilter<Props>)
        else ->
            KtFilter(coreFilter as Filter<Props>)
    }
    if (this !is ShardingFilter && this !is AssociationIntegrityAssuranceFilter) {
        return ktFilter
    }
    val interfaces = mutableListOf<Class<*>>()
    interfaces.add(Filter::class.java)
    if (this is CacheableFilter) {
        interfaces.add(KCacheableFilter::class.java)
    }
    if (this is ShardingFilter) {
        interfaces.add(KShardingFilter::class.java)
    }
    if (this is AssociationIntegrityAssuranceFilter) {
        interfaces.add(KAssociationIntegrityAssuranceFilter::class.java)
    }
    return Proxy.newProxyInstance(
        Filter::class.java.classLoader,
        interfaces.toTypedArray(),
        InvocationDelegate(ktFilter)
    ) as KFilter<E>
}
