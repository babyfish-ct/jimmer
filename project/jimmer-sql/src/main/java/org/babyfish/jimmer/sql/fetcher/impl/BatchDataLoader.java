package org.babyfish.jimmer.sql.fetcher.impl;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.sql.SqlClient;
import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.impl.query.Queries;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.ast.tuple.Tuple2;
import org.babyfish.jimmer.sql.fetcher.Field;
import org.babyfish.jimmer.sql.meta.Column;

import java.sql.Connection;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

class BatchDataLoader {

    private SqlClient sqlClient;

    private Connection con;

    private Field field;

    public BatchDataLoader(SqlClient sqlClient, Connection con, Field field) {
        this.sqlClient = sqlClient;
        this.con = con;
        this.field = field;
    }

    public Map<Object, ?> load(Collection<Object> keys) {
        if (keys.isEmpty()) {
            return Collections.emptyMap();
        }
        if (field.getProp().getStorage() instanceof Column) {
            return loadByForeignKey(keys);
        }
        throw new UnsupportedOperationException();
    }

    @SuppressWarnings("unchecked")
    private Map<Object, ImmutableSpi> loadByForeignKey(Collection<Object> keys) {
        ImmutableProp prop = field.getProp();
        List<Tuple2<Object, ImmutableSpi>> targets = Queries.createQuery(
                sqlClient,
                prop.getTargetType(),
                (q, t) -> {
                    Table<ImmutableSpi> table = (Table<ImmutableSpi>) t;
                    Expression<Object> pk = table.get(
                            prop.getTargetType().getIdProp().getName()
                    );
                    q.where(pk.in(keys));
                    return q.select(
                            pk,
                            table
                    );
                }
        ).execute(con);
        return Tuple2.toMap(targets);
    }
}
