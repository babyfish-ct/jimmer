package org.babyfish.jimmer.sql.association.loader;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.sql.ListLoader;
import org.babyfish.jimmer.sql.ReferenceLoader;
import org.babyfish.jimmer.sql.SqlClient;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.fetcher.Filter;

public class Loaders {

    private Loaders() {}

    @SuppressWarnings("unchecked")
    public static <S, T> ReferenceLoader<S, T> createReferenceLoader(
            SqlClient sqlClient,
            ImmutableProp prop,
            Filter<? extends Table<? extends T>> filter
    ) {
        if (!prop.isReference()) {
            throw new IllegalArgumentException(
                    "Cannot create reference loader for \"" + prop + "\", it is not reference association"
            );
        }
        if (!prop.isNullable() && filter != null) {
            throw new IllegalArgumentException(
                    "Cannot create filterable loader for \"" + prop + "\", non-null association does not accept filter"
            );
        }
        return new ReferenceLoaderImpl<>(sqlClient, prop, filter);
    }

    @SuppressWarnings("unchecked")
    public static <S, T> ListLoader<S, T> createListLoader(
            SqlClient sqlClient,
            ImmutableProp prop,
            Filter<? extends Table<? extends T>> filter
    ) {
        if (!prop.isEntityList()) {
            throw new IllegalArgumentException(
                    "Cannot create list loader for \"" + prop + "\", it is not list association"
            );
        }
        return new ListLoaderImpl<>(sqlClient, prop, filter);
    }
}
