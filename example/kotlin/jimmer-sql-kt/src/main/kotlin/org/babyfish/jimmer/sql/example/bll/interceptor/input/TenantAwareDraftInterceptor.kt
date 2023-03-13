package org.babyfish.jimmer.sql.example.bll.interceptor.input

import org.babyfish.jimmer.sql.example.bll.interceptor.TenantProvider
import org.babyfish.jimmer.kt.isLoaded
import org.babyfish.jimmer.sql.DraftInterceptor
import org.babyfish.jimmer.sql.example.model.common.TenantAware
import org.babyfish.jimmer.sql.example.model.common.TenantAwareDraft
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class TenantAwareDraftInterceptor(
    private val tenantProvider: TenantProvider,
    @Value("\${demo.default-tenant}") private val defaultTenant: String
) : DraftInterceptor<TenantAwareDraft> {

    override fun beforeSave(draft: TenantAwareDraft, isNew: Boolean) {
        if (!isLoaded(draft, TenantAware::tenant)) {
            draft.tenant = tenantProvider.tenant ?: defaultTenant
        }
    }
}