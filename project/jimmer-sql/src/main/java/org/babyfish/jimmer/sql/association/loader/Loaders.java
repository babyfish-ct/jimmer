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
    public static <SE, TE, TT extends Table<TE>> ReferenceLoader<SE, TE, TT> createReferenceLoader(
            SqlClient sqlClient,
            ImmutableProp prop
    ) {
        if (!prop.isReference()) {
            throw new IllegalArgumentException(
                    "Cannot create reference loader for \"" + prop + "\", it is not reference association"
            );
        }
        return new ReferenceLoaderImpl<>(sqlClient, prop);
    }

    @SuppressWarnings("unchecked")
    public static <SE, TE, TT extends Table<TE>> ListLoader<SE, TE, TT> createListLoader(
            SqlClient sqlClient,
            ImmutableProp prop
    ) {
        if (!prop.isEntityList()) {
            throw new IllegalArgumentException(
                    "Cannot create list loader for \"" + prop + "\", it is not list association"
            );
        }
        return new ListLoaderImpl<>(sqlClient, prop);
    }
}
