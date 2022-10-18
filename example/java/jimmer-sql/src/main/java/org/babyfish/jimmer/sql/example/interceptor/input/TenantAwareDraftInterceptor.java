package org.babyfish.jimmer.sql.example.interceptor.input;

import org.babyfish.jimmer.ImmutableObjects;
import org.babyfish.jimmer.sql.DraftInterceptor;
import org.babyfish.jimmer.sql.example.interceptor.TenantProvider;
import org.babyfish.jimmer.sql.example.model.common.TenantAwareDraft;
import org.babyfish.jimmer.sql.example.model.common.TenantAwareProps;
import org.jetbrains.annotations.NotNull;

public class TenantAwareDraftInterceptor implements DraftInterceptor<TenantAwareDraft> {

    private final TenantProvider tenantProvider;

    public TenantAwareDraftInterceptor(TenantProvider tenantProvider) {
        this.tenantProvider = tenantProvider;
    }

    @Override
    public void beforeSave(@NotNull TenantAwareDraft draft, boolean isNew) {
        if (!ImmutableObjects.isLoaded(draft, TenantAwareProps.TENANT)) {
            String tenant = tenantProvider.get();
            if (tenant.isEmpty()) {
                throw new IllegalStateException(
                        "Global tenant must be specified when the tenant of saved object is not specified"
                );
            }
            draft.setTenant(tenant);
        }
    }
}
