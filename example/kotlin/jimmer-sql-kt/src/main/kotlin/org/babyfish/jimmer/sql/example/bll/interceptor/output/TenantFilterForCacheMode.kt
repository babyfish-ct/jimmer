package org.babyfish.jimmer.sql.example.bll.interceptor.output

import org.babyfish.jimmer.sql.example.bll.interceptor.TenantProvider
import org.babyfish.jimmer.sql.event.EntityEvent
import org.babyfish.jimmer.sql.example.model.common.TenantAware
import org.babyfish.jimmer.sql.kt.event.getChangedFieldRef
import org.babyfish.jimmer.sql.kt.filter.KCacheableFilter
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import java.util.*

// -----------------------------
// If you are a beginner, please ignore this class
// and view its super class `TenantFilterForNonCacheMode`,
// for non-cache mode, this class will never be used.
//
// When the global filter generates additional query
// conditions for `Book`, different users will see different data.
//
// In this case, all associated properties and calculated properties
// related to `Book` will be affected, such as `BookStore.books`,
// `Author.books`, `BookStore.avgPrice`, `BookStore.newestBooks`.
//
// Even when running the program with caching enabled, all affected
// properties will not use the cache, unless multi-view caching is enabled
// for those affected properties.
//
// This class cooperates with `RedisHashBinder` in `CacheConfig`
// to support multi-view caching together.
// -----------------------------
@ConditionalOnProperty("spring.redis.host")
@Component
class TenantFilterForCacheMode(
    tenantProvider: TenantProvider
) : TenantFilterForNonCacheMode(tenantProvider), KCacheableFilter<TenantAware> {

    override fun getParameters(): SortedMap<String, Any>? =
        tenantProvider.tenant?.let {
            sortedMapOf("tenant" to it)
        }

    override fun isAffectedBy(e: EntityEvent<*>): Boolean =
        e.getChangedFieldRef(TenantAware::tenant) !== null
}