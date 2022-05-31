package org.babyfish.jimmer.sql.ast.impl.mutation;

import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.sql.Entities;
import org.babyfish.jimmer.sql.SqlClient;
import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.Selection;
import org.babyfish.jimmer.sql.ast.impl.query.Queries;
import org.babyfish.jimmer.sql.ast.impl.table.TableImplementor;
import org.babyfish.jimmer.sql.ast.mutation.BatchEntitySaveCommand;
import org.babyfish.jimmer.sql.ast.mutation.DeleteCommand;
import org.babyfish.jimmer.sql.ast.mutation.SimpleEntitySaveCommand;
import org.babyfish.jimmer.sql.ast.query.TypedRootQuery;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.fetcher.Fetcher;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public class EntitiesImpl implements Entities {

    private SqlClient sqlClient;

    public EntitiesImpl(SqlClient sqlClient) {
        this.sqlClient = sqlClient;
    }

    @Override
    public <E> E findById(Class<E> entityType, Object id) {
        if (id instanceof Collection<?>) {
            throw new IllegalArgumentException(
                    "id cannot be collection, do you want to call 'findByIds'?"
            );
        }
        List<E> rows = findByIds(entityType, null, Collections.singleton(id));
        return rows.isEmpty() ? null : rows.get(0);
    }

    @Override
    public <E> List<E> findByIds(Class<E> entityType, Collection<Object> ids) {
        return findByIds(entityType, null, ids);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <ID, E> Map<ID, E> findMapByIds(Class<E> entityType, Collection<ID> ids) {
        String idPropName = ImmutableType.get(entityType).getIdProp().getName();
        return this.findByIds(entityType, null, ids).stream().collect(
                Collectors.toMap(
                        it -> (ID)((ImmutableSpi) it).__get(idPropName),
                        Function.identity(),
                        (a, b) -> { throw new IllegalStateException("Objects with same id"); },
                        LinkedHashMap::new
                )
        );
    }

    @Override
    public <E> E findById(Fetcher<E> fetcher, Object id) {
        if (id instanceof Collection<?>) {
            throw new IllegalArgumentException(
                    "id cannot be collection, do you want to call 'findByIds'?"
            );
        }
        List<E> rows = findByIds(fetcher.getJavaClass(), fetcher, Collections.singleton(id));
        return rows.isEmpty() ? null : rows.get(0);
    }

    @Override
    public <E> List<E> findByIds(Fetcher<E> fetcher, Collection<Object> ids) {
        return findByIds(fetcher.getJavaClass(), fetcher, ids);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <ID, E> Map<ID, E> findMapByIds(Fetcher<E> fetcher, Collection<ID> ids) {
        String idPropName = ImmutableType.get(fetcher.getJavaClass()).getIdProp().getName();
        return this.findByIds(fetcher.getJavaClass(), fetcher, ids).stream().collect(
                Collectors.toMap(
                        it -> (ID)((ImmutableSpi) it).__get(idPropName),
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
            Collection<?> ids
    ) {
        if (ids.isEmpty()) {
            return Collections.emptyList();
        }
        ImmutableType immutableType = ImmutableType.get(entityType);
        return Queries
                .createQuery(
                        sqlClient, immutableType, (q, table) -> {
                            Expression<Object> idProp = table.get(immutableType.getIdProp().getName());
                            if (ids.size() == 1) {
                                q.where(idProp.eq(ids.iterator().next()));
                            } else {
                                q.where(idProp.in((Collection<Object>) ids));
                            }
                            return q.select(((Table<E>) table).fetch(fetcher));
                        }
                )
                .execute();
    }

    @Override
    public <E> SimpleEntitySaveCommand<E> saveCommand(E entity) {
        if (entity instanceof Collection<?>) {
            throw new IllegalArgumentException("entity cannot be collection, do you want to call 'batchSaveCommand'?");
        }
        return new SimpleEntitySaveCommandImpl<>(sqlClient, entity);
    }

    @Override
    public <E> BatchEntitySaveCommand<E> batchSaveCommand(Collection<E> entities) {
        return new BatchEntitySaveCommandImpl<>(sqlClient, entities);
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
        return new DeleteCommandImpl(sqlClient, immutableType, ids);
    }
}