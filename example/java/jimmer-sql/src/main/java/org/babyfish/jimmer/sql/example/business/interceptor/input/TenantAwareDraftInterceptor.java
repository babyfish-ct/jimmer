package org.babyfish.jimmer.sql.example.business.interceptor.input;

import org.babyfish.jimmer.ImmutableObjects;
import org.babyfish.jimmer.sql.DraftInterceptor;
import org.babyfish.jimmer.sql.example.business.interceptor.TenantProvider;
import org.babyfish.jimmer.sql.example.model.common.TenantAwareDraft;
import org.babyfish.jimmer.sql.example.model.common.TenantAwareProps;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class TenantAwareDraftInterceptor implements DraftInterceptor<TenantAwareDraft> { //

    private final TenantProvider tenantProvider;

    private final String defaultTenant;

    public TenantAwareDraftInterceptor(
            TenantProvider tenantProvider,
            @Value("${demo.default-tenant}") String defaultTenant
    ) {
        this.tenantProvider = tenantProvider;
        this.defaultTenant = defaultTenant;
    }

    @Override
    public void beforeSave(@NotNull TenantAwareDraft draft, boolean isNew) { // ❷
        if (!ImmutableObjects.isLoaded(draft, TenantAwareProps.TENANT)) { // ❸
            String tenant = tenantProvider.get();
            if (tenant == null) {
                tenant = defaultTenant;
            }
            draft.setTenant(tenant);
        }
    }
}

/*----------------Documentation Links----------------
❶ https://babyfish-ct.github.io/jimmer/docs/mutation/draft-interceptor
❷ https://babyfish-ct.github.io/jimmer/docs/object/draft

❸ https://babyfish-ct.github.io/jimmer/docs/object/tool#isloaded
  https://babyfish-ct.github.io/jimmer/docs/object/dynamic
---------------------------------------------------*/
