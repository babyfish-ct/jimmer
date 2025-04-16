package org.babyfish.jimmer.spring.repository;

import org.babyfish.jimmer.View;
import org.babyfish.jimmer.impl.util.CollectionUtils;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.TypedProp;
import org.babyfish.jimmer.Input;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.ast.mutation.*;
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

/**
 * Spring Data Repository
 * @param <E> The entity type
 * @param <ID> The entity id type
 */
@NoRepositoryBean
public interface JRepository<E, ID> extends PagingAndSortingRepository<E, ID> {

    /*
     * For provider
     */

    JSqlClient sql();

    ImmutableType type();

    Class<E> entityType();

    /*
     * For consumer
     */

    E findNullable(ID id);

    E findNullable(ID id, Fetcher<E> fetcher);

    @NotNull
    @Override
    default Optional<E> findById(@NotNull ID id) {
        return Optional.ofNullable(findNullable(id));
    }

    @NotNull
    default Optional<E> findById(ID id, Fetcher<E> fetcher) {
        return Optional.ofNullable(findNullable(id, fetcher));
    }

    @AliasFor("findAllById")
    List<E> findByIds(Iterable<ID> ids);

    @AliasFor("findByIds")
    @NotNull
    @Override
    default List<E> findAllById(@NotNull Iterable<ID> ids) {
        return findByIds(ids);
    }

    List<E> findByIds(Iterable<ID> ids, Fetcher<E> fetcher);

    Map<ID, E> findMapByIds(Iterable<ID> ids);

    Map<ID, E> findMapByIds(Iterable<ID> ids, Fetcher<E> fetcher);

    @NotNull
    @Override
    List<E> findAll();

    List<E> findAll(TypedProp.Scalar<?, ?> ... sortedProps);

    List<E> findAll(Fetcher<E> fetcher, TypedProp.Scalar<?, ?> ... sortedProps);

    @NotNull
    @Override
    List<E> findAll(@NotNull Sort sort);

    List<E> findAll(Fetcher<E> fetcher, Sort sort);

    Page<E> findAll(int pageIndex, int pageSize);

    Page<E> findAll(int pageIndex, int pageSize, Fetcher<E> fetcher);

    Page<E> findAll(int pageIndex, int pageSize, TypedProp.Scalar<?, ?> ... sortedProps);

    Page<E> findAll(int pageIndex, int pageSize, Fetcher<E> fetcher, TypedProp.Scalar<?, ?> ... sortedProps);
    
    Page<E> findAll(int pageIndex, int pageSize, Sort sort);

    Page<E> findAll(int pageIndex, int pageSize, Fetcher<E> fetcher, Sort sort);

    @NotNull
    @Override
    Page<E> findAll(@NotNull Pageable pageable);
    
    Page<E> findAll(Pageable pageable, Fetcher<E> fetcher);

    @Override
    default boolean existsById(@NotNull ID id) {
        return findNullable(id) != null;
    }

    @Override
    long count();

    @NotNull
    default SimpleEntitySaveCommand<E> saveCommand(@NotNull E entity) {
        return sql().saveCommand(entity);
    }

    @NotNull
    default SimpleEntitySaveCommand<E> saveCommand(@NotNull Input<E> input) {
        return sql().saveCommand(input);
    }

    @NotNull
    default BatchEntitySaveCommand<E> saveEntitiesCommand(@NotNull Iterable<E> entities) {
        return sql().saveEntitiesCommand(entities);
    }

    @NotNull
    default BatchEntitySaveCommand<E> saveInputsCommand(@NotNull Iterable<? extends Input<E>> inputs) {
        return sql().saveInputsCommand(inputs);
    }

    @SuppressWarnings("unchecked")
    @NotNull
    @Override
    default <S extends E> S save(
            @NotNull S entity
    ) {
        return (S)saveCommand(entity)
                .execute()
                .getModifiedEntity();
    }

    @SuppressWarnings("unchecked")
    @NotNull
    default <S extends E> List<S> saveAll(
            @NotNull Iterable<S> entities
    ) {
        return (List<S>)saveEntitiesCommand((List<E>)entities)
                .execute()
                .getItems()
                .stream()
                .map(BatchSaveResult.Item::getModifiedEntity)
                .collect(Collectors.toList());
    }

    default E save(
            E entity,
            SaveMode mode
    ) {
        return saveCommand(entity)
                .setMode(mode)
                .execute()
                .getModifiedEntity();
    }

    default E save(
            E entity,
            SaveMode mode,
            AssociatedSaveMode associatedMode
    ) {
        return saveCommand(entity)
                .setMode(mode)
                .setAssociatedModeAll(associatedMode)
                .execute()
                .getModifiedEntity();
    }

    default List<E> saveEntities(
            Iterable<E> entities
    ) {
        return saveEntitiesCommand(entities)
                .execute()
                .getItems()
                .stream()
                .map(BatchSaveResult.Item::getModifiedEntity)
                .collect(Collectors.toList());
    }

    default List<E> saveEntities(
            Iterable<E> entities,
            SaveMode mode
    ) {
        return saveEntitiesCommand(entities)
                .setMode(mode)
                .execute()
                .getItems()
                .stream()
                .map(BatchSaveResult.Item::getModifiedEntity)
                .collect(Collectors.toList());
    }

    default List<E> saveEntities(
            Iterable<E> entities,
            SaveMode mode,
            AssociatedSaveMode associatedMode
    ) {
        return saveEntitiesCommand(entities)
                .setMode(mode)
                .setAssociatedModeAll(associatedMode)
                .execute()
                .getItems()
                .stream()
                .map(BatchSaveResult.Item::getModifiedEntity)
                .collect(Collectors.toList());
    }

    default E save(
            Input<E> input
    ) {
        return saveCommand(input)
                .execute()
                .getModifiedEntity();
    }

    default E save(
            Input<E> input,
            SaveMode mode,
            AssociatedSaveMode associatedMode
    ) {
        return saveCommand(input)
                .setMode(mode)
                .setAssociatedModeAll(associatedMode)
                .execute()
                .getModifiedEntity();
    }

    default List<E> saveInputs(
            Iterable<? extends Input<E>> inputs
    ) {
        return saveInputsCommand(inputs)
                .execute()
                .getItems()
                .stream()
                .map(BatchSaveResult.Item::getModifiedEntity)
                .collect(Collectors.toList());
    }

    default List<E> saveInputs(
            Iterable<? extends Input<E>> inputs,
            SaveMode mode,
            AssociatedSaveMode associatedMode
    ) {
        return saveInputsCommand(inputs)
                .setMode(mode)
                .setAssociatedModeAll(associatedMode)
                .execute()
                .getItems()
                .stream()
                .map(BatchSaveResult.Item::getModifiedEntity)
                .collect(Collectors.toList());
    }

    /**
     * @deprecated saving and re-fetching by fetcher/viewType is advanced feature,
     *              please use `saveCommand`
     */
    @Deprecated
    default E save(
            E entity,
            Fetcher<E> fetcher
    ) {
        return saveCommand(entity)
                .execute(fetcher)
                .getModifiedEntity();
    }

    /**
     * @deprecated saving and re-fetching by fetcher/viewType is advanced feature,
     *              please use `saveCommand`
     */
    @Deprecated
    default E save(
            E entity,
            SaveMode mode,
            AssociatedSaveMode associatedMode,
            Fetcher<E> fetcher
    ) {
        return saveCommand(entity)
                .setMode(mode)
                .setAssociatedModeAll(associatedMode)
                .execute(fetcher)
                .getModifiedEntity();
    }

    /**
     * @deprecated saving and re-fetching by fetcher/viewType is advanced feature,
     *              please use `saveCommand`
     */
    @Deprecated
    default List<E> saveEntities(
            Iterable<E> entities,
            Fetcher<E> fetcher
    ) {
        return saveEntitiesCommand(entities)
                .execute(fetcher)
                .getItems()
                .stream()
                .map(BatchSaveResult.Item::getModifiedEntity)
                .collect(Collectors.toList());
    }

    /**
     * @deprecated saving and re-fetching by fetcher/viewType is advanced feature,
     *              please use `saveCommand`
     */
    @Deprecated
    default List<E> saveEntities(
            Iterable<E> entities,
            SaveMode mode,
            AssociatedSaveMode associatedMode,
            Fetcher<E> fetcher
    ) {
        return saveEntitiesCommand(entities)
                .setMode(mode)
                .setAssociatedModeAll(associatedMode)
                .execute(fetcher)
                .getItems()
                .stream()
                .map(BatchSaveResult.Item::getModifiedEntity)
                .collect(Collectors.toList());
    }

    /**
     * @deprecated saving and re-fetching by fetcher/viewType is advanced feature,
     *              please use `saveCommand`
     */
    @Deprecated
    default E save(
            Input<E> input,
            Fetcher<E> fetcher
    ) {
        return saveCommand(input)
                .execute(fetcher)
                .getModifiedEntity();
    }

    /**
     * @deprecated saving and re-fetching by fetcher/viewType is advanced feature,
     *              please use `saveCommand`
     */
    @Deprecated
    default E save(
            Input<E> input,
            SaveMode mode,
            AssociatedSaveMode associatedMode,
            Fetcher<E> fetcher
    ) {
        return saveCommand(input)
                .setMode(mode)
                .setAssociatedModeAll(associatedMode)
                .execute(fetcher)
                .getModifiedEntity();
    }

    /**
     * @deprecated saving and re-fetching by fetcher/viewType is advanced feature,
     *              please use `saveCommand`
     */
    @Deprecated
    default List<E> saveInputs(
            Iterable<? extends Input<E>> inputs,
            Fetcher<E> fetcher
    ) {
        return saveInputsCommand(inputs)
                .execute(fetcher)
                .getItems()
                .stream()
                .map(BatchSaveResult.Item::getModifiedEntity)
                .collect(Collectors.toList());
    }

    /**
     * @deprecated saving and re-fetching by fetcher/viewType is advanced feature,
     *              please use `saveCommand`
     */
    @Deprecated
    default List<E> saveInputs(
            Iterable<? extends Input<E>> inputs,
            SaveMode mode,
            AssociatedSaveMode associatedMode,
            Fetcher<E> fetcher
    ) {
        return saveInputsCommand(inputs)
                .setMode(mode)
                .setAssociatedModeAll(associatedMode)
                .execute(fetcher)
                .getItems()
                .stream()
                .map(BatchSaveResult.Item::getModifiedEntity)
                .collect(Collectors.toList());
    }

    /**
     * @deprecated saving and re-fetching by fetcher/viewType is advanced feature,
     *              please use `saveCommand`
     */
    @Deprecated
    default <V extends View<E>> V save(
            E entity,
            Class<V> viewType
    ) {
        return saveCommand(entity)
                .execute(viewType)
                .getModifiedView();
    }

    /**
     * @deprecated saving and re-fetching by fetcher/viewType is advanced feature,
     *              please use `saveCommand`
     */
    @Deprecated
    default <V extends View<E>> V save(
            E entity,
            SaveMode mode,
            AssociatedSaveMode associatedMode,
            Class<V> viewType
    ) {
        return saveCommand(entity)
                .setMode(mode)
                .setAssociatedModeAll(associatedMode)
                .execute(viewType)
                .getModifiedView();
    }

    /**
     * @deprecated saving and re-fetching by fetcher/viewType is advanced feature,
     *              please use `saveCommand`
     */
    @Deprecated
    default <V extends View<E>> List<V> saveEntities(
            Iterable<E> entities,
            Class<V> viewType
    ) {
        return saveEntitiesCommand(entities)
                .execute(viewType)
                .getViewItems()
                .stream()
                .map(BatchSaveResult.View.ViewItem::getModifiedView)
                .collect(Collectors.toList());
    }

    /**
     * @deprecated saving and re-fetching by fetcher/viewType is advanced feature,
     *              please use `saveCommand`
     */
    @Deprecated
    default <V extends View<E>> List<V> saveEntities(
            Iterable<E> entities,
            SaveMode mode,
            AssociatedSaveMode associatedMode,
            Class<V> viewType
    ) {
        return saveEntitiesCommand(entities)
                .setMode(mode)
                .setAssociatedModeAll(associatedMode)
                .execute(viewType)
                .getViewItems()
                .stream()
                .map(BatchSaveResult.View.ViewItem::getModifiedView)
                .collect(Collectors.toList());
    }

    /**
     * @deprecated saving and re-fetching by fetcher/viewType is advanced feature,
     *              please use `saveCommand`
     */
    @Deprecated
    default <V extends View<E>> V save(
            Input<E> input,
            Class<V> viewType
    ) {
        return saveCommand(input)
                .execute(viewType)
                .getModifiedView();
    }

    /**
     * @deprecated saving and re-fetching by fetcher/viewType is advanced feature,
     *              please use `saveCommand`
     */
    @Deprecated
    default <V extends View<E>> V save(
            Input<E> input,
            SaveMode mode,
            AssociatedSaveMode associatedMode,
            Class<V> viewType
    ) {
        return saveCommand(input)
                .setMode(mode)
                .setAssociatedModeAll(associatedMode)
                .execute(viewType)
                .getModifiedView();
    }

    /**
     * @deprecated saving and re-fetching by fetcher/viewType is advanced feature,
     *              please use `saveCommand`
     */
    @Deprecated
    default <V extends View<E>> List<V> saveInputs(
            Iterable<? extends Input<E>> inputs,
            Class<V> viewType
    ) {
        return saveInputsCommand(inputs)
                .execute(viewType)
                .getViewItems()
                .stream()
                .map(BatchSaveResult.View.ViewItem::getModifiedView)
                .collect(Collectors.toList());
    }

    /**
     * @deprecated saving and re-fetching by fetcher/viewType is advanced feature,
     *              please use `saveCommand`
     */
    @Deprecated
    default <V extends View<E>> List<V> saveInputs(
            Iterable<? extends Input<E>> inputs,
            SaveMode mode,
            AssociatedSaveMode associatedMode,
            Class<V> viewType
    ) {
        return saveInputsCommand(inputs)
                .setMode(mode)
                .setAssociatedModeAll(associatedMode)
                .execute(viewType)
                .getViewItems()
                .stream()
                .map(BatchSaveResult.View.ViewItem::getModifiedView)
                .collect(Collectors.toList());
    }

    @Deprecated
    default E save(
            E entity,
            AssociatedSaveMode associatedMode
    ) {
        return saveCommand(entity)
                .setAssociatedModeAll(associatedMode)
                .execute()
                .getModifiedEntity();
    }

    @Deprecated
    default List<E> saveEntities(
            Iterable<E> entities,
            AssociatedSaveMode associatedMode
    ) {
        return saveEntitiesCommand(entities)
                .setAssociatedModeAll(associatedMode)
                .execute()
                .getItems()
                .stream()
                .map(BatchSaveResult.Item::getModifiedEntity)
                .collect(Collectors.toList());
    }

    @Deprecated
    default E save(
            Input<E> input,
            AssociatedSaveMode associatedMode
    ) {
        return saveCommand(input)
                .setAssociatedModeAll(associatedMode)
                .execute()
                .getModifiedEntity();
    }

    default E save(
            Input<E> input,
            SaveMode mode
    ) {
        return saveCommand(input)
                .setMode(mode)
                .execute()
                .getModifiedEntity();
    }

    @Deprecated
    default List<E> saveInputs(
            Iterable<? extends Input<E>> inputs,
            AssociatedSaveMode associatedMode
    ) {
        return saveInputsCommand(inputs)
                .setAssociatedModeAll(associatedMode)
                .execute()
                .getItems()
                .stream()
                .map(BatchSaveResult.Item::getModifiedEntity)
                .collect(Collectors.toList());
    }

    default List<E> saveInputs(
            Iterable<? extends Input<E>> inputs,
            SaveMode mode
    ) {
        return saveInputsCommand(inputs)
                .setMode(mode)
                .execute()
                .getItems()
                .stream()
                .map(BatchSaveResult.Item::getModifiedEntity)
                .collect(Collectors.toList());
    }

    /**
     * @deprecated saving and re-fetching by fetcher/viewType is advanced feature,
     *              please use `saveCommand`
     */
    @Deprecated
    default E save(
            E entity,
            AssociatedSaveMode associatedMode,
            Fetcher<E> fetcher
    ) {
        return saveCommand(entity)
                .setAssociatedModeAll(associatedMode)
                .execute(fetcher)
                .getModifiedEntity();
    }

    /**
     * @deprecated saving and re-fetching by fetcher/viewType is advanced feature,
     *              please use `saveCommand`
     */
    @Deprecated
    default E save(
            E entity,
            SaveMode mode,
            Fetcher<E> fetcher
    ) {
        return saveCommand(entity)
                .setMode(mode)
                .execute(fetcher)
                .getModifiedEntity();
    }

    /**
     * @deprecated saving and re-fetching by fetcher/viewType is advanced feature,
     *              please use `saveCommand`
     */
    @Deprecated
    default List<E> saveEntities(
            Iterable<E> entities,
            AssociatedSaveMode associatedMode,
            Fetcher<E> fetcher
    ) {
        return saveEntitiesCommand(entities)
                .setAssociatedModeAll(associatedMode)
                .execute(fetcher)
                .getItems()
                .stream()
                .map(BatchSaveResult.Item::getModifiedEntity)
                .collect(Collectors.toList());
    }

    /**
     * @deprecated saving and re-fetching by fetcher/viewType is advanced feature,
     *              please use `saveCommand`
     */
    @Deprecated
    default List<E> saveEntities(
            Iterable<E> entities,
            SaveMode mode,
            Fetcher<E> fetcher
    ) {
        return saveEntitiesCommand(entities)
                .setMode(mode)
                .execute(fetcher)
                .getItems()
                .stream()
                .map(BatchSaveResult.Item::getModifiedEntity)
                .collect(Collectors.toList());
    }

    /**
     * @deprecated saving and re-fetching by fetcher/viewType is advanced feature,
     *              please use `saveCommand`
     */
    @Deprecated
    default E save(
            Input<E> input,
            AssociatedSaveMode associatedMode,
            Fetcher<E> fetcher
    ) {
        return saveCommand(input)
                .setAssociatedModeAll(associatedMode)
                .execute(fetcher)
                .getModifiedEntity();
    }

    /**
     * @deprecated saving and re-fetching by fetcher/viewType is advanced feature,
     *              please use `saveCommand`
     */
    @Deprecated
    default E save(
            Input<E> input,
            SaveMode mode,
            Fetcher<E> fetcher
    ) {
        return saveCommand(input)
                .setMode(mode)
                .execute(fetcher)
                .getModifiedEntity();
    }

    /**
     * @deprecated saving and re-fetching by fetcher/viewType is advanced feature,
     *              please use `saveCommand`
     */
    @Deprecated
    default List<E> saveInputs(
            Iterable<? extends Input<E>> inputs,
            AssociatedSaveMode associatedMode,
            Fetcher<E> fetcher
    ) {
        return saveInputsCommand(inputs)
                .setAssociatedModeAll(associatedMode)
                .execute(fetcher)
                .getItems()
                .stream()
                .map(BatchSaveResult.Item::getModifiedEntity)
                .collect(Collectors.toList());
    }

    /**
     * @deprecated saving and re-fetching by fetcher/viewType is advanced feature,
     *              please use `saveCommand`
     */
    @Deprecated
    default List<E> saveInputs(
            Iterable<? extends Input<E>> inputs,
            SaveMode mode,
            Fetcher<E> fetcher
    ) {
        return saveInputsCommand(inputs)
                .setMode(mode)
                .execute(fetcher)
                .getItems()
                .stream()
                .map(BatchSaveResult.Item::getModifiedEntity)
                .collect(Collectors.toList());
    }

    /**
     * @deprecated saving and re-fetching by fetcher/viewType is advanced feature,
     *              please use `saveCommand`
     */
    @Deprecated
    default <V extends View<E>> V save(
            E entity,
            AssociatedSaveMode associatedMode,
            Class<V> viewType
    ) {
        return saveCommand(entity)
                .setAssociatedModeAll(associatedMode)
                .execute(viewType)
                .getModifiedView();
    }

    /**
     * @deprecated saving and re-fetching by fetcher/viewType is advanced feature,
     *              please use `saveCommand`
     */
    @Deprecated
    default <V extends View<E>> V save(
            E entity,
            SaveMode mode,
            Class<V> viewType
    ) {
        return saveCommand(entity)
                .setMode(mode)
                .execute(viewType)
                .getModifiedView();
    }

    /**
     * @deprecated saving and re-fetching by fetcher/viewType is advanced feature,
     *              please use `saveCommand`
     */
    @Deprecated
    default <V extends View<E>> List<V> saveEntities(
            Iterable<E> entities,
            AssociatedSaveMode associatedMode,
            Class<V> viewType
    ) {
        return saveEntitiesCommand(entities)
                .setAssociatedModeAll(associatedMode)
                .execute(viewType)
                .getViewItems()
                .stream()
                .map(BatchSaveResult.View.ViewItem::getModifiedView)
                .collect(Collectors.toList());
    }

    /**
     * @deprecated saving and re-fetching by fetcher/viewType is advanced feature,
     *              please use `saveCommand`
     */
    @Deprecated
    default <V extends View<E>> List<V> saveEntities(
            Iterable<E> entities,
            SaveMode mode,
            Class<V> viewType
    ) {
        return saveEntitiesCommand(entities)
                .setMode(mode)
                .execute(viewType)
                .getViewItems()
                .stream()
                .map(BatchSaveResult.View.ViewItem::getModifiedView)
                .collect(Collectors.toList());
    }

    /**
     * @deprecated saving and re-fetching by fetcher/viewType is advanced feature,
     *              please use `saveCommand`
     */
    @Deprecated
    default <V extends View<E>> V save(
            Input<E> input,
            AssociatedSaveMode associatedMode,
            Class<V> viewType
    ) {
        return saveCommand(input)
                .setAssociatedModeAll(associatedMode)
                .execute(viewType)
                .getModifiedView();
    }

    /**
     * @deprecated saving and re-fetching by fetcher/viewType is advanced feature,
     *              please use `saveCommand`
     */
    @Deprecated
    default <V extends View<E>> V save(
            Input<E> input,
            SaveMode mode,
            Class<V> viewType
    ) {
        return saveCommand(input)
                .setMode(mode)
                .execute(viewType)
                .getModifiedView();
    }

    /**
     * @deprecated saving and re-fetching by fetcher/viewType is advanced feature,
     *              please use `saveCommand`
     */
    @Deprecated
    default <V extends View<E>> List<V> saveInputs(
            Iterable<? extends Input<E>> inputs,
            AssociatedSaveMode associatedMode,
            Class<V> viewType
    ) {
        return saveInputsCommand(inputs)
                .setAssociatedModeAll(associatedMode)
                .execute(viewType)
                .getViewItems()
                .stream()
                .map(BatchSaveResult.View.ViewItem::getModifiedView)
                .collect(Collectors.toList());
    }

    /**
     * @deprecated saving and re-fetching by fetcher/viewType is advanced feature,
     *              please use `saveCommand`
     */
    @Deprecated
    default <V extends View<E>> List<V> saveInputs(
            Iterable<? extends Input<E>> inputs,
            SaveMode mode,
            Class<V> viewType
    ) {
        return saveInputsCommand(inputs)
                .setMode(mode)
                .execute(viewType)
                .getViewItems()
                .stream()
                .map(BatchSaveResult.View.ViewItem::getModifiedView)
                .collect(Collectors.toList());
    }

    @Deprecated
    @NotNull
    default E insert(@NotNull E entity) {
        return save(entity, SaveMode.INSERT_ONLY, AssociatedSaveMode.APPEND);
    }

    @Deprecated
    @NotNull
    default E insert(@NotNull E entity, AssociatedSaveMode associatedMode) {
        return save(entity, SaveMode.INSERT_ONLY, associatedMode);
    }

    @Deprecated
    @NotNull
    default E insert(@NotNull Input<E> input) {
        return save(input.toEntity(), SaveMode.INSERT_ONLY, AssociatedSaveMode.APPEND_IF_ABSENT);
    }

    @Deprecated
    @NotNull
    default E insert(@NotNull Input<E> input, AssociatedSaveMode associatedMode) {
        return save(input.toEntity(), SaveMode.INSERT_ONLY, associatedMode);
    }

    @Deprecated
    @NotNull
    default E insertIfAbsent(@NotNull E entity) {
        return save(entity, SaveMode.INSERT_IF_ABSENT, AssociatedSaveMode.APPEND_IF_ABSENT);
    }

    @Deprecated
    @NotNull
    default E insertIfAbsent(@NotNull E entity, AssociatedSaveMode associatedMode) {
        return save(entity, SaveMode.INSERT_IF_ABSENT, associatedMode);
    }

    @Deprecated
    @NotNull
    default E insertIfAbsent(@NotNull Input<E> input) {
        return save(input.toEntity(), SaveMode.INSERT_IF_ABSENT, AssociatedSaveMode.APPEND_IF_ABSENT);
    }

    @Deprecated
    @NotNull
    default E insertIfAbsent(@NotNull Input<E> input, AssociatedSaveMode associatedMode) {
        return save(input.toEntity(), SaveMode.INSERT_IF_ABSENT, associatedMode);
    }

    @Deprecated
    @NotNull
    default E update(@NotNull E entity) {
        return save(entity, SaveMode.UPDATE_ONLY, AssociatedSaveMode.UPDATE);
    }

    @Deprecated
    @NotNull
    default E update(@NotNull E entity, AssociatedSaveMode associatedMode) {
        return save(entity, SaveMode.UPDATE_ONLY, associatedMode);
    }

    @Deprecated
    @NotNull
    default E update(@NotNull Input<E> input) {
        return save(input.toEntity(), SaveMode.UPDATE_ONLY, AssociatedSaveMode.UPDATE);
    }

    @Deprecated
    @NotNull
    default E update(@NotNull Input<E> input, AssociatedSaveMode associatedMode) {
        return save(input.toEntity(), SaveMode.UPDATE_ONLY, associatedMode);
    }

    @Deprecated
    @NotNull
    default E merge(@NotNull E entity) {
        return save(entity, SaveMode.UPSERT, AssociatedSaveMode.MERGE);
    }

    @Deprecated
    @NotNull
    default E merge(@NotNull E entity, AssociatedSaveMode associatedMode) {
        return save(entity, SaveMode.UPSERT, associatedMode);
    }

    @Deprecated
    @NotNull
    default E merge(@NotNull Input<E> input) {
        return save(input.toEntity(), SaveMode.UPSERT, AssociatedSaveMode.MERGE);
    }

    @Deprecated
    @NotNull
    default E merge(@NotNull Input<E> input, AssociatedSaveMode associatedMode) {
        return save(input.toEntity(), SaveMode.UPSERT, associatedMode);
    }

    @Override
    default void delete(@NotNull E entity) {
        delete(entity, DeleteMode.AUTO);
    }

    int delete(@NotNull E entity, DeleteMode mode);

    @Override
    default void deleteAll(@NotNull Iterable<? extends E> entities) {
        deleteAll(entities, DeleteMode.AUTO);
    }

    int deleteAll(@NotNull Iterable<? extends E> entities, DeleteMode mode);

    @Override
    default void deleteById(@NotNull ID id) {
        deleteById(id, DeleteMode.AUTO);
    }

    int deleteById(@NotNull ID id, DeleteMode mode);
    
    @AliasFor("deleteAllById")
    default void deleteByIds(Iterable<? extends ID> ids) {
        deleteByIds(ids, DeleteMode.AUTO);
    }

    @AliasFor("deleteByIds")
    @Override
    default void deleteAllById(@NotNull Iterable<? extends ID> ids) {
        deleteByIds(ids, DeleteMode.AUTO);
    }

    @AliasFor("deleteAllById")
    int deleteByIds(Iterable<? extends ID> ids, DeleteMode mode);

    @Override
    void deleteAll();

    <V extends View<E>> Viewer<E, ID, V> viewer(Class<V> viewType);

    interface Viewer<E, ID, V extends View<E>> {

        V findNullable(ID id);

        List<V> findByIds(Iterable<ID> ids);

        Map<ID, V> findMapByIds(Iterable<ID> ids);

        List<V> findAll();

        List<V> findAll(TypedProp.Scalar<?, ?> ... sortedProps);

        List<V> findAll(Sort sort);

        Page<V> findAll(Pageable pageable);

        Page<V> findAll(int pageIndex, int pageSize);

        Page<V> findAll(int pageIndex, int pageSize, TypedProp.Scalar<?, ?> ... sortedProps);

        Page<V> findAll(int pageIndex, int pageSize, Sort sort);
    }
}
