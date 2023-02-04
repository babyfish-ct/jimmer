package org.babyfish.jimmer.sql.example.bll.interceptor.output;

import org.babyfish.jimmer.sql.event.EntityEvent;
import org.babyfish.jimmer.sql.example.bll.interceptor.TenantProvider;
import org.babyfish.jimmer.sql.example.model.common.TenantAwareProps;
import org.babyfish.jimmer.sql.filter.CacheableFilter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.SortedMap;
import java.util.TreeMap;

/*
 * see JSqlClient.Builder.addFilters
 *
 * This bean is only be used when cache is used.
 */
@Component
@ConditionalOnProperty("spring.redis.host")
public class TenantFilterForCacheMode
        extends TenantFilterForNonCacheMode
        implements CacheableFilter<TenantAwareProps> {

    public TenantFilterForCacheMode(TenantProvider tenantProvider) {
        super(tenantProvider);
    }

    @Override
    public SortedMap<String, Object> getParameters() {
        String tenant = tenantProvider.get();
        if (tenant == null) {
            return null;
        }
        SortedMap<String, Object> map = new TreeMap<>();
        map.put("tenant", tenant);
        return map;
    }

    @Override
    public boolean isAffectedBy(EntityEvent<?> e) {
        return e.getChangedFieldRef(TenantAwareProps.TENANT) != null;
    }
}
