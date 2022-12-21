package org.babyfish.jimmer.sql.example.graphql.bll.interceptor.input;

import org.babyfish.jimmer.ImmutableObjects;
import org.babyfish.jimmer.sql.DraftInterceptor;
import org.babyfish.jimmer.sql.example.graphql.entities.common.TenantAwareDraft;
import org.babyfish.jimmer.sql.example.graphql.entities.common.TenantAwareProps;
import org.babyfish.jimmer.sql.example.graphql.bll.interceptor.TenantProvider;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

/*
 * See JSqlClient.Builder.addDraftInterceptors()
 */
@Component
public class TenantAwareDraftInterceptor implements DraftInterceptor<TenantAwareDraft> {

    private final TenantProvider tenantProvider;

    public TenantAwareDraftInterceptor(TenantProvider tenantProvider) {
        this.tenantProvider = tenantProvider;
    }

    @Override
    public void beforeSave(@NotNull TenantAwareDraft draft, boolean isNew) {
        if (!ImmutableObjects.isLoaded(draft, TenantAwareProps.TENANT)) {
            String tenant = tenantProvider.get();
            if (tenant == null) {
                throw new IllegalStateException(
                        "Global tenant must be specified when the tenant of saved object is not specified"
                );
            }
            draft.setTenant(tenant);
        }
    }
}


