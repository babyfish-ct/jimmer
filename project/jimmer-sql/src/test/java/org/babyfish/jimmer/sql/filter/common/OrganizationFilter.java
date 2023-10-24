package org.babyfish.jimmer.sql.filter.common;

import org.babyfish.jimmer.sql.cache.ParameterMaps;
import org.babyfish.jimmer.sql.event.EntityEvent;
import org.babyfish.jimmer.sql.filter.CacheableFilter;
import org.babyfish.jimmer.sql.filter.FilterArgs;
import org.babyfish.jimmer.sql.model.filter.OrganizationProps;

import java.util.SortedMap;

public class OrganizationFilter implements CacheableFilter<OrganizationProps> {

    private static final ThreadLocal<String> TENANT_LOCAL = new ThreadLocal<>();

    @Override
    public SortedMap<String, Object> getParameters() {
        return ParameterMaps.of("tenant", tenant());
    }

    @Override
    public void filter(FilterArgs<OrganizationProps> args) {
        args.where(args.getTable().tenant().eq(tenant()));
    }

    @Override
    public boolean isAffectedBy(EntityEvent<?> e) {
        return e.isChanged(OrganizationProps.TENANT);
    }

    private String tenant() {
        String tenant = TENANT_LOCAL.get();
        if (tenant == null) {
            throw new IllegalStateException("No tenant in context");
        }
        return tenant;
    }
}
