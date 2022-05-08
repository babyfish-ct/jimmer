package org.babyfish.jimmer.sql.ast.impl;

import org.apache.commons.lang3.reflect.TypeUtils;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.sql.SqlClient;
import org.babyfish.jimmer.sql.ast.query.ConfigurableTypedRootQuery;
import org.babyfish.jimmer.sql.ast.query.MutableRootQuery;
import org.babyfish.jimmer.sql.ast.table.SubQueryTable;
import org.babyfish.jimmer.sql.ast.table.Table;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.BiFunction;

public class Queries {

    private static Map<Class<? extends Table<?>>, ImmutableType> cacheMap = new WeakHashMap<>();

    private static ReadWriteLock cacheLock = new ReentrantReadWriteLock();

    public static <T extends Table<?>, R> ConfigurableTypedRootQuery<R> createQuery(
            Class<T> tableType,
            SqlClient sqlClient,
            BiFunction<MutableRootQuery, T, ConfigurableTypedRootQuery<R>> block
    ) {
        RootMutableQueryImpl query = new RootMutableQueryImpl(
                sqlClient,
                getImmutableType(tableType)
        );
        return block.apply(query, (T)query.getTable());
    }

    private static ImmutableType getImmutableType(Class<? extends Table<?>> tableType) {

        ImmutableType immutableType;
        Lock lock;

        (lock = cacheLock.readLock()).lock();
        try {
            immutableType = cacheMap.get(tableType);
        } finally {
            lock.unlock();
        }

        if (immutableType == null) {
            (lock = cacheLock.writeLock()).lock();
            try {
                immutableType = cacheMap.get(tableType);
                if (immutableType == null) {
                    immutableType = createImmutableType(tableType);
                    cacheMap.put(tableType, immutableType);
                }
            } finally {
                lock.unlock();
            }
        }
        return immutableType;
    }

    private static ImmutableType createImmutableType(Class<? extends Table<?>> tableType) {
        if (tableType.getTypeParameters().length != 0) {
            throw new IllegalArgumentException(
                    "Cannot create query base on table type with type parameters"
            );
        }
        Type type = TypeUtils
                .getTypeArguments(tableType, Table.class)
                .values()
                .iterator()
                .next();
        if (type instanceof Class<?>) {
            return ImmutableType.tryGet((Class<?>) type);
        }
        throw new IllegalArgumentException(
                "Cannot get immutable type from table type \"" +
                        tableType.getName() +
                        "\""
        );
    }
}
