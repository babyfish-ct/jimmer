package org.babyfish.jimmer.sql.example.bll.interceptor.input

import org.babyfish.jimmer.sql.example.bll.interceptor.TenantProvider
import org.babyfish.jimmer.kt.isLoaded
import org.babyfish.jimmer.sql.DraftInterceptor
import org.babyfish.jimmer.sql.example.bll.error.BusinessException
import org.babyfish.jimmer.sql.example.model.common.TenantAware
import org.babyfish.jimmer.sql.example.model.common.TenantAwareDraft
import org.springframework.stereotype.Component

@Component
class TenantAwareDraftInterceptor(
    private val tenantProvider: TenantProvider
) : DraftInterceptor<TenantAwareDraft> {

    override fun beforeSave(draft: TenantAwareDraft, isNew: Boolean) {
        if (!isLoaded(draft, TenantAware::tenant)) {
            val tenant = tenantProvider.tenant
                ?: throw BusinessException.globalTenantRequired(
                    "Global tenant is required"
                )
            draft.tenant = tenant
        }
    }
}