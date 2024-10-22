package org.babyfish.jimmer.sql.ast.impl;

import org.babyfish.jimmer.Input;
import org.babyfish.jimmer.View;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.PropId;
import org.babyfish.jimmer.meta.TypedProp;
import org.babyfish.jimmer.runtime.DraftSpi;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.runtime.Internal;
import org.babyfish.jimmer.sql.Entities;
import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.PropExpression;
import org.babyfish.jimmer.sql.ast.impl.mutation.BatchEntitySaveCommandImpl;
import org.babyfish.jimmer.sql.ast.impl.mutation.DeleteCommandImpl;
import org.babyfish.jimmer.sql.ast.impl.mutation.SimpleEntitySaveCommandImpl;
import org.babyfish.jimmer.sql.ast.impl.query.FilterLevel;
import org.babyfish.jimmer.sql.ast.impl.query.MutableRootQueryImpl;
import org.babyfish.jimmer.sql.ast.impl.query.Queries;
import org.babyfish.jimmer.sql.ast.impl.table.FetcherSelectionImpl;
import org.babyfish.jimmer.sql.ast.mutation.BatchEntitySaveCommand;
import org.babyfish.jimmer.sql.ast.mutation.DeleteCommand;
import org.babyfish.jimmer.sql.ast.mutation.SimpleEntitySaveCommand;
import org.babyfish.jimmer.sql.ast.query.ConfigurableRootQuery;
import org.babyfish.jimmer.sql.ast.query.Example;
import org.babyfish.jimmer.sql.ast.query.Order;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.cache.Cache;
import org.babyfish.jimmer.sql.cache.CacheEnvironment;
import org.babyfish.jimmer.sql.cache.CacheLoader;
import org.babyfish.jimmer.sql.exception.EmptyResultException;
import org.babyfish.jimmer.sql.fetcher.DtoMetadata;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.babyfish.jimmer.sql.fetcher.impl.FetchPath;
import org.babyfish.jimmer.sql.fetcher.impl.FetcherSelection;
import org.babyfish.jimmer.sql.fetcher.impl.FetcherUtil;
import org.babyfish.jimmer.sql.runtime.Converters;
import org.babyfish.jimmer.sql.runtime.ExecutionPurpose;
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.util.*;
import java.util.function.Function;

public class EntitiesImpl implements Entities {

    private final JSqlClientImplementor sqlClient;

    private final boolean forUpdate;

    private final Connection con;

    private final ExecutionPurpose purpose;

    public EntitiesImpl(JSqlClientImplementor sqlClient) {
        this(sqlClient, false, null, ExecutionPurpose.QUERY);
    }

    public EntitiesImpl(JSqlClientImplementor sqlClient, boolean forUpdate, Connection con, ExecutionPurpose purpose) {
        this.sqlClient = sqlClient;
        this.forUpdate = forUpdate;
        this.con = con;
        this.purpose = purpose;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static ImmutableType immutableTypeOf(Class<?> type) {
        if (View.class.isAssignableFrom(type)) {
            return DtoMetadata.of((Class<? extends View>) type).getFetcher().getImmutableType();
        }
        return ImmutableType.get(type);
    }

    public JSqlClientImplementor getSqlClient() {
        return sqlClient;
    }

    public Connection getCon() {
        return con;
    }

    public EntitiesImpl forSqlClient(JSqlClientImplementor sqlClient) {
        if (this.sqlClient == sqlClient) {
            return this;
        }
        return new EntitiesImpl(sqlClient, forUpdate, con, purpose);
    }

    @Override
    public Entities forUpdate() {
        if (forUpdate) {
            return this;
        }
        return new EntitiesImpl(sqlClient, true, con, purpose);
    }

    @Override
    public Entities forConnection(Connection con) {
        if (this.con == con) {
            return this;
        }
        return new EntitiesImpl(sqlClient, forUpdate, con, purpose);
    }

    public Entities forLoader() {
        if (purpose == ExecutionPurpose.LOAD) {
            return this;
        }
        return new EntitiesImpl(sqlClient, forUpdate, con, ExecutionPurpose.LOAD);
    }

    public Entities forExporter() {
        if (purpose == ExecutionPurpose.EXPORT) {
            return this;
        }
        return new EntitiesImpl(sqlClient, forUpdate, con, ExecutionPurpose.EXPORT);
    }

    @Override
    public <E> E findById(Class<E> type, Object id) {
        return sqlClient.getConnectionManager().execute(con, con -> findById(type, id, con));
    }

    @NotNull
    @Override
    public <T> T findOneById(Class<T> type, Object id) {
        T result = findById(type, id);
        if (result == null) {
            throw new EmptyResultException();
        }
        return result;
    }

    @Override
    public <T> List<T> findByIds(Class<T> type, Iterable<?> ids) {
        return sqlClient.getConnectionManager().execute(con, con -> findByIds(type, ids, con));
    }

    @Override
    public <ID, T> Map<ID, T> findMapByIds(Class<T> type, Iterable<ID> ids) {
        return sqlClient.getConnectionManager().execute(con, con -> findMapByIds(type, ids, con));
    }

    @Override
    public <E> E findById(Fetcher<E> fetcher, Object id) {
        return sqlClient.getConnectionManager().execute(con, con -> findById(fetcher, id, con));
    }

    @NotNull
    @Override
    public <E> E findOneById(Fetcher<E> fetcher, Object id) {
        E result = findById(fetcher, id);
        if (result == null) {
            throw new EmptyResultException();
        }
        return result;
    }

    @Override
    public <E> List<E> findByIds(Fetcher<E> fetcher, Iterable<?> ids) {
        return sqlClient.getConnectionManager().execute(con, con -> findByIds(fetcher, ids, con));
    }

    @Override
    public <ID, T> Map<ID, T> findMapByIds(Fetcher<T> fetcher, Iterable<ID> ids) {
        return sqlClient.getConnectionManager().execute(con, con -> findMapByIds(fetcher, ids, con));
    }

    private <T> T findById(Class<T> type, Object id, Connection con) {
        if (id instanceof Iterable<?>) {
            throw new IllegalArgumentException(
                    "id cannot be collection, do you want to call 'findByIds'?"
            );
        }
        List<T> rows = findByIds(type, null, Collections.singleton(id), con);
        return rows.isEmpty() ? null : rows.get(0);
    }

    private <T> List<T> findByIds(Class<T> type, Iterable<?> ids, Connection con) {
        return findByIds(type, null, ids, con);
    }

    @SuppressWarnings("unchecked")
    private <ID, T> Map<ID, T> findMapByIds(Class<T> type, Iterable<ID> ids, Connection con) {
        PropId idPropId = immutableTypeOf(type).getIdProp().getId();
        List<T> entities = findByIds(type, null, ids, con);
        Map<ID, T> map = new LinkedHashMap<>((entities.size() * 4 + 2) / 3);
        for (T entity : entities) {
            if (View.class.isAssignableFrom(type)) {
                map.put((ID) ((ImmutableSpi) (((View<?>) entity).toEntity())).__get(idPropId), entity);
            } else {
                map.put((ID) ((ImmutableSpi) entity).__get(idPropId), entity);
            }
        }
        return map;
    }

    private <E> E findById(Fetcher<E> fetcher, Object id, Connection con) {
        if (id instanceof Iterable<?>) {
            throw new IllegalArgumentException(
                    "id cannot be collection, do you want to call 'findByIds'?"
            );
        }
        List<E> rows = findByIds(fetcher.getJavaClass(), fetcher, Collections.singleton(id), con);
        return rows.isEmpty() ? null : rows.get(0);
    }

    private <E> List<E> findByIds(Fetcher<E> fetcher, Iterable<?> ids, Connection con) {
        return findByIds(fetcher.getJavaClass(), fetcher, ids, con);
    }

    @SuppressWarnings("unchecked")
    private <ID, E> Map<ID, E> findMapByIds(Fetcher<E> fetcher, Iterable<ID> ids, Connection con) {
        ImmutableType type = fetcher.getImmutableType();
        PropId idPropId = type.getIdProp().getId();
        List<E> entities = findByIds((Class<E>) type.getJavaClass(), fetcher, ids, con);
        Map<ID, E> map = new LinkedHashMap<>((entities.size() * 4 + 2) / 3);
        for (E entity : entities) {
            map.put((ID) ((ImmutableSpi) entity).__get(idPropId), entity);
        }
        return map;
    }

    @SuppressWarnings("unchecked")
    private <E> List<E> findByIds(
            Class<E> type,
            Fetcher<E> fetcher,
            Iterable<?> ids,
            Connection con
    ) {
        Set<Object> distinctIds = distinctIds(ids);
        if (distinctIds.isEmpty()) {
            return Collections.emptyList();
        }

        if (View.class.isAssignableFrom(type)) {
            return findByIds(DtoMetadata.of((Class<? extends View<Object>>) type), ids, con);
        }

        ImmutableType immutableType = ImmutableType.get(type);
        Class<?> idClass = immutableType.getIdProp().getElementClass();
        for (Object id : distinctIds) {
            if (Converters.tryConvert(id, idClass) == null) {
                throw new IllegalArgumentException(
                        "The type of \"" +
                                immutableType.getIdProp() +
                                "\" must be \"" +
                                idClass.getName() +
                                "\", but the actual type is \"" +
                                id.getClass().getName() +
                                "\""
                );
            }
        }
        Cache<Object, E> cache = sqlClient.getCaches().getObjectCache(immutableType);
        if (cache != null) {
            Collection<E> cachedEntities = cache.getAll(
                    distinctIds,
                    new CacheEnvironment<>(
                            sqlClient,
                            con,
                            CacheLoader.objectLoader(
                                    sqlClient,
                                    con,
                                    (Class<E>) immutableType.getJavaClass()
                            ),
                            true
                    )
            ).values();
            List<E> entities = new ArrayList<>(cachedEntities.size());
            for (E entity : cachedEntities) {
                if (entity != null) {
                    entities.add(entity);
                }
            }
            if (fetcher != null && !entities.isEmpty()) {
                boolean needUnload = false;
                for (ImmutableSpi spi : (List<ImmutableSpi>) entities) {
                    for (ImmutableProp prop : immutableType.getProps().values()) {
                        if (spi.__isLoaded(prop.getId()) && !fetcher.getFieldMap().containsKey(prop.getName())) {
                            needUnload = true;
                            break;
                        }
                    }
                }
                if (needUnload) {
                    ListIterator<ImmutableSpi> itr = (ListIterator<ImmutableSpi>) entities.listIterator();
                    while (itr.hasNext()) {
                        ImmutableSpi spi = itr.next();
                        itr.set(
                                (ImmutableSpi) Internal.produce(immutableType, spi, draft -> {
                                    for (ImmutableProp prop : immutableType.getProps().values()) {
                                        if (!prop.isView() &&
                                            spi.__isLoaded(prop.getId()) &&
                                            !fetcher.getFieldMap().containsKey(prop.getName())) {
                                            ((DraftSpi) draft).__unload(prop.getId());
                                        }
                                    }
                                })
                        );
                    }
                }
                FetcherUtil.fetch(
                        sqlClient,
                        con,
                        Collections.singletonList(
                                new FetcherSelection<E>() {

                                    @Override
                                    public FetchPath getPath() {
                                        return null;
                                    }

                                    @Override
                                    public Fetcher<?> getFetcher() {
                                        return fetcher;
                                    }

                                    @Override
                                    public PropExpression.Embedded<?> getEmbeddedPropExpression() {
                                        return null;
                                    }

                                    @Override
                                    public @Nullable Function<?, E> getConverter() {
                                        return null;
                                    }
                                }
                        ),
                        entities
                );
            }
            return entities;
        }
        ConfigurableRootQuery<?, E> query = Queries.createQuery(
                sqlClient, immutableType, purpose, FilterLevel.DEFAULT, (q, table) -> {
                    Expression<Object> idProp = table.get(immutableType.getIdProp().getName());
                    if (distinctIds.size() == 1) {
                        q.where(idProp.eq(distinctIds.iterator().next()));
                    } else {
                        q.where(idProp.in(distinctIds));
                    }
                    return q.select(((Table<E>) table).fetch(fetcher));
                }
        );
        if (forUpdate) {
            query = query.forUpdate(true);
        }
        return query.execute(con);
    }

    @SuppressWarnings("unchecked")
    private <E> List<E> findByIds(
            DtoMetadata<?, ?> metadata,
            Iterable<?> ids,
            Connection con
    ) {
        Set<Object> distinctIds = distinctIds(ids);
        if (distinctIds.isEmpty()) {
            return Collections.emptyList();
        }

        Fetcher<?> fetcher = metadata.getFetcher();
        Function<?, E> converter = (Function<?, E>) metadata.getConverter();
        ImmutableType immutableType = metadata.getFetcher().getImmutableType();
        Class<?> idClass = immutableType.getIdProp().getElementClass();
        for (Object id : distinctIds) {
            if (Converters.tryConvert(id, idClass) == null) {
                throw new IllegalArgumentException(
                        "The type of \"" +
                        immutableType.getIdProp() +
                        "\" must be \"" +
                        idClass.getName() +
                        "\""
                );
            }
        }
        Cache<Object, E> cache = sqlClient.getCaches().getObjectCache(immutableType);
        if (cache != null) {
            Collection<E> cachedEntities = cache.getAll(
                    distinctIds,
                    new CacheEnvironment<>(
                            sqlClient,
                            con,
                            CacheLoader.objectLoader(
                                    sqlClient,
                                    con,
                                    (Class<E>) immutableType.getJavaClass()
                            ),
                            true
                    )
            ).values();
            List<E> entities = new ArrayList<>(cachedEntities.size());
            for (E entity : cachedEntities) {
                if (entity != null) {
                    entities.add(entity);
                }
            }
            if (!entities.isEmpty()) {
                boolean needUnload = false;
                for (ImmutableSpi spi : (List<ImmutableSpi>) entities) {
                    for (ImmutableProp prop : immutableType.getProps().values()) {
                        if (spi.__isLoaded(prop.getId()) && !fetcher.getFieldMap().containsKey(prop.getName())) {
                            needUnload = true;
                            break;
                        }
                    }
                }
                if (needUnload) {
                    ListIterator<ImmutableSpi> itr = (ListIterator<ImmutableSpi>) entities.listIterator();
                    while (itr.hasNext()) {
                        ImmutableSpi spi = itr.next();
                        itr.set(
                                (ImmutableSpi) Internal.produce(immutableType, spi, draft -> {
                                    for (ImmutableProp prop : immutableType.getProps().values()) {
                                        if (!prop.isView() &&
                                            spi.__isLoaded(prop.getId()) &&
                                            !fetcher.getFieldMap().containsKey(prop.getName())) {
                                            ((DraftSpi) draft).__unload(prop.getId());
                                        }
                                    }
                                })
                        );
                    }
                }
                FetcherUtil.fetch(
                        sqlClient,
                        con,
                        Collections.singletonList(
                                new FetcherSelection<E>() {

                                    @Override
                                    public FetchPath getPath() {
                                        return null;
                                    }

                                    @Override
                                    public Fetcher<?> getFetcher() {
                                        return fetcher;
                                    }

                                    @Override
                                    public PropExpression.Embedded<?> getEmbeddedPropExpression() {
                                        return null;
                                    }

                                    @Override
                                    public @Nullable Function<?, E> getConverter() {
                                        return converter;
                                    }
                                }
                        ),
                        entities
                );
            }
            return entities;
        }
        ConfigurableRootQuery<?, E> query = Queries.createQuery(
                sqlClient, immutableType, purpose, FilterLevel.DEFAULT, (q, table) -> {
                    Expression<Object> idProp = table.get(immutableType.getIdProp().getName());
                    if (distinctIds.size() == 1) {
                        q.where(idProp.eq(distinctIds.iterator().next()));
                    } else {
                        q.where(idProp.in(distinctIds));
                    }
                    return q.select(new FetcherSelectionImpl<>(table, fetcher, converter));
                }
        );
        if (forUpdate) {
            query = query.forUpdate(true);
        }
        return query.execute(con);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> List<T> findAll(Class<T> type) {
        if (View.class.isAssignableFrom(type)) {
            return find(DtoMetadata.of((Class<View<Object>>) type), null);
        }
        return find(ImmutableType.get(type), null, null);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> List<T> findAll(Class<T> type, TypedProp.Scalar<?, ?>... sortedProps) {
        if (View.class.isAssignableFrom(type)) {
            DtoMetadata<?, ?> metadata = DtoMetadata.of((Class<View<Object>>) type);
            return find(metadata, null, sortedProps);
        }
        return find(ImmutableType.get(type), null, null, sortedProps);
    }

    @Override
    public <E> List<E> findAll(Fetcher<E> fetcher, TypedProp.Scalar<?, ?>... sortedProps) {
        return find(fetcher.getImmutableType(), fetcher, null, sortedProps);
    }

    @Override
    public <E> List<E> findByExample(Example<E> example, TypedProp.Scalar<?, ?>... sortedProps) {
        ExampleImpl<E> exampleImpl = (ExampleImpl<E>) example;
        return find(exampleImpl.type(), null, exampleImpl, sortedProps);
    }

    @Override
    public <E> List<E> findByExample(Example<E> example, Fetcher<E> fetcher, TypedProp.Scalar<?, ?>... sortedProps) {
        ExampleImpl<E> exampleImpl = (ExampleImpl<E>) example;
        return find(exampleImpl.type(), fetcher, exampleImpl, sortedProps);
    }

    @Override
    public <E, V extends View<E>> List<V> findExample(Class<V> viewType, Example<E> example, TypedProp.Scalar<?, ?>... sortedProps) {
        return find(DtoMetadata.of(viewType), (ExampleImpl<E>) example, sortedProps);
    }

    private <E> List<E> find(
            ImmutableType type,
            Fetcher<E> fetcher,
            ExampleImpl<E> example,
            TypedProp.Scalar<?, ?>... sortedProps
    ) {
        if (fetcher != null && fetcher.getImmutableType() != type) {
            throw new IllegalArgumentException(
                    "The type of fetcher is \"" +
                    fetcher.getImmutableType() +
                    "\", it does not match the query root type \"" +
                    type +
                    "\""
            );
        }
        if (example != null && example.type() != type) {
            throw new IllegalArgumentException(
                    "The type of example is \"" +
                    example.type() +
                    "\", it does not match the query root type \"" +
                    type +
                    "\""
            );
        }
        MutableRootQueryImpl<Table<E>> query =
                new MutableRootQueryImpl<>(sqlClient, type, ExecutionPurpose.QUERY, FilterLevel.DEFAULT);
        Table<E> table = query.getTable();
        if (example != null) {
            example.applyTo(query);
        }
        for (TypedProp.Scalar<?, ?> sortedProp : sortedProps) {
            if (!sortedProp.unwrap().getDeclaringType().isAssignableFrom(type)) {
                throw new IllegalArgumentException(
                        "The sorted field \"" +
                        sortedProp +
                        "\" does not belong to the type \"" +
                        type +
                        "\" or its super types"
                );
            }
            Expression<?> expr = table.get(sortedProp.unwrap().getName());
            Order astOrder;
            if (sortedProp.isDesc()) {
                astOrder = expr.desc();
            } else {
                astOrder = expr.asc();
            }
            if (sortedProp.isNullsFirst()) {
                astOrder = astOrder.nullsFirst();
            }
            if (sortedProp.isNullsLast()) {
                astOrder = astOrder.nullsLast();
            }
            query.orderBy(astOrder);
        }
        return query.select(
                fetcher != null ? table.fetch(fetcher) : table
        ).execute(con);
    }

    @SuppressWarnings("unchecked")
    private <V> List<V> find(
            DtoMetadata<?, ?> metadata,
            ExampleImpl<?> example,
            TypedProp.Scalar<?, ?>... sortedProps
    ) {
        Fetcher<?> fetcher = metadata.getFetcher();
        Function<?, V> converter = (Function<?, V>) metadata.getConverter();
        ImmutableType type = fetcher.getImmutableType();
        MutableRootQueryImpl<Table<?>> query =
                new MutableRootQueryImpl<>(sqlClient, type, ExecutionPurpose.QUERY, FilterLevel.DEFAULT);
        if (example != null) {
            example.applyTo(query);
        }
        Table<?> table = query.getTable();
        for (TypedProp.Scalar<?, ?> sortedProp : sortedProps) {
            if (!sortedProp.unwrap().getDeclaringType().isAssignableFrom(type)) {
                throw new IllegalArgumentException(
                        "The sorted field \"" +
                        sortedProp +
                        "\" does not belong to the type \"" +
                        type +
                        "\" or its super types"
                );
            }
            Expression<?> expr = table.get(sortedProp.unwrap().getName());
            Order astOrder;
            if (sortedProp.isDesc()) {
                astOrder = expr.desc();
            } else {
                astOrder = expr.asc();
            }
            if (sortedProp.isNullsFirst()) {
                astOrder = astOrder.nullsFirst();
            }
            if (sortedProp.isNullsLast()) {
                astOrder = astOrder.nullsLast();
            }
            query.orderBy(astOrder);
        }
        return query.select(
                new FetcherSelectionImpl<>(table, fetcher, converter)
        ).execute(con);
    }

    @Override
    public <E> SimpleEntitySaveCommand<E> saveCommand(E entity) {
        if (entity instanceof Iterable<?>) {
            throw new IllegalArgumentException("entity cannot be collection, do you want to call `saveAll/saveAllCommand`?");
        }
        if (entity instanceof Input<?>) {
            throw new IllegalArgumentException(
                    "entity cannot be input, " +
                    "please call another overloaded function whose parameter is input"
            );
        }
        return new SimpleEntitySaveCommandImpl<>(sqlClient, con, entity);
    }

    @Override
    public <E> BatchEntitySaveCommand<E> saveEntitiesCommand(Iterable<E> entities) {
        for (E e : entities) {
            if (e instanceof Input<?>) {
                throw new IllegalArgumentException(
                        "the collection cannot contains input, " +
                        "please call another overloaded function `saveInputsCommand`"
                );
            }
        }
        return new BatchEntitySaveCommandImpl<>(sqlClient, con, entities);
    }

    @Override
    public DeleteCommand deleteCommand(
            Class<?> type,
            Object id
    ) {
        if (id instanceof Iterable<?>) {
            throw new IllegalArgumentException("`id` cannot be iterable, do you want to call `deleteAll/deleteAllCommand`?");
        }
        if ((id instanceof ImmutableSpi && ((ImmutableSpi) id).__type().isEntity()) || id instanceof Input<?>) {
            throw new IllegalArgumentException("`id` must be simple type");
        }
        return deleteAllCommand(type, Collections.singleton(id));
    }

    @Override
    public DeleteCommand deleteAllCommand(
            Class<?> type,
            Iterable<?> ids
    ) {
        for (Object id : ids) {
            if ((id instanceof ImmutableSpi && ((ImmutableSpi) id).__type().isEntity()) || id instanceof Input<?>) {
                throw new IllegalArgumentException("All the elements of `ids` must be simple type");
            }
        }
        ImmutableType immutableType = immutableTypeOf(type);
        return new DeleteCommandImpl(sqlClient, con, immutableType, ids);
    }

    @SuppressWarnings("unchecked")
    private static Set<Object> distinctIds(Iterable<?> values) {
        if (values == null) {
            return Collections.emptySet();
        }
        if (values instanceof Set<?> && !((Set<?>) values).contains(null)) {
            return (Set<Object>) values;
        }
        Set<Object> set;
        if (values instanceof Collection<?>) {
            Collection<?> c = (Collection<?>)values;
            if (c.isEmpty()) {
                return Collections.emptySet();
            }
            if (c instanceof Set<?> && !c.contains(null)) {
                return (Set<Object>)c;
            }
            set = new LinkedHashSet<>((c.size() * 4 + 2) / 3);
        } else {
            set = new LinkedHashSet<>();
        }
        for (Object value : values) {
            if (value != null) {
                set.add(value);
            }
        }
        return set;
    }
}

