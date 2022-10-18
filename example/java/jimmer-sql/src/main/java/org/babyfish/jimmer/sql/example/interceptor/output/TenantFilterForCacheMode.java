package org.babyfish.jimmer.sql.example.interceptor.output;

import org.babyfish.jimmer.sql.event.EntityEvent;
import org.babyfish.jimmer.sql.example.interceptor.TenantProvider;
import org.babyfish.jimmer.sql.example.model.common.TenantAwareProps;
import org.babyfish.jimmer.sql.filter.CacheableFilter;
import org.babyfish.jimmer.sql.filter.FilterArgs;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.SortedMap;
import java.util.TreeMap;

/*
 * This bean is only be used when cache is used.
 */
@Component
@ConditionalOnProperty("spring.redis.host")
public class TenantFilterForCacheMode implements CacheableFilter<TenantAwareProps> {

    private final TenantProvider tenantProvider;

    public TenantFilterForCacheMode(TenantProvider tenantProvider) {
        this.tenantProvider = tenantProvider;
    }

    @Override
    public void filter(FilterArgs<TenantAwareProps> args) {
        String tenant = tenantProvider.get();
        if (!tenant.isEmpty()) {
            args.where(args.getTable().tenant().eq(tenant));
        }
    }

    @Override
    public SortedMap<String, Object> getParameters() {
        String tenant = tenantProvider.get();
        if (tenant.isEmpty()) {
            return Collections.emptySortedMap();
        }
        SortedMap<String, Object> map = new TreeMap<>();
        map.put("tenant", tenant);
        return map;
    }

    @Override
    public boolean isAffectedBy(EntityEvent<?> e) {
        return e.getUnchangedFieldRef(TenantAwareProps.TENANT) == null;
    }
}
