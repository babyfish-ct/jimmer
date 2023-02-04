package org.babyfish.jimmer.example.kt.sql.bll.interceptor.output

import org.babyfish.jimmer.example.kt.sql.bll.interceptor.TenantProvider
import org.babyfish.jimmer.example.kt.sql.model.common.TenantAware
import org.babyfish.jimmer.sql.event.EntityEvent
import org.babyfish.jimmer.sql.kt.event.getChangedFieldRef
import org.babyfish.jimmer.sql.kt.filter.KCacheableFilter
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import java.util.*

/*
 * see KSqlClientDsl.addFilters
 *
 * This bean is only be used when cache is enabled.
 */
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