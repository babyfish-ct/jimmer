package org.babyfish.jimmer.spring.repo;

import org.babyfish.jimmer.Input;
import org.babyfish.jimmer.Page;
import org.babyfish.jimmer.Slice;
import org.babyfish.jimmer.View;
import org.babyfish.jimmer.meta.TypedProp;
import org.babyfish.jimmer.sql.ast.mutation.*;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * In earlier versions of Jimmer, type {@link org.babyfish.jimmer.spring.repository.JRepository}
 * was used to support spring data style repository support.
 *
 * <p>However, based on user feedback, this interface was rarely used. The root causes are:</p>
 * <ul>
 * <li>Unlike JPA and MyBatis, which have lifecycle management objects like EntityManager/Session,
 * Jimmer itself is already designed with a stateless API.
 * Therefore, the stateless abstraction of spring dData style repository is meaningless for Jimmer.</li>
 * <li>Jimmer itself emphasizes type safety and strives to detect problems at compile-time.
 * spring data's approach based on conventional method names and {@code @Query} annotations
 * would lead to problems only being found at runtime (How Intellij helps certain solutions
 * cheat is not discussed here), which goes against Jimmer's design philosophy.</li>
 * </ul>
 * Therefore, developer can simply write a class and annotate it with
 * {@link org.springframework.data.repository.Repository}. At this point, users can choose to implement this interface or extends class
 * {@link org.babyfish.jimmer.spring.repo.support.AbstractJavaRepository}. Note, that this is optional, not mandatory.
 *
 * @param <E> The entity type
 * @param <ID> The entity id type
 */
public interface JavaRepository<E, ID> {

    @Nullable default E findById(ID id) {
        return findById(id, (Fetcher<E>) null);
    }

    @Nullable E findById(ID id, @Nullable Fetcher<E> fetcher);

    @Nullable <V extends View<E>> V findById(ID id, Class<V> viewType);

    @NotNull default List<E> findByIds(Iterable<ID> ids) {
        return findByIds(ids, (Fetcher<E>) null);
    }

    @NotNull List<E> findByIds(Iterable<ID> ids, @Nullable Fetcher<E> fetcher);

    @NotNull <V extends View<E>> List<V> findByIds(Iterable<ID> ids, Class<V> viewType);

    @NotNull default Map<ID, E> findMapByIds(Iterable<ID> ids) {
        return findMapByIds(ids, (Fetcher<E>) null);
    }

    @NotNull Map<ID, E> findMapByIds(Iterable<ID> ids, Fetcher<E> fetcher);

    @NotNull <V extends View<E>> Map<ID, V> findMapByIds(Iterable<ID> ids, Class<V> viewType);

    @NotNull default List<E> findAll(TypedProp.Scalar<?, ?> ... sortedProps) {
        return findAll((Fetcher<E>) null, sortedProps);
    }

    @NotNull List<E> findAll(@Nullable Fetcher<E> fetcher, TypedProp.Scalar<?, ?> ... sortedProps);

    @NotNull <V extends View<E>> List<V> findAll(Class<V> viewType, TypedProp.Scalar<?, ?> ... sortedProps);

    @NotNull
    default Page<E> findPage(PageParam pageParam, TypedProp.Scalar<?, ?> ... sortedProps) {
        return findPage(pageParam, (Fetcher<E>) null, sortedProps);
    }

    @NotNull Page<E> findPage(PageParam pageParam, @Nullable Fetcher<E> fetcher, TypedProp.Scalar<?, ?> ... sortedProps);

    @NotNull <V extends View<E>> Page<V> findPage(
            PageParam pageParam,
            Class<V> viewType,
            TypedProp.Scalar<?, ?> ... sortedProps
    );

    @NotNull
    default Slice<E> findSlice(int limit, int offset, TypedProp.Scalar<?, ?> ... sortedProps) {
        return findSlice(limit, offset, (Fetcher<E>) null, sortedProps);
    }

    @NotNull Slice<E> findSlice(int limit, int offset, @Nullable Fetcher<E> fetcher, TypedProp.Scalar<?, ?> ... sortedProps);

    @NotNull <V extends View<E>> Slice<V> findSlice(
            int limit,
            int offset,
            Class<V> viewType,
            TypedProp.Scalar<?, ?> ... sortedProps
    );

    /**
     * Shortcut or {@link org.babyfish.jimmer.sql.JSqlClient#save(Object)},
     * please view that method to know more
     */
    default SimpleSaveResult<E> save(E entity) {
        return save(entity, SaveMode.UPSERT, AssociatedSaveMode.REPLACE);
    }

    /**
     * Shortcut for {@link org.babyfish.jimmer.sql.JSqlClient#save(Object, SaveMode)},
     * please view that method to know more
     */
    default SimpleSaveResult<E> save(E entity, SaveMode mode) {
        return save(entity, mode, AssociatedSaveMode.REPLACE);
    }

    /**
     * Shortcut for {@link org.babyfish.jimmer.sql.JSqlClient#save(Object, AssociatedSaveMode)},
     * please view that method to know more
     */
    default SimpleSaveResult<E> save(E entity, AssociatedSaveMode associatedMode) {
        return save(entity, SaveMode.UPSERT, associatedMode);
    }

    /**
     * Shortcut for {@link org.babyfish.jimmer.sql.JSqlClient#save(Object, SaveMode, AssociatedSaveMode)},
     * please view that method to know more
     */
    SimpleSaveResult<E> save(E entity, SaveMode mode, AssociatedSaveMode associatedMode);

    /**
     * Shortcut for {@link org.babyfish.jimmer.sql.JSqlClient#insert(Object)},
     * please view that method to know more
     */
    default SimpleSaveResult<E> insert(E entity) {
        return save(entity, SaveMode.INSERT_ONLY, AssociatedSaveMode.APPEND);
    }

    /**
     * Shortcut for {@link org.babyfish.jimmer.sql.JSqlClient#insert(Object, AssociatedSaveMode)},
     * please view that method to know more
     */
    default SimpleSaveResult<E> insert(E entity, AssociatedSaveMode associatedSaveMode) {
        return save(entity, SaveMode.INSERT_ONLY, associatedSaveMode);
    }

    /**
     * Shortcut for {@link org.babyfish.jimmer.sql.JSqlClient#insert(Input)},
     * please view that method to know more
     */
    default SimpleSaveResult<E> insert(Input<E> input) {
        return save(input, SaveMode.INSERT_ONLY, AssociatedSaveMode.APPEND);
    }

    /**
     * Shortcut for {@link org.babyfish.jimmer.sql.JSqlClient#insert(Input, AssociatedSaveMode)},
     * please view that method to know more
     */
    default SimpleSaveResult<E> insert(Input<E> input, AssociatedSaveMode associatedSaveMode) {
        return save(input, SaveMode.INSERT_ONLY, associatedSaveMode);
    }

    /**
     * Shortcut for {@link org.babyfish.jimmer.sql.JSqlClient#insertIfAbsent(Object)},
     * please view that method to know more
     */
    default SimpleSaveResult<E> insertIfAbsent(E entity) {
        return save(entity, SaveMode.INSERT_IF_ABSENT, AssociatedSaveMode.APPEND_IF_ABSENT);
    }

    /**
     * Shortcut for {@link org.babyfish.jimmer.sql.JSqlClient#insertIfAbsent(Object, AssociatedSaveMode)},
     * please view that method to know more
     */
    default SimpleSaveResult<E> insertIfAbsent(E entity, AssociatedSaveMode associatedSaveMode) {
        return save(entity, SaveMode.INSERT_IF_ABSENT, associatedSaveMode);
    }

    /**
     * Shortcut for {@link org.babyfish.jimmer.sql.JSqlClient#insertIfAbsent(Input)},
     * please view that method to know more
     */
    default SimpleSaveResult<E> insertIfAbsent(Input<E> input) {
        return save(input, SaveMode.INSERT_IF_ABSENT, AssociatedSaveMode.APPEND_IF_ABSENT);
    }

    /**
     * Shortcut for {@link org.babyfish.jimmer.sql.JSqlClient#insertIfAbsent(Input, AssociatedSaveMode)},
     * please view that method to know more
     */
    default SimpleSaveResult<E> insertIfAbsent(Input<E> input, AssociatedSaveMode associatedSaveMode) {
        return save(input, SaveMode.INSERT_IF_ABSENT, associatedSaveMode);
    }

    /**
     * Shortcut for {@link org.babyfish.jimmer.sql.JSqlClient#update(Object)},
     * please view that method to know more
     */
    default SimpleSaveResult<E> update(E entity) {
        return save(entity, SaveMode.UPDATE_ONLY, AssociatedSaveMode.UPDATE);
    }

    /**
     * Shortcut for {@link org.babyfish.jimmer.sql.JSqlClient#update(Object, AssociatedSaveMode)},
     * please view that method to know more
     */
    default SimpleSaveResult<E> update(E entity, AssociatedSaveMode associatedSaveMode) {
        return save(entity, SaveMode.UPDATE_ONLY, associatedSaveMode);
    }

    /**
     * Shortcut for {@link org.babyfish.jimmer.sql.JSqlClient#update(Input)},
     * please view that method to know more
     */
    default SimpleSaveResult<E> update(Input<E> input) {
        return save(input, SaveMode.UPDATE_ONLY, AssociatedSaveMode.UPDATE);
    }

    /**
     * Shortcut for {@link org.babyfish.jimmer.sql.JSqlClient#update(Input, AssociatedSaveMode)},
     * please view that method to know more
     */
    default SimpleSaveResult<E> update(Input<E> input, AssociatedSaveMode associatedSaveMode) {
        return save(input, SaveMode.UPDATE_ONLY, associatedSaveMode);
    }

    /**
     * Shortcut for {@link org.babyfish.jimmer.sql.JSqlClient#merge(Object)},
     * please view that method to know more
     */
    default SimpleSaveResult<E> merge(E entity) {
        return save(entity, SaveMode.UPSERT, AssociatedSaveMode.MERGE);
    }

    /**
     * Shortcut for {@link org.babyfish.jimmer.sql.JSqlClient#merge(Object)},
     * please view that method to know more
     */
    default SimpleSaveResult<E> merge(E entity, AssociatedSaveMode associatedSaveMode) {
        return save(entity, SaveMode.UPSERT, associatedSaveMode);
    }

    /**
     * Shortcut for {@link org.babyfish.jimmer.sql.JSqlClient#merge(Input)},
     * please view that method to know more
     */
    default SimpleSaveResult<E> merge(Input<E> input) {
        return save(input, SaveMode.UPSERT, AssociatedSaveMode.MERGE);
    }

    /**
     * Shortcut for {@link org.babyfish.jimmer.sql.JSqlClient#merge(Input)},
     * please view that method to know more
     */
    default SimpleSaveResult<E> merge(Input<E> input, AssociatedSaveMode associatedSaveMode) {
        return save(input, SaveMode.UPSERT, associatedSaveMode);
    }

    /**
     * Shortcut for {@link org.babyfish.jimmer.sql.JSqlClient#saveEntities(Iterable)},
     * please view that method to know more
     */
    default BatchSaveResult<E> saveEntities(Iterable<E> entities) {
        return saveEntities(entities, SaveMode.UPSERT, AssociatedSaveMode.REPLACE);
    }

    /**
     * Shortcut for {@link org.babyfish.jimmer.sql.JSqlClient#saveEntities(Iterable, SaveMode)},
     * please view that method to know more
     */
    default BatchSaveResult<E> saveEntities(Iterable<E> entities, SaveMode mode) {
        return saveEntities(entities, mode, AssociatedSaveMode.REPLACE);
    }

    /**
     * Shortcut for {@link org.babyfish.jimmer.sql.JSqlClient#saveEntities(Iterable, AssociatedSaveMode)},
     * please view that method to know more
     */
    default BatchSaveResult<E> saveEntities(Iterable<E> entities, AssociatedSaveMode associatedMode) {
        return saveEntities(entities, SaveMode.UPSERT, associatedMode);
    }

    /**
     * Shortcut for {@link org.babyfish.jimmer.sql.JSqlClient#saveEntities(Iterable, SaveMode, AssociatedSaveMode)} ,
     * please view that method to know more
     */
    BatchSaveResult<E> saveEntities(
            Iterable<E> entities,
            SaveMode rootSaveMode,
            AssociatedSaveMode associatedSaveMode
    );

    /**
     * Shortcut for {@link org.babyfish.jimmer.sql.JSqlClient#save(Input)} )},
     * please view that method to know more
     */
    default SimpleSaveResult<E> save(Input<E> input) {
        return save(input, SaveMode.UPSERT, AssociatedSaveMode.REPLACE);
    }

    /**
     * Shortcut for {@link org.babyfish.jimmer.sql.JSqlClient#save(Input, SaveMode)},
     * please view that method to know more
     */
    default SimpleSaveResult<E> save(Input<E> input, SaveMode mode) {
        return save(input, mode, AssociatedSaveMode.REPLACE);
    }

    /**
     * Shortcut for {@link org.babyfish.jimmer.sql.JSqlClient#insertIfAbsent(Input, AssociatedSaveMode)},
     * please view that method to know more
     */
    default SimpleSaveResult<E> save(Input<E> input, AssociatedSaveMode associatedMode) {
        return save(input, SaveMode.UPSERT, associatedMode);
    }

    /**
     * Shortcut for {@link org.babyfish.jimmer.sql.JSqlClient#save(Input, SaveMode, AssociatedSaveMode)},
     * please view that method to know more
     */
    SimpleSaveResult<E> save(
            Input<E> input,
            SaveMode rootSaveMode,
            AssociatedSaveMode associatedSaveMode
    );

    /**
     * Shortcut for {@link org.babyfish.jimmer.sql.JSqlClient#saveInputs(Iterable)},
     * please view that method to know more
     */
    default BatchSaveResult<E> saveInputs(Iterable<? extends Input<E>> inputs) {
        return saveInputs(inputs, SaveMode.UPSERT, AssociatedSaveMode.REPLACE);
    }

    /**
     * Shortcut for {@link org.babyfish.jimmer.sql.JSqlClient#save(Input, SaveMode)},
     * please view that method to know more
     */
    default BatchSaveResult<E> saveInputs(Iterable<? extends Input<E>> inputs, SaveMode mode) {
        return saveInputs(inputs, mode, AssociatedSaveMode.REPLACE);
    }

    /**
     * Shortcut for {@link org.babyfish.jimmer.sql.JSqlClient#saveInputs(Iterable, AssociatedSaveMode)},
     * please view that method to know more
     */
    default BatchSaveResult<E> saveInputs(Iterable<? extends Input<E>> inputs, AssociatedSaveMode associatedMode) {
        return saveInputs(inputs, SaveMode.UPSERT, associatedMode);
    }

    /**
     * Shortcut for {@link org.babyfish.jimmer.sql.JSqlClient#saveInputs(Iterable, SaveMode, AssociatedSaveMode)},
     * please view that method to know more
     */
    BatchSaveResult<E> saveInputs(
            Iterable<? extends Input<E>> inputs,
            SaveMode rootSaveMode,
            AssociatedSaveMode associatedSaveMode
    );

    default long deleteById(ID id) {
        return deleteById(id, DeleteMode.AUTO);
    }

    long deleteById(ID id, DeleteMode deleteMode);

    default long deleteByIds(Iterable<ID> ids) {
        return deleteByIds(ids, DeleteMode.AUTO);
    }

    long deleteByIds(Iterable<ID> ids, DeleteMode deleteMode);
}
