package org.babyfish.jimmer.sql.event.impl;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.impl.query.FilterLevel;
import org.babyfish.jimmer.sql.ast.impl.query.Queries;
import org.babyfish.jimmer.sql.runtime.ExecutionPurpose;
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor;

import java.sql.Connection;
import java.util.List;

public class BackRefIds {

    public static List<?> findBackRefIds(
            JSqlClientImplementor sqlClient,
            ImmutableProp backProp,
            Object id,
            Connection con
    ) {
        ImmutableType backRefType = backProp.getDeclaringType();
        ImmutableType thisType = backProp.getTargetType();
        ImmutableProp prop = backProp.getMappedBy();
        if (prop != null && prop.isColumnDefinition()) {
            return Queries
                    .createQuery(sqlClient, thisType, ExecutionPurpose.EVICT, FilterLevel.IGNORE_USER_FILTERS, (q, table) -> {
                        q.where(table.getId().eq(id));
                        q.where(table.getAssociatedId(prop).isNotNull());
                        return q.select(table.getAssociatedId(prop));
                    })
                    .execute(con);
        }
        return Queries
                .createQuery(sqlClient, backRefType, ExecutionPurpose.EVICT, FilterLevel.IGNORE_USER_FILTERS, (q, table) -> {
                    q.where(table.getAssociatedId(backProp).eq(id));
                    return q.select(table.getId());
                })
                .distinct()
                .execute(con);
    }
}
