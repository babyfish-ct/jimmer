package org.babyfish.jimmer.spring.repository;

import org.babyfish.jimmer.meta.TypedProp;
import org.babyfish.jimmer.spring.model.Input;
import org.babyfish.jimmer.spring.repository.support.Utils;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.ast.mutation.SimpleSaveResult;
import org.babyfish.jimmer.sql.ast.query.ConfigurableRootQuery;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.annotation.AliasFor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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

    Pager<E> pager(Pageable pageable);

    Pager<E> pager(int pageIndex, int pageSize, TypedProp.Scalar<?, ?> ... props);

    default Pager<E> pager(int pageIndex, int pageSize, Sort sort) {
        return pager(PageRequest.of(pageIndex, pageSize, sort));
    }


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

    List<E> findAll(Fetcher<E> fetcher);

    default List<E> findAll(TypedProp.Scalar<E, ?> sortedProp) {
        return findAll(new TypedProp.Scalar[]{ sortedProp });
    }

    default List<E> findAll(Fetcher<E> fetcher, TypedProp.Scalar<E, ?> sortedProp) {
        return findAll(fetcher, new TypedProp.Scalar[]{ sortedProp });
    }

    default List<E> findAll(TypedProp.Scalar<E, ?> sortedProp1, TypedProp.Scalar<E, ?> sortedProp2) {
        return findAll(new TypedProp.Scalar[]{ sortedProp1, sortedProp2 });
    }

    default List<E> findAll(Fetcher<E> fetcher, TypedProp.Scalar<E, ?> sortedProp1, TypedProp.Scalar<E, ?> sortedProp2) {
        return findAll(fetcher, new TypedProp.Scalar[]{ sortedProp1, sortedProp2 });
    }

    default List<E> findAll(TypedProp.Scalar<E, ?> sortedProp1, TypedProp.Scalar<E, ?> sortedProp2, TypedProp.Scalar<E, ?> sortedProp3) {
        return findAll(new TypedProp.Scalar[]{ sortedProp1, sortedProp2 });
    }

    default List<E> findAll(Fetcher<E> fetcher, TypedProp.Scalar<E, ?> sortedProp1, TypedProp.Scalar<E, ?> sortedProp2, TypedProp.Scalar<E, ?> sortedProp3) {
        return findAll(fetcher, new TypedProp.Scalar[]{ sortedProp1, sortedProp2, sortedProp3 });
    }

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

    default Page<E> findAll(int pageIndex, int pageSize, TypedProp.Scalar<E, ?> sortedProp) {
        return findAll(pageIndex, pageSize, new TypedProp.Scalar[]{ sortedProp });
    }

    default Page<E> findAll(int pageIndex, int pageSize, Fetcher<E> fetcher, TypedProp.Scalar<E, ?> sortedProp) {
        return findAll(pageIndex, pageSize, fetcher, new TypedProp.Scalar[]{ sortedProp });
    }

    default Page<E> findAll(int pageIndex, int pageSize, TypedProp.Scalar<E, ?> sortedProp1, TypedProp.Scalar<E, ?> sortedProp2) {
        return findAll(pageIndex, pageSize, new TypedProp.Scalar[]{ sortedProp1, sortedProp2 });
    }

    default Page<E> findAll(int pageIndex, int pageSize, Fetcher<E> fetcher, TypedProp.Scalar<E, ?> sortedProp1, TypedProp.Scalar<E, ?> sortedProp2) {
        return findAll(pageIndex, pageSize, fetcher, new TypedProp.Scalar[]{ sortedProp1, sortedProp2 });
    }

    default Page<E> findAll(int pageIndex, int pageSize, TypedProp.Scalar<E, ?> sortedProp1, TypedProp.Scalar<E, ?> sortedProp2, TypedProp.Scalar<E, ?> sortedProp3) {
        return findAll(pageIndex, pageSize, new TypedProp.Scalar[]{ sortedProp1, sortedProp2, sortedProp3 });
    }

    default Page<E> findAll(int pageIndex, int pageSize, Fetcher<E> fetcher, TypedProp.Scalar<E, ?> sortedProp1, TypedProp.Scalar<E, ?> sortedProp2, TypedProp.Scalar<E, ?> sortedProp3) {
        return findAll(pageIndex, pageSize, fetcher, new TypedProp.Scalar[]{ sortedProp1, sortedProp2, sortedProp3 });
    }

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

    interface Pager<E> {

        Page<E> execute(ConfigurableRootQuery<?, E> query);
    }
}
