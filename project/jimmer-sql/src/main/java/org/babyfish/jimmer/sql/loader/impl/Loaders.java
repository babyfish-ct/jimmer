package org.babyfish.jimmer.sql.loader.impl;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.TargetLevel;
import org.babyfish.jimmer.sql.loader.ListLoader;
import org.babyfish.jimmer.sql.loader.ReferenceLoader;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.loader.ValueLoader;

public class Loaders {

    private Loaders() {}

    public static <S, V>ValueLoader<S, V> createValueLoader(
            JSqlClient sqlClient,
            ImmutableProp prop
    ) {
        if (!prop.hasTransientResolver()) {
            throw new IllegalArgumentException(
                    "Cannot create reference loader for \"" + prop + "\", it is not transient property with resolver"
            );
        }
        return new ValueLoaderImpl<>(sqlClient, prop);
    }

    public static <SE, TE, TT extends Table<TE>> ReferenceLoader<SE, TE, TT> createReferenceLoader(
            JSqlClient sqlClient,
            ImmutableProp prop
    ) {
        if (!prop.isReference(TargetLevel.ENTITY)) {
            throw new IllegalArgumentException(
                    "Cannot create reference loader for \"" + prop + "\", it is not entity reference association"
            );
        }
        return new ReferenceLoaderImpl<>(sqlClient, prop);
    }

    public static <SE, TE, TT extends Table<TE>> ListLoader<SE, TE, TT> createListLoader(
            JSqlClient sqlClient,
            ImmutableProp prop
    ) {
        if (!prop.isReferenceList(TargetLevel.ENTITY)) {
            throw new IllegalArgumentException(
                    "Cannot create list loader for \"" + prop + "\", it is not entity list association"
            );
        }
        return new ListLoaderImpl<>(sqlClient, prop);
    }
}
