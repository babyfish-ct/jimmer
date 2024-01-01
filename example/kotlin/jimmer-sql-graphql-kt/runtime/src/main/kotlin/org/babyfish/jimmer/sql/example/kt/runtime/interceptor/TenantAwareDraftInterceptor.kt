package org.babyfish.jimmer.sql.example.kt.runtime.interceptor

import org.babyfish.jimmer.kt.isLoaded
import org.babyfish.jimmer.sql.DraftInterceptor
import org.babyfish.jimmer.sql.example.kt.model.common.TenantAware
import org.babyfish.jimmer.sql.example.kt.model.common.TenantAwareDraft
import org.babyfish.jimmer.sql.example.kt.runtime.TenantProvider
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class TenantAwareDraftInterceptor(
    private val tenantProvider: TenantProvider,
    @Value("\${demo.default-tenant}") private val defaultTenant: String
) : DraftInterceptor<TenantAware, TenantAwareDraft> {

    override fun beforeSave(draft: TenantAwareDraft, original: TenantAware?) {
        if (!isLoaded(draft, TenantAware::tenant)) {
            draft.tenant = tenantProvider.tenant ?: defaultTenant
        }
    }
}
