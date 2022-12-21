package org.babyfish.jimmer.example.kt.graphql.bll.interceptor.output

import org.babyfish.jimmer.example.kt.graphql.entities.common.TenantAware
import org.babyfish.jimmer.example.kt.graphql.entities.common.tenant
import org.babyfish.jimmer.example.kt.graphql.bll.interceptor.TenantProvider
import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.babyfish.jimmer.sql.kt.filter.KFilter
import org.babyfish.jimmer.sql.kt.filter.KFilterArgs
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.stereotype.Component

/*
 * see KSqlClientDsl.addFilters
 *
 * This bean is only be used when cache is NOT enabled.
 */
@ConditionalOnMissingBean(TenantFilterForCacheMode::class)
@Component
class TenantFilterForNonCacheMode(
    protected val tenantProvider: TenantProvider
) : KFilter<TenantAware> {

    override fun filter(args: KFilterArgs<TenantAware>) {
        tenantProvider.tenant?.let {
            args.apply {
                where(table.tenant.eq(it))
            }
        }
    }
}