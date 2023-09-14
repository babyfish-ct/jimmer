package org.babyfish.jimmer.sql.example.business.interceptor.input

import org.babyfish.jimmer.sql.example.business.interceptor.TenantProvider
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
) : DraftInterceptor<TenantAwareDraft> { // ❶

    override fun beforeSave(draft: TenantAwareDraft, isNew: Boolean) { // ❷
        if (!isLoaded(draft, TenantAware::tenant)) { // ❸
            draft.tenant = tenantProvider.tenant ?: defaultTenant
        }
    }
}

/*----------------Documentation Links----------------
❶ https://babyfish-ct.github.io/jimmer/docs/mutation/draft-interceptor
❷ https://babyfish-ct.github.io/jimmer/docs/object/draft

❸ https://babyfish-ct.github.io/jimmer/docs/object/tool#isloaded
  https://babyfish-ct.github.io/jimmer/docs/object/dynamic
---------------------------------------------------*/
