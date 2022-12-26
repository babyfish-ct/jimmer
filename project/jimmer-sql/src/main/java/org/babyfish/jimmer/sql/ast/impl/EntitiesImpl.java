package org.babyfish.jimmer.sql.ast.impl;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.TypedProp;
import org.babyfish.jimmer.runtime.DraftSpi;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.runtime.Internal;
import org.babyfish.jimmer.sql.Entities;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.impl.mutation.BatchEntitySaveCommandImpl;
import org.babyfish.jimmer.sql.ast.impl.mutation.DeleteCommandImpl;
import org.babyfish.jimmer.sql.ast.impl.mutation.SimpleEntitySaveCommandImpl;
import org.babyfish.jimmer.sql.ast.impl.query.MutableRootQueryImpl;
import org.babyfish.jimmer.sql.ast.impl.query.Queries;
import org.babyfish.jimmer.sql.ast.mutation.*;
import org.babyfish.jimmer.sql.ast.query.ConfigurableRootQuery;
import org.babyfish.jimmer.sql.ast.query.Example;
import org.babyfish.jimmer.sql.ast.query.Order;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.cache.Cache;
import org.babyfish.jimmer.sql.cache.CacheEnvironment;
import org.babyfish.jimmer.sql.cache.CacheLoader;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.babyfish.jimmer.sql.fetcher.impl.FetcherSelection;
import org.babyfish.jimmer.sql.fetcher.impl.Fetchers;
import org.babyfish.jimmer.sql.runtime.Converters;
import org.babyfish.jimmer.sql.runtime.ExecutionPurpose;

import java.sql.Connection;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class EntitiesImpl implements Entities {

    private final JSqlClient sqlClient;

    private final boolean forUpdate;

    private final Connection con;

    public EntitiesImpl(JSqlClient sqlClient) {
        this(sqlClient, false, null);
    }

    public EntitiesImpl(JSqlClient sqlClient, boolean forUpdate, Connection con) {
        this.sqlClient = sqlClient;
        this.forUpdate = forUpdate;
        this.con = con;
    }

    public JSqlClient getSqlClient() {
        return sqlClient;
    }

    public Connection getCon() {
        return con;
    }

    public EntitiesImpl forSqlClient(JSqlClient sqlClient) {
        if (this.sqlClient == sqlClient) {
            return this;
        }
        return new EntitiesImpl(sqlClient, forUpdate, con);
    }

    @Override
    public Entities forUpdate() {
        if (forUpdate) {
            return this;
        }
        return new EntitiesImpl(sqlClient, true, con);
    }

    @Override
    public Entities forConnection(Connection con) {
        if (this.con == con) {
            return this;
        }
        return new EntitiesImpl(sqlClient, forUpdate, con);
    }

    @Override
    public <E> E findById(Class<E> entityType, Object id) {
        if (con != null) {
            return findById(entityType, id, con);
        }
        return sqlClient.getConnectionManager().execute(con ->
                findById(entityType, id, con)
        );
    }

    @Override
    public <E> List<E> findByIds(Class<E> entityType, Collection<?> ids) {
        if (con != null) {
            return findByIds(entityType, ids, con);
        }
        return sqlClient.getConnectionManager().execute(con ->
                findByIds(entityType, ids, con)
        );
    }

    @Override
    public <ID, E> Map<ID, E> findMapByIds(Class<E> entityType, Collection<ID> ids) {
        if (con != null) {
            return findMapByIds(entityType, ids, con);
        }
        return sqlClient.getConnectionManager().execute(con ->
                findMapByIds(entityType, ids, con)
        );
    }

    @Override
    public <E> E findById(Fetcher<E> fetcher, Object id) {
        if (con != null) {
            return findById(fetcher, id, con);
        }
        return sqlClient.getConnectionManager().execute(con ->
                findById(fetcher, id, con)
        );
    }

    @Override
    public <E> List<E> findByIds(Fetcher<E> fetcher, Collection<?> ids) {
        if (con != null) {
            return findByIds(fetcher, ids, con);
        }
        return sqlClient.getConnectionManager().execute(con ->
                findByIds(fetcher, ids, con)
        );
    }

    @Override
    public <ID, E> Map<ID, E> findMapByIds(Fetcher<E> fetcher, Collection<ID> ids) {
        if (con != null) {
            return findMapByIds(fetcher, ids, con);
        }
        return sqlClient.getConnectionManager().execute(con ->
                findMapByIds(fetcher, ids, con)
        );
    }

    private <E> E findById(Class<E> entityType, Object id, Connection con) {
        if (id instanceof Collection<?>) {
            throw new IllegalArgumentException(
                    "id cannot be collection, do you want to call 'findByIds'?"
            );
        }
        List<E> rows = findByIds(entityType, null, Collections.singleton(id), con);
        return rows.isEmpty() ? null : rows.get(0);
    }

    private <E> List<E> findByIds(Class<E> entityType, Collection<?> ids, Connection con) {
        return findByIds(entityType, null, ids, con);
    }

    @SuppressWarnings("unchecked")
    private <ID, E> Map<ID, E> findMapByIds(Class<E> entityType, Collection<ID> ids, Connection con) {
        ImmutableProp idProp = ImmutableType.get(entityType).getIdProp();
        return this.findByIds(entityType, null, ids, con).stream().collect(
                Collectors.toMap(
                        it -> (ID)((ImmutableSpi) it).__get(idProp.getId()),
                        Function.identity(),
                        (a, b) -> { throw new IllegalStateException("Objects with same id"); },
                        LinkedHashMap::new
                )
        );
    }

    private <E> E findById(Fetcher<E> fetcher, Object id, Connection con) {
        if (id instanceof Collection<?>) {
            throw new IllegalArgumentException(
                    "id cannot be collection, do you want to call 'findByIds'?"
            );
        }
        List<E> rows = findByIds(fetcher.getJavaClass(), fetcher, Collections.singleton(id), con);
        return rows.isEmpty() ? null : rows.get(0);
    }

    private <E> List<E> findByIds(Fetcher<E> fetcher, Collection<?> ids, Connection con) {
        return findByIds(fetcher.getJavaClass(), fetcher, ids, con);
    }

    @SuppressWarnings("unchecked")
    private <ID, E> Map<ID, E> findMapByIds(Fetcher<E> fetcher, Collection<ID> ids, Connection con) {
        ImmutableProp idProp = ImmutableType.get(fetcher.getJavaClass()).getIdProp();
        return this.findByIds(fetcher.getJavaClass(), fetcher, ids, con).stream().collect(
                Collectors.toMap(
                        it -> (ID)((ImmutableSpi) it).__get(idProp.getId()),
                        Function.identity(),
                        (a, b) -> { throw new IllegalStateException("Objects with same id"); },
                        LinkedHashMap::new
                )
        );
    }

    @SuppressWarnings("unchecked")
    private <E> List<E> findByIds(
            Class<E> entityType,
            Fetcher<E> fetcher,
            Collection<?> ids,
            Connection con
    ) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }

        Set<Object> distinctIds;
        if (ids instanceof Set<?>) {
            distinctIds = (Set<Object>) ids;
        } else {
            distinctIds = new LinkedHashSet<>(ids);
        }

        ImmutableType immutableType = ImmutableType.get(entityType);
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
            List<E> entities = new ArrayList<>(
                    cache.getAll(
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
                    ).values()
            );
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
                                        if (spi.__isLoaded(prop.getId()) && !fetcher.getFieldMap().containsKey(prop.getName())) {
                                            ((DraftSpi) draft).__unload(prop.getId());
                                        }
                                    }
                                })
                        );
                    }
                }
                Fetchers.fetch(
                        sqlClient,
                        con,
                        Collections.singletonList(
                                new FetcherSelection<E>() {
                                    @Override
                                    public Fetcher<E> getFetcher() {
                                        return fetcher;
                                    }
                                }
                        ),
                        entities
                );
            }
            return entities;
        }
        ConfigurableRootQuery<?, E> query = Queries.createQuery(
                sqlClient, immutableType, ExecutionPurpose.QUERY, true, (q, table) -> {
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
            query = query.forUpdate();
        }
        return query.execute(con);
    }

    @Override
    public <E> List<E> findAll(Class<E> type, TypedProp.Scalar<?, ?>... sortedProps) {
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
                new MutableRootQueryImpl<>(sqlClient, type, ExecutionPurpose.QUERY, false);
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
        query.freeze();
        return query.select(
                fetcher != null ? table.fetch(fetcher) : table
        ).execute(con);
    }

    @Override
    public <E> SimpleEntitySaveCommand<E> saveCommand(E entity) {
        if (entity instanceof Collection<?>) {
            throw new IllegalArgumentException("entity cannot be collection, do you want to call 'batchSaveCommand'?");
        }
        return new SimpleEntitySaveCommandImpl<>(sqlClient, con, entity);
    }

    @Override
    public <E> BatchEntitySaveCommand<E> batchSaveCommand(Collection<E> entities) {
        return new BatchEntitySaveCommandImpl<>(sqlClient, con, entities);
    }

    @Override
    public DeleteCommand deleteCommand(
            Class<?> entityType,
            Object id
    ) {
        if (id instanceof Collection<?>) {
            throw new IllegalArgumentException("id cannot be collection, do you want to call 'batchDeleteCommand'?");
        }
        return batchDeleteCommand(entityType, Collections.singleton(id));
    }

    @Override
    public DeleteCommand batchDeleteCommand(
            Class<?> entityType,
            Collection<?> ids
    ) {
        ImmutableType immutableType = ImmutableType.get(entityType);
        return new DeleteCommandImpl(sqlClient, con, immutableType, ids);
    }
}
