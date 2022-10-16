package org.babyfish.jimmer.sql.model.inheritance;

import org.babyfish.jimmer.lang.Ref;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.TransientResolver;
import org.babyfish.jimmer.sql.ast.table.Props;
import org.babyfish.jimmer.sql.ast.tuple.Tuple2;
import org.babyfish.jimmer.sql.event.EntityEvent;
import org.babyfish.jimmer.sql.filter.CacheableFilter;
import org.babyfish.jimmer.sql.filter.Filter;
import org.babyfish.jimmer.sql.fluent.Fluent;

import java.sql.Connection;
import java.util.*;

public class RolePermissionCountResolver implements TransientResolver.Parameterized<Long, Integer> {

    private final JSqlClient sqlClient;

    private final CacheableFilter<Props> filter;

    public RolePermissionCountResolver(JSqlClient sqlClient) {
        this.sqlClient = sqlClient;
        this.filter = sqlClient.getFilters().getCacheableFilter(Permission.class);
        sqlClient.getTriggers().addAssociationListener(RoleProps.PERMISSIONS, e -> {
            sqlClient.getCaches().getPropertyCache(RoleProps.PERMISSION_COUNT).delete(e.getSourceId());
        });
        if (filter != null) {
            sqlClient.getTriggers().addEntityListener(Permission.class, e -> {
                Ref<Role> roleRef = e.getUnchangedFieldRef(PermissionProps.ROLE);
                Role role = roleRef != null ? roleRef.getValue() : null;
                if (role != null && filter.isAffectedBy(e)) {
                    sqlClient.getCaches().getPropertyCache(RoleProps.PERMISSION_COUNT).delete(role.getId());
                }
            });
        }
    }

    @Override
    public Map<Long, Integer> resolve(Collection<Long> roleIds, Connection con) {
        Fluent fluent = sqlClient.createFluent();
        PermissionTable permission = new PermissionTable();
        List<Tuple2<Long, Long>> tuples = fluent
                .query(permission)
                .where(permission.role().id().in(roleIds))
                .groupBy(permission.role().id())
                .select(
                        permission.role().id(),
                        permission.count()
                )
                .execute(con);
        return Tuple2.toMap(tuples, Long::intValue);
    }

    @Override
    public SortedMap<String, Object> getParameters() {
        return filter != null ?
                filter.getParameters() :
                Collections.emptySortedMap();
    }
}
