package org.babyfish.jimmer.example.kt.graphql.bll.interceptor.input

import org.babyfish.jimmer.example.kt.graphql.entities.common.TenantAware
import org.babyfish.jimmer.example.kt.graphql.entities.common.TenantAwareDraft
import org.babyfish.jimmer.example.kt.graphql.bll.interceptor.TenantProvider
import org.babyfish.jimmer.kt.isLoaded
import org.babyfish.jimmer.sql.DraftInterceptor
import org.springframework.stereotype.Component

/*
 * see KSqlClientDsl.addDraftInterceptors
 */
@Component
class TenantAwareDraftInterceptor(
    private val tenantProvider: TenantProvider
) : DraftInterceptor<TenantAwareDraft> {

    override fun beforeSave(draft: TenantAwareDraft, isNew: Boolean) {
        if (!isLoaded(draft, TenantAware::tenant)) {
            val tenant = tenantProvider.tenant ?: error(
                "Global tenant must be specified when the tenant of saved object is unspecified"
            )
            draft.tenant = tenant
        }
    }
}