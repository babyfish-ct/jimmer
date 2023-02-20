package org.babyfish.jimmer.spring.repository;

import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.TypedProp;
import org.babyfish.jimmer.Input;
import org.babyfish.jimmer.spring.repository.support.Utils;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.ast.mutation.SaveMode;
import org.babyfish.jimmer.sql.ast.mutation.SimpleSaveResult;
import org.babyfish.jimmer.sql.ast.query.ConfigurableRootQuery;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.annotation.AliasFor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.*;
import java.util.stream.Collectors;

@NoRepositoryBean
public interface JRepository<E, ID> extends PagingAndSortingRepository<E, ID> {

    /*
     * For provider
     */

    JSqlClient sql();

    ImmutableType type();

    Pager pager(Pageable pageable);

    Pager pager(int pageIndex, int pageSize);

    /*
     * For consumer
     */

    E findNullable(ID id);

    E findNullable(ID id, Fetcher<E> fetcher);

    @NotNull
    @Override
    default Optional<E> findById(ID id) {
        return Optional.ofNullable(findNullable(id));
    }

    default Optional<E> findById(ID id, Fetcher<E> fetcher) {
        return Optional.ofNullable(findNullable(id, fetcher));
    }

    @AliasFor("findAllById")
    List<E> findByIds(Iterable<ID> ids);

    @AliasFor("findByIds")
    @NotNull
    @Override
    default Iterable<E> findAllById(@NotNull Iterable<ID> ids) {
        return findByIds(ids);
    }

    List<E> findByIds(Iterable<ID> ids, Fetcher<E> fetcher);

    Map<ID, E> findMapByIds(Iterable<ID> ids);

    Map<ID, E> findMapByIds(Iterable<ID> ids, Fetcher<E> fetcher);

    @NotNull
    @Override
    List<E> findAll();

    @SuppressWarnings("unchecked")
    List<E> findAll(TypedProp.Scalar<?, ?> ... sortedProps);

    @SuppressWarnings("unchecked")
    List<E> findAll(Fetcher<E> fetcher, TypedProp.Scalar<?, ?> ... sortedProps);

    @NotNull
    @Override
    List<E> findAll(@NotNull Sort sort);

    List<E> findAll(Fetcher<E> fetcher, Sort sort);

    Page<E> findAll(int pageIndex, int pageSize);

    Page<E> findAll(int pageIndex, int pageSize, Fetcher<E> fetcher);

    @SuppressWarnings("unchecked")
    Page<E> findAll(int pageIndex, int pageSize, TypedProp.Scalar<?, ?> ... sortedProps);

    @SuppressWarnings("unchecked")
    Page<E> findAll(int pageIndex, int pageSize, Fetcher<E> fetcher, TypedProp.Scalar<?, ?> ... sortedProps);
    
    Page<E> findAll(int pageIndex, int pageSize, Sort sort);

    Page<E> findAll(int pageIndex, int pageSize, Fetcher<E> fetcher, Sort sort);

    @NotNull
    @Override
    Page<E> findAll(@NotNull Pageable pageable);
    
    Page<E> findAll(Pageable pageable, Fetcher<E> fetcher);

    @Override
    default boolean existsById(ID id) {
        return findNullable(id) != null;
    }

    @Override
    long count();

    @NotNull
    default E insert(@NotNull Input<E> input) {
        return insert(input.toEntity());
    }

    @NotNull
    default E insert(@NotNull E entity) {
        return sql()
                .getEntities()
                .saveCommand(entity)
                .configure(cfg -> {
                    cfg.setMode(SaveMode.INSERT_ONLY);
                    cfg.setAutoAttachingAll();
                }).execute().getModifiedEntity();
    }

    @NotNull
    default E update(@NotNull Input<E> input) {
        return update(input.toEntity());
    }

    @NotNull
    default E update(@NotNull E entity) {
        return sql()
                .getEntities()
                .saveCommand(entity)
                .configure(cfg -> {
                    cfg.setMode(SaveMode.UPDATE_ONLY);
                    cfg.setAutoAttachingAll();
                }).execute().getModifiedEntity();
    }

    @NotNull
    @Override
    default <S extends E> S save(@NotNull S entity) {
        return sql().getEntities().save(entity, true).getModifiedEntity();
    }
    
    @NotNull
    @Override
    default <S extends E> Iterable<S> saveAll(@NotNull Iterable<S> entities) {
        return sql()
                .getEntities()
                .batchSave(Utils.toCollection(entities), true)
                .getSimpleResults()
                .stream()
                .map(SimpleSaveResult::getModifiedEntity)
                .collect(Collectors.toList());
    }

    default E save(Input<E> input) {
        return sql()
                .getEntities()
                .save(input.toEntity(), true)
                .getModifiedEntity();
    }

    @Override
    void delete(@NotNull E entity);

    @Override
    void deleteAll(@NotNull Iterable<? extends E> entities);

    @Override
    void deleteAll();

    @Override
    void deleteById(@NotNull ID id);
    
    @AliasFor("deleteAllById")
    void deleteByIds(Iterable<? extends ID> ids);

    @AliasFor("deleteByIds")
    @Override
    default void deleteAllById(@NotNull Iterable<? extends ID> ids) {
        deleteByIds(ids);
    }

    GraphQl<E> graphql();

    interface Pager {

        <T> Page<T> execute(ConfigurableRootQuery<?, T> query);
    }

    interface GraphQl<E> {

        <X> Map<E, X> load(TypedProp.Scalar<E, X> prop, Collection<E> sources);

        <X> Map<E, X> load(TypedProp.Reference<E, X> prop, Collection<E> sources);

        <X> Map<E, List<X>> load(TypedProp.ReferenceList<E, X> prop, Collection<E> sources);
    }
}
