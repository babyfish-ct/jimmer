package org.babyfish.jimmer.sql.example.interceptor.output;

import org.babyfish.jimmer.sql.example.interceptor.TenantProvider;
import org.babyfish.jimmer.sql.example.model.common.TenantAwareProps;
import org.babyfish.jimmer.sql.filter.Filter;
import org.babyfish.jimmer.sql.filter.FilterArgs;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

/*
 * This bean is only be used when cache is NOT used.
 */
@ConditionalOnMissingBean(TenantFilterForCacheMode.class)
@Component
public class TenantFilterForNonCacheMode implements Filter<TenantAwareProps> {

    private final TenantProvider tenantProvider;

    public TenantFilterForNonCacheMode(TenantProvider tenantProvider) {
        this.tenantProvider = tenantProvider;
    }

    @Override
    public void filter(FilterArgs<TenantAwareProps> args) {
        String tenant = tenantProvider.get();
        if (!tenant.isEmpty()) {
            args.where(args.getTable().tenant().eq(tenant));
        }
    }
}
