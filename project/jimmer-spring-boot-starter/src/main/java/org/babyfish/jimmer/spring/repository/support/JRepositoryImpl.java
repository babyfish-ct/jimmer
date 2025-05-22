package org.babyfish.jimmer.spring.repository.support;

import org.babyfish.jimmer.ImmutableObjects;
import org.babyfish.jimmer.Input;
import org.babyfish.jimmer.View;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.TypedProp;
import org.babyfish.jimmer.spring.repository.JRepository;
import org.babyfish.jimmer.spring.repository.SpringOrders;
import org.babyfish.jimmer.sql.Entity;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.ast.PropExpression;
import org.babyfish.jimmer.sql.ast.Selection;
import org.babyfish.jimmer.sql.ast.impl.mutation.Mutations;
import org.babyfish.jimmer.sql.ast.impl.query.FilterLevel;
import org.babyfish.jimmer.sql.ast.impl.query.MutableRootQueryImpl;
import org.babyfish.jimmer.sql.ast.impl.table.FetcherSelectionImpl;
import org.babyfish.jimmer.sql.ast.impl.table.TableImplementor;
import org.babyfish.jimmer.sql.ast.mutation.*;
import org.babyfish.jimmer.sql.ast.query.ConfigurableRootQuery;
import org.babyfish.jimmer.sql.ast.query.Order;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.babyfish.jimmer.sql.fetcher.DtoMetadata;
import org.babyfish.jimmer.sql.runtime.ExecutionPurpose;
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.core.GenericTypeResolver;
import org.springframework.data.domain.*;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@NoRepositoryBean
public class JRepositoryImpl<E, ID> implements JRepository<E, ID> {

    protected final JSqlClientImplementor sqlClient;

    protected final Class<E> entityType;

    protected final ImmutableType immutableType;

    protected JRepositoryImpl(JSqlClient sqlClient) {
        this(sqlClient, null);
    }

    @SuppressWarnings("unchecked")
    public JRepositoryImpl(JSqlClient sqlClient, Class<E> entityType) {
        this.sqlClient = Utils.validateSqlClient(sqlClient);
        if (entityType != null) {
            this.entityType = entityType;
        } else {
            Class<?>[] typeArguments = GenericTypeResolver
                    .resolveTypeArguments(this.getClass(), JRepository.class);
            if (typeArguments == null) {
                throw new IllegalArgumentException(
                        "The class \"" + this.getClass() + "\" " +
                                "does not explicitly specify the type arguments of \"" +
                                JRepository.class.getName() +
                                "\" so that the entityType must be specified"
                );
            }
            this.entityType = entityType = (Class<E>) typeArguments[0];
        }
        this.immutableType = ImmutableType.get(entityType);
        if (!immutableType.isEntity()) {
            throw new IllegalArgumentException(
                    "\"" +
                            entityType +
                            "\" is not entity type decorated by @" +
                            Entity.class.getName()
            );
        }
    }

    @Override
    public JSqlClient sql() {
        return sqlClient;
    }

    @Override
    public ImmutableType type() {
        return immutableType;
    }

    @Override
    public Class<E> entityType() {
        return entityType;
    }

    @Override
    public E findNullable(ID id) {
        return sqlClient.getEntities().findById(entityType, id);
    }

    @Override
    public E findNullable(ID id, Fetcher<E> fetcher) {
        if (fetcher == null) {
            return findNullable(id);
        }
        return sqlClient.getEntities().findById(fetcher, id);
    }

    @Override
    public List<E> findByIds(Iterable<ID> ids) {
        return sqlClient.getEntities().findByIds(entityType, Utils.toCollection(ids));
    }

    @Override
    public List<E> findByIds(Iterable<ID> ids, Fetcher<E> fetcher) {
        if (fetcher == null) {
            return findByIds(ids);
        }
        return sqlClient.getEntities().findByIds(fetcher, Utils.toCollection(ids));
    }

    @Override
    public Map<ID, E> findMapByIds(Iterable<ID> ids) {
        return sqlClient.getEntities().findMapByIds(entityType, Utils.toCollection(ids));
    }

    @Override
    public Map<ID, E> findMapByIds(Iterable<ID> ids, Fetcher<E> fetcher) {
        if (fetcher == null) {
            return findMapByIds(ids);
        }
        return sqlClient.getEntities().findMapByIds(fetcher, Utils.toCollection(ids));
    }

    @NotNull
    @Override
    public List<E> findAll() {
        return createQuery(null, (Function<?, E>)null, null, null).execute();
    }

    @Override
    public List<E> findAll(TypedProp.Scalar<?, ?>... sortedProps) {
        return createQuery(null, (Function<?, E>)null, sortedProps, null).execute();
    }

    @Override
    public List<E> findAll(Fetcher<E> fetcher, TypedProp.Scalar<?, ?>... sortedProps) {
        return createQuery(fetcher, (Function<?, E>)null, sortedProps, null).execute();
    }

    @NotNull
    @Override
    public List<E> findAll(@NotNull Sort sort) {
        return createQuery(null, (Function<?, E>)null, null, sort).execute();
    }

    @Override
    public List<E> findAll(Fetcher<E> fetcher, Sort sort) {
        return createQuery(fetcher, (Function<?, E>)null, null, sort).execute();
    }

    @Override
    public Page<E> findAll(int pageIndex, int pageSize) {
        return this.<E>createQuery(null, null, null, null)
                .fetchPage(pageIndex, pageSize, SpringPageFactory.getInstance());
    }

    @Override
    public Page<E> findAll(int pageIndex, int pageSize, Fetcher<E> fetcher) {
        return this.<E>createQuery(fetcher, null, null, null)
                .fetchPage(pageIndex, pageSize, SpringPageFactory.getInstance());
    }

    @Override
    public Page<E> findAll(int pageIndex, int pageSize, TypedProp.Scalar<?, ?>... sortedProps) {
        return this.<E>createQuery(null, null, sortedProps, null)
                .fetchPage(pageIndex, pageSize, SpringPageFactory.getInstance());
    }

    @Override
    public Page<E> findAll(int pageIndex, int pageSize, Fetcher<E> fetcher, TypedProp.Scalar<?, ?>... sortedProps) {
        return this.<E>createQuery(fetcher, null, sortedProps, null)
                .fetchPage(pageIndex, pageSize, SpringPageFactory.getInstance());
    }

    @Override
    public Page<E> findAll(int pageIndex, int pageSize, Sort sort) {
        return this.<E>createQuery(null, null, null, sort)
                .fetchPage(pageIndex, pageSize, SpringPageFactory.getInstance());
    }

    @Override
    public Page<E> findAll(int pageIndex, int pageSize, Fetcher<E> fetcher, Sort sort) {
        return this.<E>createQuery(fetcher, null, null, sort)
                .fetchPage(pageIndex, pageSize, SpringPageFactory.getInstance());
    }

    @NotNull
    @Override
    public Page<E> findAll(@NotNull Pageable pageable) {
        return this.<E>createQuery(null, null, null, pageable.getSort())
                .fetchPage(pageable.getPageNumber(), pageable.getPageSize(), SpringPageFactory.getInstance());
    }

    @Override
    public Page<E> findAll(Pageable pageable, Fetcher<E> fetcher) {
        return this.<E>createQuery(fetcher, null, null, pageable.getSort())
                .fetchPage(pageable.getPageNumber(), pageable.getPageSize(), SpringPageFactory.getInstance());
    }

    @Override
    public long count() {
        return createQuery(null, null, null, null).fetchUnlimitedCount();
    }

    @NotNull
    @Override
    public SimpleEntitySaveCommand<E> saveCommand(@NotNull Input<E> input) {
        return sqlClient.getEntities().saveCommand(input);
    }

    @Override
    public int delete(@NotNull E entity, DeleteMode mode) {
        return sqlClient.getEntities().delete(
                entityType,
                ImmutableObjects.get(entity, immutableType.getIdProp().getId()),
                mode
        ).getAffectedRowCount(AffectedTable.of(immutableType));
    }

    @Override
    public int deleteAll(@NotNull Iterable<? extends E> entities, DeleteMode mode) {
        return sqlClient.getEntities().deleteAll(
                entityType,
                Utils
                        .toCollection(entities)
                        .stream()
                        .map(it -> ImmutableObjects.get(it, immutableType.getIdProp().getId()))
                        .collect(Collectors.toList()),
                mode
        ).getAffectedRowCount(AffectedTable.of(immutableType));
    }

    @Override
    public int deleteById(@NotNull ID id, DeleteMode mode) {
        return sqlClient
                .getEntities()
                .delete(entityType, id, mode)
                .getAffectedRowCount(AffectedTable.of(immutableType));
    }

    @Override
    public int deleteByIds(Iterable<? extends ID> ids, DeleteMode mode) {
        return sqlClient
                .getEntities()
                .deleteAll(entityType, Utils.toCollection(ids), mode)
                .getAffectedRowCount(AffectedTable.of(immutableType));
    }

    @Override
    public void deleteAll() {
        Mutations
                .createDelete(sqlClient, immutableType, (d, t) -> {})
                .execute();
    }

    @Override
    public <V extends View<E>> Viewer<E, ID, V> viewer(Class<V> viewType) {
        return new ViewerImpl<>(viewType);
    }

    @SuppressWarnings("unchecked")
    private <X> ConfigurableRootQuery<?, X> createQuery(
            Fetcher<?> fetcher,
            @Nullable Function<?, X> converter,
            @Nullable TypedProp.Scalar<?, ?>[] sortedProps,
            @Nullable Sort sort
    ) {
        MutableRootQueryImpl<Table<?>> query =
                new MutableRootQueryImpl<>(sqlClient, immutableType, ExecutionPurpose.QUERY, FilterLevel.DEFAULT);
        TableImplementor<?> table = (TableImplementor<?>) query.getTableLikeImplementor();
        if (sortedProps != null) {
            for (TypedProp.Scalar<?, ?> sortedProp : sortedProps) {
                if (!sortedProp.unwrap().getDeclaringType().isAssignableFrom(immutableType)) {
                    throw new IllegalArgumentException(
                            "The sorted field \"" +
                                    sortedProp +
                                    "\" does not belong to the type \"" +
                                    immutableType +
                                    "\" or its super types"
                    );
                }
                PropExpression<?> expr = table.get(sortedProp.unwrap());
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
        }
        if (sort != null) {
            query.orderBy(SpringOrders.toOrders(table, sort));
        }
        return query.select(
                fetcher != null ?
                        new FetcherSelectionImpl<>(table, fetcher, converter) :
                        (Selection<X>) table
        );
    }

    private class ViewerImpl<V extends View<E>> implements Viewer<E, ID, V> {

        private final Class<V> viewType;

        private final DtoMetadata<E, V> metadata;

        private ViewerImpl(Class<V> viewType) {
            this.viewType = viewType;
            this.metadata = DtoMetadata.of(viewType);
        }

        @Override
        public V findNullable(ID id) {
            return sqlClient.getEntities().findById(viewType, id);
        }

        @Override
        public List<V> findByIds(Iterable<ID> ids) {
            return sqlClient.getEntities().findByIds(viewType, Utils.toCollection(ids));
        }

        @Override
        public Map<ID, V> findMapByIds(Iterable<ID> ids) {
            return sqlClient.getEntities().findMapByIds(viewType, Utils.toCollection(ids));
        }

        @Override
        public List<V> findAll() {
            return createQuery(metadata.getFetcher(), metadata.getConverter(), null, null).execute();
        }

        @Override
        public List<V> findAll(TypedProp.Scalar<?, ?>... sortedProps) {
            return createQuery(metadata.getFetcher(), metadata.getConverter(), sortedProps, null).execute();
        }

        @Override
        public List<V> findAll(Sort sort) {
            return createQuery(metadata.getFetcher(), metadata.getConverter(), null, sort).execute();
        }

        @Override
        public Page<V> findAll(Pageable pageable) {
            return createQuery(metadata.getFetcher(), metadata.getConverter(), null, pageable.getSort())
                    .fetchPage(pageable.getPageNumber(), pageable.getPageSize(), SpringPageFactory.getInstance());
        }

        @Override
        public Page<V> findAll(int pageIndex, int pageSize) {
            return createQuery(metadata.getFetcher(), metadata.getConverter(), null, null)
                    .fetchPage(pageIndex, pageSize, SpringPageFactory.getInstance());
        }

        @Override
        public Page<V> findAll(int pageIndex, int pageSize, TypedProp.Scalar<?, ?>... sortedProps) {
            return createQuery(metadata.getFetcher(), metadata.getConverter(), sortedProps, null)
                    .fetchPage(pageIndex, pageSize, SpringPageFactory.getInstance());
        }

        @Override
        public Page<V> findAll(int pageIndex, int pageSize, Sort sort) {
            return createQuery(metadata.getFetcher(), metadata.getConverter(), null, sort)
                    .fetchPage(pageIndex, pageSize, SpringPageFactory.getInstance());
        }
    }
}
