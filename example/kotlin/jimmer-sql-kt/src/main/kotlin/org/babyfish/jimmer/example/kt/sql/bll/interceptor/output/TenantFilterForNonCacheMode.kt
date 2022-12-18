package org.babyfish.jimmer.example.kt.sql.bll.interceptor.output

import org.babyfish.jimmer.example.kt.sql.bll.interceptor.TenantProvider
import org.babyfish.jimmer.example.kt.sql.model.common.TenantAware
import org.babyfish.jimmer.example.kt.sql.model.common.tenant
import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.babyfish.jimmer.sql.kt.ast.expression.isNull
import org.babyfish.jimmer.sql.kt.ast.expression.or
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