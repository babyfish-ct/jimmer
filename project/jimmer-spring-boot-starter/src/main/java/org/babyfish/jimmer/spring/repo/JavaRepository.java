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

import java.util.Collection;
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

    @NotNull default List<E> findByIds(Collection<ID> ids) {
        return findByIds(ids, (Fetcher<E>) null);
    }

    @NotNull List<E> findByIds(Collection<ID> ids, @Nullable Fetcher<E> fetcher);

    @NotNull <V extends View<E>> List<V> findByIds(Collection<ID> ids, Class<V> viewType);

    @NotNull default Map<ID, E> findMapByIds(Collection<ID> ids) {
        return findMapByIds(ids, (Fetcher<E>) null);
    }

    @NotNull Map<ID, E> findMapByIds(Collection<ID> ids, Fetcher<E> fetcher);

    @NotNull <V extends View<E>> Map<ID, V> findMapByIds(Collection<ID> ids, Class<V> viewType);

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

    default SimpleSaveResult<E> saveEntity(E entity) {
        return saveEntity(entity, SaveMode.UPSERT, AssociatedSaveMode.REPLACE);
    }

    SimpleSaveResult<E> saveEntity(E entity, SaveMode rootSaveMode, AssociatedSaveMode associatedSaveMode);

    default BatchSaveResult<E> saveEntities(Collection<E> entities) {
        return saveEntities(entities, SaveMode.UPSERT, AssociatedSaveMode.REPLACE);
    }

    BatchSaveResult<E> saveEntities(
            Collection<E> entities,
            SaveMode rootSaveMode,
            AssociatedSaveMode associatedSaveMode
    );

    default SimpleSaveResult<E> saveInput(Input<E> input) {
        return saveInput(input, SaveMode.UPSERT, AssociatedSaveMode.REPLACE);
    }

    SimpleSaveResult<E> saveInput(
            Input<E> input,
            SaveMode rootSaveMode,
            AssociatedSaveMode associatedSaveMode
    );

    default BatchSaveResult<E> saveInputs(Collection<Input<E>> inputs) {
        return saveInputs(inputs, SaveMode.UPSERT, AssociatedSaveMode.REPLACE);
    }

    BatchSaveResult<E> saveInputs(
            Collection<Input<E>> inputs,
            SaveMode rootSaveMode,
            AssociatedSaveMode associatedSaveMode
    );

    default long deleteById(ID id) {
        return deleteById(id, DeleteMode.AUTO);
    }

    long deleteById(ID id, DeleteMode deleteMode);

    default long deleteByIds(Collection<ID> ids) {
        return deleteByIds(ids, DeleteMode.AUTO);
    }

    long deleteByIds(Collection<ID> ids, DeleteMode deleteMode);
}
