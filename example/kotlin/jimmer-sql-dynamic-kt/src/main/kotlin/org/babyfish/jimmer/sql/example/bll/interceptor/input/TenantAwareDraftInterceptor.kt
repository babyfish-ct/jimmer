package org.babyfish.jimmer.sql.example.bll.interceptor.input

import org.babyfish.jimmer.sql.example.bll.interceptor.TenantProvider
import org.babyfish.jimmer.kt.isLoaded
import org.babyfish.jimmer.sql.DraftInterceptor
import org.babyfish.jimmer.sql.example.model.common.TenantAware
import org.babyfish.jimmer.sql.example.model.common.TenantAwareDraft
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