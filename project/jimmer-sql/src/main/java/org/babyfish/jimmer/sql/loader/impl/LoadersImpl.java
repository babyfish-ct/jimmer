package org.babyfish.jimmer.sql.loader.impl;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.TargetLevel;
import org.babyfish.jimmer.meta.TypedProp;
import org.babyfish.jimmer.sql.ImmutableProps;
import org.babyfish.jimmer.sql.loader.ListLoader;
import org.babyfish.jimmer.sql.loader.Loaders;
import org.babyfish.jimmer.sql.loader.ReferenceLoader;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.loader.ValueLoader;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class LoadersImpl implements Loaders {

    private final JSqlClient sqlClient;

    public LoadersImpl(JSqlClient sqlClient) {
        this.sqlClient = sqlClient;
    }

    @Override
    public <S, T> Map<S, T> batchLoad(Collection<S> sources, TypedProp.Scalar<S, T> prop) {
        return value(prop).batchLoad(sources);
    }

    @Override
    public <S, T> Map<S, T> batchLoad(Collection<S> sources, TypedProp.Reference<S, T> prop) {
        return this.<S, T, Table<T>>reference(prop.unwrap()).batchLoad(sources);
    }

    @Override
    public <S, T> Map<S, List<T>> batchLoad(Collection<S> sources, TypedProp.ReferenceList<S, T> prop) {
        return this.<S, T, Table<T>>list(prop.unwrap()).batchLoad(sources);
    }

    @Override
    public <S, T> ValueLoader<S, T> value(TypedProp.Scalar<S, T> prop) {
        return value(prop.unwrap());
    }

    @Override
    public <SE, ST extends Table<SE>, TE, TT extends Table<TE>> ReferenceLoader<SE, TE, TT> reference(
            Class<ST> sourceTableType,
            Function<ST, TT> block
    ) {
        return reference(ImmutableProps.join(sourceTableType, block));
    }

    @Override
    public <SE, ST extends Table<SE>, TE, TT extends Table<TE>> ListLoader<SE, TE, TT> list(
            Class<ST> sourceTableType,
            Function<ST, TT> block
    ) {
        return list(ImmutableProps.join(sourceTableType, block));
    }

    public <S, T> ValueLoader<S, T> value(ImmutableProp prop) {
        if (!prop.hasTransientResolver()) {
            throw new IllegalArgumentException(
                    "Cannot create reference loader for \"" + prop + "\", it is not transient property with resolver"
            );
        }
        return new ValueLoaderImpl<>(sqlClient, prop);
    }

    public <SE, TE, TT extends Table<TE>> ReferenceLoader<SE, TE, TT> reference(ImmutableProp prop) {
        if (!prop.isReference(TargetLevel.ENTITY)) {
            throw new IllegalArgumentException(
                    "Cannot create reference loader for \"" + prop + "\", it is not entity reference association"
            );
        }
        return new ReferenceLoaderImpl<>(sqlClient, prop);
    }

    public <SE, TE, TT extends Table<TE>> ListLoader<SE, TE, TT> list(ImmutableProp prop) {
        if (!prop.isReferenceList(TargetLevel.ENTITY)) {
            throw new IllegalArgumentException(
                    "Cannot create list loader for \"" + prop + "\", it is not entity list association"
            );
        }
        return new ListLoaderImpl<>(sqlClient, prop);
    }
}
