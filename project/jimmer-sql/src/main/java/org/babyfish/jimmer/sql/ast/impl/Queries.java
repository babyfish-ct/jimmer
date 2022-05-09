package org.babyfish.jimmer.sql.ast.impl;

import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.sql.SqlClient;
import org.babyfish.jimmer.sql.ast.query.ConfigurableTypedRootQuery;
import org.babyfish.jimmer.sql.ast.query.MutableRootQuery;
import org.babyfish.jimmer.sql.ast.table.Table;

import java.util.function.BiFunction;

public class Queries {

    public static <T extends Table<?>, R> ConfigurableTypedRootQuery<R> createQuery(
            Class<T> tableType,
            SqlClient sqlClient,
            BiFunction<MutableRootQuery, T, ConfigurableTypedRootQuery<R>> block
    ) {
        ImmutableType immutableType = ImmutableType.tryGet(tableType);
        if (immutableType == null) {
            throw new IllegalArgumentException(
                    "Cannot get immutable type from table type \"" +
                            tableType.getName() +
                            "\""
            );
        }
        RootMutableQueryImpl query = new RootMutableQueryImpl(
                sqlClient,
                immutableType
        );
        return block.apply(query, (T)query.getTable());
    }
}
