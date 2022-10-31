package org.babyfish.jimmer.sql.model.inheritance;

import org.babyfish.jimmer.lang.Ref;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.TransientResolver;
import org.babyfish.jimmer.sql.ast.tuple.Tuple2;

import java.sql.Connection;
import java.util.*;

public class RolePermissionCountResolver implements TransientResolver<Long, Integer> {

    private final JSqlClient sqlClient;

    public RolePermissionCountResolver(JSqlClient sqlClient) {
        this.sqlClient = sqlClient;
        sqlClient.getTriggers().addAssociationListener(RoleProps.PERMISSIONS, e -> {
            sqlClient.getCaches().getPropertyCache(RoleProps.PERMISSION_COUNT).delete(e.getSourceId());
        });
    }

    @Override
    public Map<Long, Integer> resolve(Collection<Long> roleIds, Connection con) {
        PermissionTable permission = PermissionTable.$;
        List<Tuple2<Long, Long>> tuples = sqlClient
                .createQuery(permission)
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
    public Ref<SortedMap<String, Object>> getParameterMapRef() {
        return sqlClient.getFilters().getTargetParameterMapRef(RoleProps.PERMISSIONS);
    }
}
