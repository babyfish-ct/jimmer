package org.babyfish.jimmer.spring.repo.support;

import org.babyfish.jimmer.Input;
import org.babyfish.jimmer.Page;
import org.babyfish.jimmer.Slice;
import org.babyfish.jimmer.View;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.PropId;
import org.babyfish.jimmer.meta.TypedProp;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.spring.repo.JavaRepository;
import org.babyfish.jimmer.spring.repo.PageParam;
import org.babyfish.jimmer.spring.repository.JRepository;
import org.babyfish.jimmer.sql.Entity;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.ast.PropExpression;
import org.babyfish.jimmer.sql.ast.Selection;
import org.babyfish.jimmer.sql.ast.impl.query.FilterLevel;
import org.babyfish.jimmer.sql.ast.impl.query.MutableRootQueryImpl;
import org.babyfish.jimmer.sql.ast.impl.table.FetcherSelectionImpl;
import org.babyfish.jimmer.sql.ast.impl.table.TableImplementor;
import org.babyfish.jimmer.sql.ast.mutation.*;
import org.babyfish.jimmer.sql.ast.query.ConfigurableRootQuery;
import org.babyfish.jimmer.sql.ast.query.Order;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.fetcher.DtoMetadata;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.babyfish.jimmer.sql.runtime.ExecutionPurpose;
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.core.GenericTypeResolver;

import java.util.*;
import java.util.function.Function;

/**
 * Base implementation of {@link JavaRepository}
 * @param <E> The entity type
 * @param <ID> The entity id type
 */
public class AbstractJavaRepository<E, ID> implements JavaRepository<E, ID> {

    protected final JSqlClient sqlClient;

    protected final Class<E> entityType;

    protected final ImmutableType type;

    @SuppressWarnings("unchecked")
    protected AbstractJavaRepository(JSqlClient sqlClient) {
        this.sqlClient = Objects.requireNonNull(sqlClient, "sqlClient is required");
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
        this.entityType = (Class<E>) typeArguments[0];
        this.type = ImmutableType.get(entityType);
        if (!type.isEntity()) {
            throw new IllegalArgumentException(
                    "\"" +
                            entityType +
                            "\" is not entity type decorated by @" +
                            Entity.class.getName()
            );
        }
    }

    @Nullable
    @Override
    public E findById(ID id, @Nullable Fetcher<E> fetcher) {
        if (fetcher == null) {
            return sqlClient.findById(entityType, id);
        }
        return sqlClient.findById(fetcher, id);
    }

    @Nullable
    @Override
    public <V extends View<E>> V findById(ID id, Class<V> viewType) {
        return sqlClient.findById(viewType, id);
    }

    @NotNull
    @Override
    public List<E> findByIds(Collection<ID> ids, @Nullable Fetcher<E> fetcher) {
        if (fetcher == null) {
            return sqlClient.findByIds(entityType, ids);
        }
        return sqlClient.findByIds(fetcher, ids);
    }

    @NotNull
    @Override
    public <V extends View<E>> List<V> findByIds(Collection<ID> ids, Class<V> viewType) {
        return sqlClient.findByIds(viewType, ids);
    }

    @NotNull
    @Override
    public Map<ID, E> findMapByIds(Collection<ID> ids, Fetcher<E> fetcher) {
        if (fetcher == null) {
            return sqlClient.findMapByIds(entityType, ids);
        }
        return sqlClient.findMapByIds(fetcher, ids);
    }

    @SuppressWarnings("unchecked")
    @NotNull
    @Override
    public <V extends View<E>> Map<ID, V> findMapByIds(Collection<ID> ids, Class<V> viewType) {
        DtoMetadata<E, V> metadata = DtoMetadata.of(viewType);
        List<E> entities = sqlClient.findByIds(metadata.getFetcher(), ids);
        Map<ID, V> map = new LinkedHashMap<>((entities.size() * 4 + 2) / 3);
        PropId idPropId = type.getIdProp().getId();
        for (E entity : entities) {
            map.put(
                    (ID)((ImmutableSpi)entity).__get(idPropId),
                    metadata.getConverter().apply(entity)
            );
        }
        return map;
    }

    @NotNull
    @Override
    public List<E> findAll(@Nullable Fetcher<E> fetcher, TypedProp.Scalar<?, ?>... sortedProps) {
        ConfigurableRootQuery<?, E> query = createQuery(fetcher, null, sortedProps);
        return query.execute();
    }

    @NotNull
    @Override
    public <V extends View<E>> List<V> findAll(Class<V> viewType, TypedProp.Scalar<?, ?>... sortedProps) {
        DtoMetadata<E, V> metadata = DtoMetadata.of(viewType);
        ConfigurableRootQuery<?, V> query = createQuery(metadata.getFetcher(), metadata.getConverter(), sortedProps);
        return query.execute();
    }

    @NotNull
    @Override
    public Page<E> findPage(PageParam pageParam, @Nullable Fetcher<E> fetcher, TypedProp.Scalar<?, ?>... sortedProps) {
        ConfigurableRootQuery<?, E> query = createQuery(fetcher, null, sortedProps);
        return query.fetchPage(pageParam.getIndex(), pageParam.getSize());
    }

    @NotNull
    @Override
    public <V extends View<E>> Page<V> findPage(PageParam pageParam, Class<V> viewType, TypedProp.Scalar<?, ?>... sortedProps) {
        DtoMetadata<E, V> metadata = DtoMetadata.of(viewType);
        ConfigurableRootQuery<?, V> query = createQuery(metadata.getFetcher(), metadata.getConverter(), sortedProps);
        return query.fetchPage(pageParam.getIndex(), pageParam.getSize());
    }

    @NotNull
    @Override
    public Slice<E> findSlice(int limit, int offset, @Nullable Fetcher<E> fetcher, TypedProp.Scalar<?, ?>... sortedProps) {
        ConfigurableRootQuery<?, E> query = createQuery(fetcher, null, sortedProps);
        return query.fetchSlice(limit, offset);
    }

    @NotNull
    @Override
    public <V extends View<E>> Slice<V> findSlice(int limit, int offset, Class<V> viewType, TypedProp.Scalar<?, ?>... sortedProps) {
        DtoMetadata<E, V> metadata = DtoMetadata.of(viewType);
        ConfigurableRootQuery<?, V> query = createQuery(metadata.getFetcher(), metadata.getConverter(), sortedProps);
        return query.fetchSlice(limit, offset);
    }

    @Override
    public SimpleSaveResult<E> saveEntity(E entity, SaveMode rootSaveMode, AssociatedSaveMode associatedSaveMode) {
        return sqlClient
                .getEntities()
                .saveCommand(entity)
                .setMode(rootSaveMode)
                .setAssociatedModeAll(associatedSaveMode)
                .execute();
    }

    @Override
    public BatchSaveResult<E> saveEntities(Collection<E> entities, SaveMode rootSaveMode, AssociatedSaveMode associatedSaveMode) {
        return sqlClient
                .getEntities()
                .saveEntitiesCommand(entities)
                .setMode(rootSaveMode)
                .setAssociatedModeAll(associatedSaveMode)
                .execute();
    }

    @Override
    public SimpleSaveResult<E> saveInput(Input<E> input, SaveMode rootSaveMode, AssociatedSaveMode associatedSaveMode) {
        return sqlClient
                .getEntities()
                .saveCommand(input.toEntity())
                .setMode(rootSaveMode)
                .setAssociatedModeAll(associatedSaveMode)
                .execute();
    }

    @Override
    public BatchSaveResult<E> saveInputs(Collection<Input<E>> inputs, SaveMode rootSaveMode, AssociatedSaveMode associatedSaveMode) {
        return sqlClient
                .getEntities()
                .saveInputsCommand(inputs)
                .setMode(rootSaveMode)
                .setAssociatedModeAll(associatedSaveMode)
                .execute();
    }

    @Override
    public long deleteById(ID id, DeleteMode deleteMode) {
        return sqlClient.deleteById(entityType, id, deleteMode).getAffectedRowCount(entityType);
    }

    @Override
    public long deleteByIds(Collection<ID> ids, DeleteMode deleteMode) {
        return sqlClient.deleteByIds(entityType, ids, deleteMode).getAffectedRowCount(entityType);
    }

    @SuppressWarnings("unchecked")
    private <X> ConfigurableRootQuery<?, X> createQuery(
            Fetcher<?> fetcher,
            @Nullable Function<?, X> converter,
            @Nullable TypedProp.Scalar<?, ?>[] sortedProps
    ) {
        MutableRootQueryImpl<Table<?>> query =
                new MutableRootQueryImpl<>(
                        (JSqlClientImplementor) sqlClient,
                        type,
                        ExecutionPurpose.QUERY,
                        FilterLevel.DEFAULT
                );
        TableImplementor<?> table = query.getTableImplementor();
        if (sortedProps != null) {
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
        return query.select(
                fetcher != null ?
                        new FetcherSelectionImpl<>(table, fetcher, converter) :
                        (Selection<X>) table
        );
    }
}
