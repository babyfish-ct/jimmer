package org.babyfish.jimmer.sql.event.impl;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.sql.ast.Expression;
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
                    .createQuery(sqlClient, thisType, ExecutionPurpose.EVICT, true, (q, table) -> {
                        Expression<Object> idExpr = table.get(thisType.getIdProp().getName());
                        Expression<Object> backRefIdExpr = table.join(prop.getName()).get(backRefType.getIdProp().getName());
                        q.where(idExpr.eq(id));
                        return q.select(backRefIdExpr);
                    })
                    .execute(con);
        }
        return Queries
                .createQuery(sqlClient, backRefType, ExecutionPurpose.EVICT, true, (q, table) -> {
                    Expression<?> backRefIdExpr = table.get(backRefType.getIdProp().getName());
                    Expression<Object> idExpr = table.join(backProp.getName()).get(thisType.getIdProp().getName());
                    q.where(idExpr.eq(id));
                    return q.select(backRefIdExpr);
                })
                .distinct()
                .execute(con);
    }
}
