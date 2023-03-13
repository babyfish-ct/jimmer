package org.babyfish.jimmer.example.kt.graphql.bll.interceptor.input

import org.babyfish.jimmer.example.kt.graphql.entities.common.TenantAware
import org.babyfish.jimmer.example.kt.graphql.entities.common.TenantAwareDraft
import org.babyfish.jimmer.example.kt.graphql.bll.interceptor.TenantProvider
import org.babyfish.jimmer.kt.isLoaded
import org.babyfish.jimmer.sql.DraftInterceptor
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