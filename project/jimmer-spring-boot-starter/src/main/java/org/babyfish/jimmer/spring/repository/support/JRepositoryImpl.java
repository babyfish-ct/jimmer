package org.babyfish.jimmer.spring.repository.support;

import org.babyfish.jimmer.ImmutableObjects;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.TypedProp;
import org.babyfish.jimmer.spring.repository.JRepository;
import org.babyfish.jimmer.spring.repository.Sorts;
import org.babyfish.jimmer.sql.Entity;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.ast.impl.mutation.Mutations;
import org.babyfish.jimmer.sql.ast.impl.query.MutableRootQueryImpl;
import org.babyfish.jimmer.sql.ast.query.ConfigurableRootQuery;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.babyfish.jimmer.sql.runtime.ExecutionPurpose;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.GenericTypeResolver;
import org.springframework.data.domain.*;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@NoRepositoryBean
public class JRepositoryImpl<E, ID> implements JRepository<E, ID> {

    private static final TypedProp.Scalar<?, ?>[] EMPTY_SORTED_PROPS = new TypedProp.Scalar[0];

    protected final JSqlClient sqlClient;

    protected final Class<E> entityType;

    protected final ImmutableType immutableType;

    protected JRepositoryImpl(JSqlClient sqlClient) {
        this(sqlClient, null);
    }

    @SuppressWarnings("unchecked")
    public JRepositoryImpl(JSqlClient sqlClient, Class<E> entityType) {
        Utils.validateSqlClient(sqlClient);
        this.sqlClient = sqlClient;
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
            this.entityType = (Class<E>) typeArguments[0];
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
    public Pager<E> pager(Pageable pageable) {
        return new PagerImpl<>(pageable);
    }

    @Override
    public Pager<E> pager(int pageIndex, int pageSize, TypedProp.Scalar<?, ?> ... props) {
        return new PagerImpl<>(PageRequest.of(pageIndex, pageSize, Sorts.toSort(props)));
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
        return sqlClient.getEntities().findAll(entityType);
    }

    @Override
    public List<E> findAll(Fetcher<E> fetcher) {
        if (fetcher == null) {
            return findAll();
        }
        return sqlClient.getEntities().findAll(fetcher);
    }

    @Override
    public List<E> findAll(TypedProp.Scalar<?, ?>... sortedProps) {
        return sqlClient.getEntities().findAll(entityType, sortedProps);
    }

    @Override
    public List<E> findAll(Fetcher<E> fetcher, TypedProp.Scalar<?, ?>... sortedProps) {
        if (fetcher == null) {
            return findAll(sortedProps);
        }
        return sqlClient.getEntities().findAll(fetcher, sortedProps);
    }

    @NotNull
    @Override
    public List<E> findAll(@NotNull Sort sort) {
        return sqlClient.getEntities().findAll(entityType, Sorts.toTypedProps(entityType, sort));
    }

    @Override
    public List<E> findAll(Fetcher<E> fetcher, Sort sort) {
        if (fetcher == null) {
            return findAll(sort);
        }
        return sqlClient.getEntities().findAll(fetcher, Sorts.toTypedProps(entityType, sort));
    }

    @Override
    public Page<E> findAll(int pageIndex, int pageSize) {
        return findAll(pageIndex, pageSize, null, EMPTY_SORTED_PROPS);
    }

    @Override
    public Page<E> findAll(int pageIndex, int pageSize, Fetcher<E> fetcher) {
        return findAll(pageIndex, pageSize, fetcher, EMPTY_SORTED_PROPS);
    }

    @Override
    public Page<E> findAll(int pageIndex, int pageSize, TypedProp.Scalar<?, ?>... sortedProps) {
        return findAll(pageIndex, pageSize, null, sortedProps);
    }

    @Override
    public Page<E> findAll(int pageIndex, int pageSize, Fetcher<E> fetcher, TypedProp.Scalar<?, ?>... sortedProps) {
        return pager(pageIndex, pageSize, sortedProps).execute(createQuery(fetcher, sortedProps));
    }

    @Override
    public Page<E> findAll(int pageIndex, int pageSize, Sort sort) {
        return findAll(PageRequest.of(pageIndex, pageSize, sort), null);
    }

    @Override
    public Page<E> findAll(int pageIndex, int pageSize, Fetcher<E> fetcher, Sort sort) {
        return findAll(PageRequest.of(pageIndex, pageSize, sort), fetcher);
    }

    @NotNull
    @Override
    public Page<E> findAll(@NotNull Pageable pageable) {
        return findAll(pageable, null);
    }

    @Override
    public Page<E> findAll(Pageable pageable, Fetcher<E> fetcher) {
        return pager(pageable).execute(
                createQuery(fetcher, Sorts.toTypedProps(entityType, pageable.getSort()))
        );
    }

    @Override
    public long count() {
        return createQuery(null, EMPTY_SORTED_PROPS).count();
    }

    @Override
    public void delete(@NotNull E entity) {
        sqlClient.getEntities().delete(
                entityType,
                ImmutableObjects.get(entity, immutableType.getIdProp().getId())
        );
    }

    @Override
    public void deleteAll(@NotNull Iterable<? extends E> entities) {
        sqlClient.getEntities().batchDelete(
                entityType,
                Utils
                        .toCollection(entities)
                        .stream()
                        .map(it -> ImmutableObjects.get(it, immutableType.getIdProp().getId()))
                        .collect(Collectors.toList())
        );
    }

    @Override
    public void deleteAll() {
        Mutations
                .createDelete(sqlClient, immutableType, (d, t) -> {})
                .execute();
    }

    @Override
    public void deleteById(@NotNull ID id) {
        sqlClient.getEntities().delete(entityType, id);
    }

    @Override
    public void deleteByIds(Iterable<? extends ID> ids) {
        sqlClient
                .getEntities()
                .batchDelete(entityType, Utils.toCollection(ids));
    }

    @Override
    public GraphQl<E> graphql() {
        return new GraphQlImpl();
    }

    private ConfigurableRootQuery<?, E> createQuery(Fetcher<E> fetcher, TypedProp.Scalar<?, ?>[] sortedProps) {
        MutableRootQueryImpl<Table<E>> query =
                new MutableRootQueryImpl<>(sqlClient, immutableType, ExecutionPurpose.QUERY, false);
        Table<E> table = query.getTable();
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
            if (sortedProp instanceof TypedProp.Scalar.Desc<?, ?>) {
                query.orderBy(table.get(sortedProp.unwrap().getName()).desc());
            } else {
                query.orderBy(table.get(sortedProp.unwrap().getName()).asc());
            }
        }
        query.freeze();
        return query.select(fetcher != null ? table.fetch(fetcher) : table);
    }

    private static class PagerImpl<E> implements Pager<E> {

        private final Pageable pageable;

        private PagerImpl(Pageable pageable) {
            this.pageable = pageable;
        }

        @Override
        public Page<E> execute(ConfigurableRootQuery<?, E> query) {
            if (pageable.getPageSize() == 0) {
                return new PageImpl<>(query.execute());
            }
            long offset = pageable.getOffset();
            if (offset > Integer.MAX_VALUE - pageable.getPageSize()) {
                throw new IllegalArgumentException("offset is too big");
            }
            int total = query.count();
            List<E> content =
                    query
                            .limit(pageable.getPageSize(), (int)offset)
                            .execute();
            return new PageImpl<>(content, pageable, total);
        }
    }

    private class GraphQlImpl implements GraphQl<E> {

        @Override
        public <X> Map<E, X> load(TypedProp.Scalar<E, X> prop, Collection<E> sources) {
            return sqlClient.getLoaders().value(prop).batchLoad(sources);
        }

        @Override
        public <X> Map<E, X> load(TypedProp.Reference<E, X> prop, Collection<E> sources) {
            return sqlClient.getLoaders().reference(prop).batchLoad(sources);
        }

        @Override
        public <X> Map<E, List<X>> load(TypedProp.ReferenceList<E, X> prop, Collection<E> sources) {
            return sqlClient.getLoaders().list(prop).batchLoad(sources);
        }
    }
}
