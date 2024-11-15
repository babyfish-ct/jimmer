package org.babyfish.jimmer.sql;

import org.babyfish.jimmer.Input;
import org.babyfish.jimmer.sql.ast.mutation.*;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public interface Saver {

    <E> SimpleEntitySaveCommand<E> saveCommand(E entity);

    default <E> SimpleEntitySaveCommand<E> saveCommand(Input<E> input) {
        return saveCommand(input.toEntity());
    }

    <E> BatchEntitySaveCommand<E> saveEntitiesCommand(Iterable<E> entities);

    default <E> BatchEntitySaveCommand<E> saveInputsCommand(Iterable<Input<E>> inputs) {
        List<E> entities = inputs instanceof Collection<?> ?
                new ArrayList<>(((Collection<?>)inputs).size()) :
                new ArrayList<>();
        for (Input<E> input : inputs) {
            entities.add(input.toEntity());
        }
        return saveEntitiesCommand(entities);
    }

    /**
     * Save an entity object
     * @param <E> The type of saved entity
     * @param entity The saved entity object.
     *               <p>Note: The jimmer entity is <b>not POJO</b>,
     *               it can easily express data structures of arbitrary shape,
     *               you can use it to save data structures of arbitrary shape.</p>
     *
     *               <p>Unlike most JVM ORMs, Jimmer does not specified the shape
     *               of the saved data structure by using configuration such as
     *               `insertable`, `updatable` or `cascade`; instead,
     *               it uses the dynamic nature of entity object itself to describe
     *               the shape of saved data structure, <b>without prior design</b></p>
     *
     *               <p>Unspecified properties will be ignored,
     *               only the specified properties <i>(whether null or not)</i> will be saved.
     *               In addition to objects with only id property, any associated objects
     *               will result in deeper recursive saves.</p>
     * @param mode The save mode of aggregate-root
     * @param associatedMode The save mode of associated-objects
     * @return The saved result for single object
     */
    default <E> SimpleSaveResult<E> save(
            E entity,
            SaveMode mode,
            @NotNull AssociatedSaveMode associatedMode
    ) {
        return saveCommand(entity)
                .setMode(mode)
                .setAssociatedModeAll(associatedMode)
                .execute();
    }

    /**
     * Save an entity object
     * @param <E> The type of saved entity
     * @param entity The saved entity object.
     *               <p>Note: The jimmer entity is <b>not POJO</b>,
     *               it can easily express data structures of arbitrary shape,
     *               you can use it to save data structures of arbitrary shape.</p>
     *
     *               <p>Unlike most JVM ORMs, Jimmer does not specified the shape
     *               of the saved data structure by using configuration such as
     *               `insertable`, `updatable` or `cascade`; instead,
     *               it uses the dynamic nature of entity object itself to describe
     *               the shape of saved data structure, <b>without prior design</b></p>
     *
     *               <p>Unspecified properties will be ignored,
     *               only the specified properties <i>(whether null or not)</i> will be saved.
     *               In addition to objects with only id property, any associated objects
     *               will result in deeper recursive saves.</p>
     * @param mode The save mode of aggregate-root
     *             <p>The associated save mode of associated objects is {@link AssociatedSaveMode#REPLACE}</p>
     * @return The saved result for single object
     */
    default <E> SimpleSaveResult<E> save(E entity, SaveMode mode) {
        return saveCommand(entity)
                .setMode(mode)
                .execute();
    }

    /**
     * Save an entity object
     * @param <E> The type of saved entity
     * @param entity The saved entity object.
     *               <p>Note: The jimmer entity is <b>not POJO</b>,
     *               it can easily express data structures of arbitrary shape,
     *               you can use it to save data structures of arbitrary shape.</p>
     *
     *               <p>Unlike most JVM ORMs, Jimmer does not specified the shape
     *               of the saved data structure by using configuration such as
     *               `insertable`, `updatable` or `cascade`; instead,
     *               it uses the dynamic nature of entity object itself to describe
     *               the shape of saved data structure, <b>without prior design</b></p>
     *
     *               <p>Unspecified properties will be ignored,
     *               only the specified properties <i>(whether null or not)</i> will be saved.
     *               In addition to objects with only id property, any associated objects
     *               will result in deeper recursive saves.</p>
     * @param associatedMode The save mode of associated-objects
     *                       <p>The save mode of aggregate-root is {@link SaveMode#UPSERT}</p>
     * @return The saved result for single object
     */
    default <E> SimpleSaveResult<E> save(E entity, AssociatedSaveMode associatedMode) {
        return saveCommand(entity)
                .setAssociatedModeAll(associatedMode)
                .execute();
    }

    /**
     * Save an entity object
     * @param <E> The type of saved entity
     * @param entity The saved entity object.
     *               <p>Note: The jimmer entity is <b>not POJO</b>,
     *               it can easily express data structures of arbitrary shape,
     *               you can use it to save data structures of arbitrary shape.</p>
     *
     *               <p>Unlike most JVM ORMs, Jimmer does not specified the shape
     *               of the saved data structure by using configuration such as
     *               `insertable`, `updatable` or `cascade`; instead,
     *               it uses the dynamic nature of entity object itself to describe
     *               the shape of saved data structure, <b>without prior design</b></p>
     *
     *               <p>Unspecified properties will be ignored,
     *               only the specified properties <i>(whether null or not)</i> will be saved.
     *               In addition to objects with only id property, any associated objects
     *               will result in deeper recursive saves.</p>
     *
     *               <p>The save mode of aggregate-root is {@link SaveMode#UPSERT}</p>
     *               <p>The associated save mode of associated objects is {@link AssociatedSaveMode#REPLACE}</p>
     * @return The saved result for single object
     */
    default <E> SimpleSaveResult<E> save(E entity) {
        return saveCommand(entity).execute();
    }

    /**
     * Save an input DTO
     * @param <E> The type of saved entity
     * @param input The saved input DTO
     *
     *               <p>In terms of internal mechanisms, any type of Input DTO is
     *               automatically converted into an entity object of the same type.</p>
     *
     *               <p>Note: The jimmer entity is <b>not POJO</b>,
     *               it can easily express data structures of arbitrary shape,
     *               you can use it to save data structures of arbitrary shape.</p>
     *
     *               <p>Unlike most JVM ORMs, Jimmer does not specified the shape
     *               of the saved data structure by using configuration such as
     *               `insertable`, `updatable` or `cascade`; instead,
     *               it uses the dynamic nature of entity object itself to describe
     *               the shape of saved data structure, <b>without prior design</b></p>
     *
     *               <p>Unspecified properties will be ignored,
     *               only the specified properties <i>(whether null or not)</i> will be saved.
     *               In addition to objects with only id property, any associated objects
     *               will result in deeper recursive saves.</p>
     * @param mode The save mode of aggregate-root
     * @param associatedMode The save mode of associated-objects
     * @return The saved result for single object
     */
    default <E> SimpleSaveResult<E> save(Input<E> input, SaveMode mode, AssociatedSaveMode associatedMode) {
        return saveCommand(input.toEntity())
                .setMode(mode)
                .setAssociatedModeAll(associatedMode)
                .execute();
    }

    /**
     * Save an input DTO
     * @param <E> The type of saved entity
     * @param input The saved input DTO
     *
     *               <p>In terms of internal mechanisms, any type of Input DTO is
     *               automatically converted into an entity object of the same type.</p>
     *
     *               <p>Note: The jimmer entity is <b>not POJO</b>,
     *               it can easily express data structures of arbitrary shape,
     *               you can use it to save data structures of arbitrary shape.</p>
     *
     *               <p>Unlike most JVM ORMs, Jimmer does not specified the shape
     *               of the saved data structure by using configuration such as
     *               `insertable`, `updatable` or `cascade`; instead,
     *               it uses the dynamic nature of entity object itself to describe
     *               the shape of saved data structure, <b>without prior design</b></p>
     *
     *               <p>Unspecified properties will be ignored,
     *               only the specified properties <i>(whether null or not)</i> will be saved.
     *               In addition to objects with only id property, any associated objects
     *               will result in deeper recursive saves.</p>
     * @param mode The save mode of aggregate-root
     *             <p>The associated save mode of associated objects is {@link AssociatedSaveMode#REPLACE}</p>
     * @return The saved result for single object
     */
    default <E> SimpleSaveResult<E> save(Input<E> input, SaveMode mode) {
        return saveCommand(input.toEntity())
                .setMode(mode)
                .execute();
    }

    /**
     * Save an input DTO
     * @param <E> The type of saved entity
     * @param input The saved input DTO
     *
     *               <p>In terms of internal mechanisms, any type of Input DTO is
     *               automatically converted into an entity object of the same type.</p>
     *
     *               <p>Note: The jimmer entity is <b>not POJO</b>,
     *               it can easily express data structures of arbitrary shape,
     *               you can use it to save data structures of arbitrary shape.</p>
     *
     *               <p>Unlike most JVM ORMs, Jimmer does not specified the shape
     *               of the saved data structure by using configuration such as
     *               `insertable`, `updatable` or `cascade`; instead,
     *               it uses the dynamic nature of entity object itself to describe
     *               the shape of saved data structure, <b>without prior design</b></p>
     *
     *               <p>Unspecified properties will be ignored,
     *               only the specified properties <i>(whether null or not)</i> will be saved.
     *               In addition to objects with only id property, any associated objects
     *               will result in deeper recursive saves.</p>
     * @param associatedMode The save mode of associated-objects
     *                       <p>The save mode of aggregate-root is {@link SaveMode#UPSERT}</p>
     * @return The saved result for single object
     */
    default <E> SimpleSaveResult<E> save(Input<E> input, AssociatedSaveMode associatedMode) {
        return saveCommand(input.toEntity())
                .setAssociatedModeAll(associatedMode)
                .execute();
    }

    /**
     * Save an input DTO
     * @param <E> The type of saved entity
     * @param input The saved input DTO
     *
     *               <p>In terms of internal mechanisms, any type of Input DTO is
     *               automatically converted into an entity object of the same type.</p>
     *
     *               <p>Note: The jimmer entity is <b>not POJO</b>,
     *               it can easily express data structures of arbitrary shape,
     *               you can use it to save data structures of arbitrary shape.</p>
     *
     *               <p>Unlike most JVM ORMs, Jimmer does not specified the shape
     *               of the saved data structure by using configuration such as
     *               `insertable`, `updatable` or `cascade`; instead,
     *               it uses the dynamic nature of entity object itself to describe
     *               the shape of saved data structure, <b>without prior design</b></p>
     *
     *               <p>Unspecified properties will be ignored,
     *               only the specified properties <i>(whether null or not)</i> will be saved.
     *               In addition to objects with only id property, any associated objects
     *               will result in deeper recursive saves.</p>
     *
     *               <p>The save mode of aggregate-root is {@link SaveMode#UPSERT}</p>
     *               <p>The associated save mode of associated objects is {@link AssociatedSaveMode#REPLACE}</p>
     * @return The saved result for single object
     */
    default <E> SimpleSaveResult<E> save(Input<E> input) {
        return saveCommand(input.toEntity()).execute();
    }

    /**
     * Insert an entity object
     * @param entity The inserted entity object
     *
     *               <p>Note: The jimmer entity is <b>not POJO</b>,
     *               it can easily express data structures of arbitrary shape,
     *               you can use it to save data structures of arbitrary shape.</p>
     *
     *               <p>Unlike most JVM ORMs, Jimmer does not specified the shape
     *               of the saved data structure by using configuration such as
     *               `insertable`, `updatable` or `cascade`; instead,
     *               it uses the dynamic nature of entity object itself to describe
     *               the shape of saved data structure, <b>without prior design</b></p>
     *
     *               <p>Unspecified properties will be ignored,
     *               only the specified properties <i>(whether null or not)</i> will be saved.
     *               In addition to objects with only id property, any associated objects
     *               will result in deeper recursive saves.</p>
     *
     *               <ul>
     *                  <li>The save mode of aggregate-root is {@link SaveMode#INSERT_ONLY}</li>
     *                  <li>The associated save mode of associated objects is {@link AssociatedSaveMode#APPEND}</li>
     *               </ul>
     * @return The save result for single object
     * @param <E> The type of inserted entity
     */
    default <E> SimpleSaveResult<E> insert(@NotNull E entity) {
        return saveCommand(entity)
                .setMode(SaveMode.INSERT_ONLY)
                .setAssociatedModeAll(AssociatedSaveMode.APPEND)
                .execute();
    }

    /**
     * Insert an entity object
     * @param entity The inserted entity object
     *
     *               <p>Note: The jimmer entity is <b>not POJO</b>,
     *               it can easily express data structures of arbitrary shape,
     *               you can use it to save data structures of arbitrary shape.</p>
     *
     *               <p>Unlike most JVM ORMs, Jimmer does not specified the shape
     *               of the saved data structure by using configuration such as
     *               `insertable`, `updatable` or `cascade`; instead,
     *               it uses the dynamic nature of entity object itself to describe
     *               the shape of saved data structure, <b>without prior design</b></p>
     *
     *               <p>Unspecified properties will be ignored,
     *               only the specified properties <i>(whether null or not)</i> will be saved.
     *               In addition to objects with only id property, any associated objects
     *               will result in deeper recursive saves.</p>
     *
     *               <p>The save mode of aggregate-root is {@link SaveMode#INSERT_ONLY}</p>
     * @param associatedMode The associated save mode of associated objects.
     * @return The save result for single object
     * @param <E> The type of inserted entity
     */
    default <E> SimpleSaveResult<E> insert(@NotNull E entity, @NotNull AssociatedSaveMode associatedMode) {
        return saveCommand(entity)
                .setMode(SaveMode.INSERT_ONLY)
                .setAssociatedModeAll(associatedMode)
                .execute();
    }

    /**
     * Insert an input DTO
     * @param input The inserted input DTO
     *
     *               <p>In terms of internal mechanisms, any type of Input DTO is
     *               automatically converted into an entity object of the same type.</p>
     *
     *               <p>Note: The jimmer entity is <b>not POJO</b>,
     *               it can easily express data structures of arbitrary shape,
     *               you can use it to save data structures of arbitrary shape.</p>
     *
     *               <p>Unlike most JVM ORMs, Jimmer does not specified the shape
     *               of the saved data structure by using configuration such as
     *               `insertable`, `updatable` or `cascade`; instead,
     *               it uses the dynamic nature of entity object itself to describe
     *               the shape of saved data structure, <b>without prior design</b></p>
     *
     *               <p>Unspecified properties will be ignored,
     *               only the specified properties <i>(whether null or not)</i> will be saved.
     *               In addition to objects with only id property, any associated objects
     *               will result in deeper recursive saves.</p>
     *
     *               <ul>
     *                  <li>The save mode of aggregate-root is {@link SaveMode#INSERT_ONLY}</li>
     *                  <li>The associated save mode of associated objects is {@link AssociatedSaveMode#APPEND}</li>
     *               </ul>
     * @return The save result for single object
     * @param <E> The type of inserted entity
     */
    default <E> SimpleSaveResult<E> insert(@NotNull Input<E> input) {
        return saveCommand(input)
                .setMode(SaveMode.INSERT_ONLY)
                .setAssociatedModeAll(AssociatedSaveMode.APPEND)
                .execute();
    }

    /**
     * Insert an input DTO
     * @param input The inserted input DTO
     *
     *               <p>In terms of internal mechanisms, any type of Input DTO is
     *               automatically converted into an entity object of the same type.</p>
     *
     *               <p>Note: The jimmer entity is <b>not POJO</b>,
     *               it can easily express data structures of arbitrary shape,
     *               you can use it to save data structures of arbitrary shape.</p>
     *
     *               <p>Unlike most JVM ORMs, Jimmer does not specified the shape
     *               of the saved data structure by using configuration such as
     *               `insertable`, `updatable` or `cascade`; instead,
     *               it uses the dynamic nature of entity object itself to describe
     *               the shape of saved data structure, <b>without prior design</b></p>
     *
     *               <p>Unspecified properties will be ignored,
     *               only the specified properties <i>(whether null or not)</i> will be saved.
     *               In addition to objects with only id property, any associated objects
     *               will result in deeper recursive saves.</p>
     *
     *               <p>The save mode of aggregate-root is {@link SaveMode#INSERT_ONLY}</p>
     * @param associatedMode The associated save mode of associated objects.
     * @return The save result for single object
     * @param <E> The type of inserted entity
     */
    default <E> SimpleSaveResult<E> insert(@NotNull Input<E> input, @NotNull AssociatedSaveMode associatedMode) {
        return saveCommand(input)
                .setMode(SaveMode.INSERT_ONLY)
                .setAssociatedModeAll(associatedMode)
                .execute();
    }

    /**
     * Insert an entity object if necessary,
     * if the entity object exists in database, ignore it.
     * <ul>
     *     <li>If the value of id property decorated by {@link Id} is specified,
     *     use id value to check whether the entity object exists in database</li>
     *     <li>otherwise, if the values of key properties decorated by {@link Key} is specified
     *     use key values to check whether the entity object exists in database</li>
     *     <li>If neither value of id property nor values of key properties is specified,
     *     exception will be raised.</li>
     * </ul>
     * @param entity The inserted entity object
     *
     *               <p>Note: The jimmer entity is <b>not POJO</b>,
     *               it can easily express data structures of arbitrary shape,
     *               you can use it to save data structures of arbitrary shape.</p>
     *
     *               <p>Unlike most JVM ORMs, Jimmer does not specified the shape
     *               of the saved data structure by using configuration such as
     *               `insertable`, `updatable` or `cascade`; instead,
     *               it uses the dynamic nature of entity object itself to describe
     *               the shape of saved data structure, <b>without prior design</b></p>
     *
     *               <p>Unspecified properties will be ignored,
     *               only the specified properties <i>(whether null or not)</i> will be saved.
     *               In addition to objects with only id property, any associated objects
     *               will result in deeper recursive saves.</p>
     *
     *               <ul>
     *                  <li>The save mode of aggregate-root is {@link SaveMode#INSERT_IF_ABSENT}</li>
     *                  <li>The associated save mode of associated objects {@link AssociatedSaveMode#APPEND_IF_ABSENT}</li>
     *               </ul>
     * @return The save result for single object
     * @param <E> The type of inserted entity
     */
    default <E> SimpleSaveResult<E> insertIfAbsent(@NotNull E entity) {
        return saveCommand(entity)
                .setMode(SaveMode.INSERT_IF_ABSENT)
                .setAssociatedModeAll(AssociatedSaveMode.APPEND_IF_ABSENT)
                .execute();
    }

    /**
     * Insert an entity object if necessary,
     * if the entity object exists in database, ignore it.
     * <ul>
     *     <li>If the value of id property decorated by {@link Id} is specified,
     *     use id value to check whether the entity object exists in database</li>
     *     <li>otherwise, if the values of key properties decorated by {@link Key} is specified
     *     use key values to check whether the entity object exists in database</li>
     *     <li>If neither value of id property nor values of key properties is specified,
     *     exception will be raised.</li>
     * </ul>
     * @param entity The inserted entity object
     *
     *               <p>Note: The jimmer entity is <b>not POJO</b>,
     *               it can easily express data structures of arbitrary shape,
     *               you can use it to save data structures of arbitrary shape.</p>
     *
     *               <p>Unlike most JVM ORMs, Jimmer does not specified the shape
     *               of the saved data structure by using configuration such as
     *               `insertable`, `updatable` or `cascade`; instead,
     *               it uses the dynamic nature of entity object itself to describe
     *               the shape of saved data structure, <b>without prior design</b></p>
     *
     *               <p>Unspecified properties will be ignored,
     *               only the specified properties <i>(whether null or not)</i> will be saved.
     *               In addition to objects with only id property, any associated objects
     *               will result in deeper recursive saves.</p>
     *
     *               <p>The save mode of aggregate-root is {@link SaveMode#INSERT_IF_ABSENT}</p>
     * @param associatedMode The associated save mode of associated objects.
     *
     * @return The save result for single object
     * @param <E> The type of inserted entity
     */
    default <E> SimpleSaveResult<E> insertIfAbsent(@NotNull E entity, @NotNull AssociatedSaveMode associatedMode) {
        return saveCommand(entity)
                .setMode(SaveMode.INSERT_IF_ABSENT)
                .setAssociatedModeAll(associatedMode)
                .execute();
    }

    /**
     * Insert an input DTO if necessary, that means to convert
     * the input DTO to entity object and save it.
     * If the entity object exists in database, ignore it.
     * <ul>
     *     <li>If the value of id property decorated by {@link Id} is specified,
     *     use id value to check whether the entity object exists in database</li>
     *     <li>otherwise, if the values of key properties decorated by {@link Key} is specified
     *     use key values to check whether the entity object exists in database</li>
     *     <li>If neither value of id property nor values of key properties is specified,
     *     exception will be raised.</li>
     * </ul>
     * @param input The inserted input DTO
     *
     *               <p>In terms of internal mechanisms, any type of Input DTO is
     *               automatically converted into an entity object of the same type.</p>
     *
     *               <p>Note: The jimmer entity is <b>not POJO</b>,
     *               it can easily express data structures of arbitrary shape,
     *               you can use it to save data structures of arbitrary shape.</p>
     *
     *               <p>Unlike most JVM ORMs, Jimmer does not specified the shape
     *               of the saved data structure by using configuration such as
     *               `insertable`, `updatable` or `cascade`; instead,
     *               it uses the dynamic nature of entity object itself to describe
     *               the shape of saved data structure, <b>without prior design</b></p>
     *
     *               <p>Unspecified properties will be ignored,
     *               only the specified properties <i>(whether null or not)</i> will be saved.
     *               In addition to objects with only id property, any associated objects
     *               will result in deeper recursive saves.</p>
     *
     *               <ul>
     *                  <li>The save mode of aggregate-root is {@link SaveMode#INSERT_IF_ABSENT}</li>
     *                  <li>The associated save mode of associated objects is {@link AssociatedSaveMode#APPEND_IF_ABSENT}</li>
     *               </ul>
     * @return The save result for single object
     * @param <E> The type of inserted entity
     */
    default <E> SimpleSaveResult<E> insertIfAbsent(@NotNull Input<E> input) {
        return saveCommand(input)
                .setMode(SaveMode.INSERT_IF_ABSENT)
                .setAssociatedModeAll(AssociatedSaveMode.APPEND_IF_ABSENT)
                .execute();
    }

    /**
     * Insert an input DTO if necessary, that means to convert
     * the input DTO to entity object and save it.
     * If the entity object exists in database, ignore it.
     * <ul>
     *     <li>If the value of id property decorated by {@link Id} is specified,
     *     use id value to check whether the entity object exists in database</li>
     *     <li>otherwise, if the values of key properties decorated by {@link Key} is specified
     *     use key values to check whether the entity object exists in database</li>
     *     <li>If neither value of id property nor values of key properties is specified,
     *     exception will be raised.</li>
     * </ul>
     * @param input The inserted input DTO
     *
     *               <p>In terms of internal mechanisms, any type of Input DTO is
     *               automatically converted into an entity object of the same type.</p>
     *
     *               <p>Note: The jimmer entity is <b>not POJO</b>,
     *               it can easily express data structures of arbitrary shape,
     *               you can use it to save data structures of arbitrary shape.</p>
     *
     *               <p>Unlike most JVM ORMs, Jimmer does not specified the shape
     *               of the saved data structure by using configuration such as
     *               `insertable`, `updatable` or `cascade`; instead,
     *               it uses the dynamic nature of entity object itself to describe
     *               the shape of saved data structure, <b>without prior design</b></p>
     *
     *               <p>Unspecified properties will be ignored,
     *               only the specified properties <i>(whether null or not)</i> will be saved.
     *               In addition to objects with only id property, any associated objects
     *               will result in deeper recursive saves.</p>
     *
     *               <p>The save mode of aggregate-root is {@link SaveMode#INSERT_IF_ABSENT}</p>
     * @param associatedMode The associated save mode of associated objects.
     * @return The save result for single object
     * @param <E> The type of inserted entity
     */
    default <E> SimpleSaveResult<E> insertIfAbsent(
            @NotNull Input<E> input,
            @NotNull AssociatedSaveMode associatedMode
    ) {
        return saveCommand(input)
                .setMode(SaveMode.INSERT_IF_ABSENT)
                .setAssociatedModeAll(associatedMode)
                .execute();
    }

    /**
     * Update an entity object
     * @param entity The updated entity object
     *
     *               <p>Note: The jimmer entity is <b>not POJO</b>,
     *               it can easily express data structures of arbitrary shape,
     *               you can use it to save data structures of arbitrary shape.</p>
     *
     *               <p>Unlike most JVM ORMs, Jimmer does not specified the shape
     *               of the saved data structure by using configuration such as
     *               `insertable`, `updatable` or `cascade`; instead,
     *               it uses the dynamic nature of entity object itself to describe
     *               the shape of saved data structure, <b>without prior design</b></p>
     *
     *               <p>Unspecified properties will be ignored,
     *               only the specified properties <i>(whether null or not)</i> will be saved.
     *               In addition to objects with only id property, any associated objects
     *               will result in deeper recursive saves.</p>
     *
     *               <ul>
     *                  <li>The save mode of aggregate-root is {@link SaveMode#UPDATE_ONLY}</li>
     *                  <li>The associated save mode of associated objects is {@link AssociatedSaveMode#UPDATE}</li>
     *               </ul>
     * @return The save result for single object
     * @param <E> The type of inserted entity
     */
    default <E> SimpleSaveResult<E> update(@NotNull E entity) {
        return saveCommand(entity)
                .setMode(SaveMode.UPDATE_ONLY)
                .setAssociatedModeAll(AssociatedSaveMode.UPDATE)
                .execute();
    }

    /**
     * Update an entity object
     * @param entity The updated entity object
     *
     *               <p>Note: The jimmer entity is <b>not POJO</b>,
     *               it can easily express data structures of arbitrary shape,
     *               you can use it to save data structures of arbitrary shape.</p>
     *
     *               <p>Unlike most JVM ORMs, Jimmer does not specified the shape
     *               of the saved data structure by using configuration such as
     *               `insertable`, `updatable` or `cascade`; instead,
     *               it uses the dynamic nature of entity object itself to describe
     *               the shape of saved data structure, <b>without prior design</b></p>
     *
     *               <p>Unspecified properties will be ignored,
     *               only the specified properties <i>(whether null or not)</i> will be saved.
     *               In addition to objects with only id property, any associated objects
     *               will result in deeper recursive saves.</p>
     *
     *               <p>The save mode of aggregate-root is {@link SaveMode#UPDATE_ONLY}</p>
     * @param associatedMode The associated save mode of associated objects.
     * @return The save result for single object
     * @param <E> The type of inserted entity
     */
    default <E> SimpleSaveResult<E> update(@NotNull E entity, @NotNull AssociatedSaveMode associatedMode) {
        return saveCommand(entity)
                .setMode(SaveMode.UPDATE_ONLY)
                .setAssociatedModeAll(associatedMode)
                .execute();
    }

    /**
     * Update an input DTO
     * @param input The updated input DTO
     *
     *               <p>In terms of internal mechanisms, any type of Input DTO is
     *               automatically converted into an entity object of the same type.</p>
     *
     *               <p>Note: The jimmer entity is <b>not POJO</b>,
     *               it can easily express data structures of arbitrary shape,
     *               you can use it to save data structures of arbitrary shape.</p>
     *
     *               <p>Unlike most JVM ORMs, Jimmer does not specified the shape
     *               of the saved data structure by using configuration such as
     *               `insertable`, `updatable` or `cascade`; instead,
     *               it uses the dynamic nature of entity object itself to describe
     *               the shape of saved data structure, <b>without prior design</b></p>
     *
     *               <p>Unspecified properties will be ignored,
     *               only the specified properties <i>(whether null or not)</i> will be saved.
     *               In addition to objects with only id property, any associated objects
     *               will result in deeper recursive saves.</p>
     *
     *               <ul>
     *                  <li>The save mode of aggregate-root is {@link SaveMode#UPDATE_ONLY}</li>
     *                  <li>The associated save mode of associated objects is {@link AssociatedSaveMode#UPDATE}</li>
     *               </ul>
     * @return The save result for single object
     * @param <E> The type of inserted entity
     */
    default <E> SimpleSaveResult<E> update(@NotNull Input<E> input) {
        return saveCommand(input)
                .setMode(SaveMode.UPDATE_ONLY)
                .setAssociatedModeAll(AssociatedSaveMode.UPDATE)
                .execute();
    }

    /**
     * Update an input DTO
     * @param input The updated input DTO
     *
     *               <p>In terms of internal mechanisms, any type of Input DTO is
     *               automatically converted into an entity object of the same type.</p>
     *
     *               <p>Note: The jimmer entity is <b>not POJO</b>,
     *               it can easily express data structures of arbitrary shape,
     *               you can use it to save data structures of arbitrary shape.</p>
     *
     *               <p>Unlike most JVM ORMs, Jimmer does not specified the shape
     *               of the saved data structure by using configuration such as
     *               `insertable`, `updatable` or `cascade`; instead,
     *               it uses the dynamic nature of entity object itself to describe
     *               the shape of saved data structure, <b>without prior design</b></p>
     *
     *               <p>Unspecified properties will be ignored,
     *               only the specified properties <i>(whether null or not)</i> will be saved.
     *               In addition to objects with only id property, any associated objects
     *               will result in deeper recursive saves.</p>
     *
     *               <p>The save mode of aggregate-root is {@link SaveMode#UPDATE_ONLY}</p>
     * @param associatedMode The associated save mode of associated objects.
     * @return The save result for single object
     * @param <E> The type of inserted entity
     */
    default <E> SimpleSaveResult<E> update(@NotNull Input<E> input, @NotNull AssociatedSaveMode associatedMode) {
        return saveCommand(input)
                .setMode(SaveMode.UPDATE_ONLY)
                .setAssociatedModeAll(associatedMode)
                .execute();
    }

    /**
     * Merge an entity object
     * @param entity The merged entity object
     *
     *               <p>Note: The jimmer entity is <b>not POJO</b>,
     *               it can easily express data structures of arbitrary shape,
     *               you can use it to save data structures of arbitrary shape.</p>
     *
     *               <p>Unlike most JVM ORMs, Jimmer does not specified the shape
     *               of the saved data structure by using configuration such as
     *               `insertable`, `updatable` or `cascade`; instead,
     *               it uses the dynamic nature of entity object itself to describe
     *               the shape of saved data structure, <b>without prior design</b></p>
     *
     *               <p>Unspecified properties will be ignored,
     *               only the specified properties <i>(whether null or not)</i> will be saved.
     *               In addition to objects with only id property, any associated objects
     *               will result in deeper recursive saves.</p>
     *
     *               <ul>
     *                  <li>The save mode of aggregate-root is {@link SaveMode#UPSERT}</li>
     *                  <li>The associated save mode of associated objects is {@link AssociatedSaveMode#MERGE}</li>
     *               </ul>
     * @return The save result for single object
     * @param <E> The type of inserted entity
     */
    default <E> SimpleSaveResult<E> merge(@NotNull E entity) {
        return saveCommand(entity)
                .setAssociatedModeAll(AssociatedSaveMode.MERGE)
                .execute();
    }

    /**
     * Merge an input DTO
     * @param input The merged input DTO
     *
     *               <p>In terms of internal mechanisms, any type of Input DTO is
     *               automatically converted into an entity object of the same type.</p>
     *
     *               <p>Note: The jimmer entity is <b>not POJO</b>,
     *               it can easily express data structures of arbitrary shape,
     *               you can use it to save data structures of arbitrary shape.</p>
     *
     *               <p>Unlike most JVM ORMs, Jimmer does not specified the shape
     *               of the saved data structure by using configuration such as
     *               `insertable`, `updatable` or `cascade`; instead,
     *               it uses the dynamic nature of entity object itself to describe
     *               the shape of saved data structure, <b>without prior design</b></p>
     *
     *               <p>Unspecified properties will be ignored,
     *               only the specified properties <i>(whether null or not)</i> will be saved.
     *               In addition to objects with only id property, any associated objects
     *               will result in deeper recursive saves.</p>
     *
     *               <ul>
     *                  <li>The save mode of aggregate-root is {@link SaveMode#UPSERT}</li>
     *                  <li>The associated save mode of associated objects is {@link AssociatedSaveMode#MERGE}</li>
     *               </ul>
     * @return The save result for single object
     * @param <E> The type of inserted entity
     */
    default <E> SimpleSaveResult<E> merge(@NotNull Input<E> input) {
        return saveCommand(input)
                .setAssociatedModeAll(AssociatedSaveMode.MERGE)
                .execute();
    }

    /**
     * Save some entities objects
     * @param entities The saved entity objects.
     *
     *               <p>Note: The jimmer entity is <b>not POJO</b>,
     *               it can easily express data structures of arbitrary shape,
     *               you can use it to save data structures of arbitrary shape.</p>
     *
     *               <p>Unlike most JVM ORMs, Jimmer does not specified the shape
     *               of the saved data structure by using configuration such as
     *               `insertable`, `updatable` or `cascade`; instead,
     *               it uses the dynamic nature of entity object itself to describe
     *               the shape of saved data structure, <b>without prior design</b></p>
     *
     *               <p>Unspecified properties will be ignored,
     *               only the specified properties <i>(whether null or not)</i> will be saved.
     *               In addition to objects with only id property, any associated objects
     *               will result in deeper recursive saves.</p>
     * @param mode The save mode of aggregate-roots
     * @param associatedMode The save mode of associated-objects
     * @param <E> The type of saved entities
     * @return The saved result for multiple objects
     */
    default <E> BatchSaveResult<E> saveEntities(
            @NotNull Iterable<E> entities,
            @NotNull SaveMode mode,
            @NotNull AssociatedSaveMode associatedMode
    ) {
        return saveEntitiesCommand(entities)
                .setMode(mode)
                .setAssociatedModeAll(associatedMode)
                .execute();
    }

    /**
     * Save some entities objects
     * @param entities The saved entity objects.
     *
     *               <p>Note: The jimmer entity is <b>not POJO</b>,
     *               it can easily express data structures of arbitrary shape,
     *               you can use it to save data structures of arbitrary shape.</p>
     *
     *               <p>Unlike most JVM ORMs, Jimmer does not specified the shape
     *               of the saved data structure by using configuration such as
     *               `insertable`, `updatable` or `cascade`; instead,
     *               it uses the dynamic nature of entity object itself to describe
     *               the shape of saved data structure, <b>without prior design</b></p>
     *
     *               <p>Unspecified properties will be ignored,
     *               only the specified properties <i>(whether null or not)</i> will be saved.
     *               In addition to objects with only id property, any associated objects
     *               will result in deeper recursive saves.</p>
     *
     *               <p>The associated save mode of associated objects is {@link AssociatedSaveMode#REPLACE}</p>
     * @param mode The save mode of aggregate-roots
     * @param <E> The type of saved entities
     * @return The saved result for multiple objects
     */
    default <E> BatchSaveResult<E> saveEntities(@NotNull Iterable<E> entities, @NotNull SaveMode mode) {
        return saveEntitiesCommand(entities)
                .setMode(mode)
                .execute();
    }

    /**
     * Save some entities objects
     * @param entities The saved entity objects.
     *
     *               <p>Note: The jimmer entity is <b>not POJO</b>,
     *               it can easily express data structures of arbitrary shape,
     *               you can use it to save data structures of arbitrary shape.</p>
     *
     *               <p>Unlike most JVM ORMs, Jimmer does not specified the shape
     *               of the saved data structure by using configuration such as
     *               `insertable`, `updatable` or `cascade`; instead,
     *               it uses the dynamic nature of entity object itself to describe
     *               the shape of saved data structure, <b>without prior design</b></p>
     *
     *               <p>Unspecified properties will be ignored,
     *               only the specified properties <i>(whether null or not)</i> will be saved.
     *               In addition to objects with only id property, any associated objects
     *               will result in deeper recursive saves.</p>
     *
     *               <p>The save mode of aggregate-roots is {@link SaveMode#UPSERT}</p>
     * @param associatedMode The save mode of associated-objects
     * @param <E> The type of saved entities
     * @return The saved result for multiple objects
     */
    default <E> BatchSaveResult<E> saveEntities(@NotNull Iterable<E> entities, @NotNull AssociatedSaveMode associatedMode) {
        return saveEntitiesCommand(entities)
                .setAssociatedModeAll(associatedMode)
                .execute();
    }

    /**
     * Save some entities objects
     * @param entities The saved entity objects.
     *
     *               <p>Note: The jimmer entity is <b>not POJO</b>,
     *               it can easily express data structures of arbitrary shape,
     *               you can use it to save data structures of arbitrary shape.</p>
     *
     *               <p>Unlike most JVM ORMs, Jimmer does not specified the shape
     *               of the saved data structure by using configuration such as
     *               `insertable`, `updatable` or `cascade`; instead,
     *               it uses the dynamic nature of entity object itself to describe
     *               the shape of saved data structure, <b>without prior design</b></p>
     *
     *               <p>Unspecified properties will be ignored,
     *               only the specified properties <i>(whether null or not)</i> will be saved.
     *               In addition to objects with only id property, any associated objects
     *               will result in deeper recursive saves.</p>
     *
     *               <ul>
     *                 <li>The save mode of aggregate-roots is {@link SaveMode#UPSERT}</li>
     *                 <li>The associated save mode of aggregated objects is {@link AssociatedSaveMode#REPLACE}</li>
     *               </ul>
     * @param <E> The type of saved entities
     * @return The saved result for multiple objects
     */
    default <E> BatchSaveResult<E> saveEntities(@NotNull Iterable<E> entities) {
        return saveEntitiesCommand(entities)
                .execute();
    }

    /**
     * Save some input DTOs
     * @param inputs The saved input DTOs.
     *
     *               <p>In terms of internal mechanisms, any type of Input DTO is
     *               automatically converted into an entity object of the same type.</p>
     *
     *               <p>Note: The jimmer entity is <b>not POJO</b>,
     *               it can easily express data structures of arbitrary shape,
     *               you can use it to save data structures of arbitrary shape.</p>
     *
     *               <p>Unlike most JVM ORMs, Jimmer does not specified the shape
     *               of the saved data structure by using configuration such as
     *               `insertable`, `updatable` or `cascade`; instead,
     *               it uses the dynamic nature of entity object itself to describe
     *               the shape of saved data structure, <b>without prior design</b></p>
     *
     *               <p>Unspecified properties will be ignored,
     *               only the specified properties <i>(whether null or not)</i> will be saved.
     *               In addition to objects with only id property, any associated objects
     *               will result in deeper recursive saves.</p>
     * @param mode The save mode of aggregate-roots
     * @param associatedMode The save mode of associated-objects
     * @param <E> The type of saved entities
     * @return The saved result for multiple objects
     */
    default <E> BatchSaveResult<E> saveInputs(
            @NotNull Iterable<Input<E>> inputs,
            @NotNull SaveMode mode,
            @NotNull AssociatedSaveMode associatedMode
    ) {
        return saveInputsCommand(inputs)
                .setMode(mode)
                .setAssociatedModeAll(associatedMode)
                .execute();
    }

    /**
     * Save some input DTOs
     * @param inputs The saved input DTOs.
     *
     *               <p>In terms of internal mechanisms, any type of Input DTO is
     *               automatically converted into an entity object of the same type.</p>
     *
     *               <p>Note: The jimmer entity is <b>not POJO</b>,
     *               it can easily express data structures of arbitrary shape,
     *               you can use it to save data structures of arbitrary shape.</p>
     *
     *               <p>Unlike most JVM ORMs, Jimmer does not specified the shape
     *               of the saved data structure by using configuration such as
     *               `insertable`, `updatable` or `cascade`; instead,
     *               it uses the dynamic nature of entity object itself to describe
     *               the shape of saved data structure, <b>without prior design</b></p>
     *
     *               <p>Unspecified properties will be ignored,
     *               only the specified properties <i>(whether null or not)</i> will be saved.
     *               In addition to objects with only id property, any associated objects
     *               will result in deeper recursive saves.</p>
     *
     *               <p>The associated save mode of associated objects is {@link AssociatedSaveMode#REPLACE}</p>
     * @param mode The save mode of aggregate-roots
     * @param <E> The type of saved entities
     * @return The saved result for multiple objects
     */
    default <E> BatchSaveResult<E> saveInputs(@NotNull Iterable<Input<E>> inputs, @NotNull SaveMode mode) {
        return saveInputsCommand(inputs)
                .setMode(mode)
                .execute();
    }

    /**
     * Save some input DTOs
     * @param inputs The saved input DTOs.
     *
     *               <p>In terms of internal mechanisms, any type of Input DTO is
     *               automatically converted into an entity object of the same type.</p>
     *
     *               <p>Note: The jimmer entity is <b>not POJO</b>,
     *               it can easily express data structures of arbitrary shape,
     *               you can use it to save data structures of arbitrary shape.</p>
     *
     *               <p>Unlike most JVM ORMs, Jimmer does not specified the shape
     *               of the saved data structure by using configuration such as
     *               `insertable`, `updatable` or `cascade`; instead,
     *               it uses the dynamic nature of entity object itself to describe
     *               the shape of saved data structure, <b>without prior design</b></p>
     *
     *               <p>Unspecified properties will be ignored,
     *               only the specified properties <i>(whether null or not)</i> will be saved.
     *               In addition to objects with only id property, any associated objects
     *               will result in deeper recursive saves.</p>
     *
     *               <p>The save mode of aggregate-roots is {@link SaveMode#UPSERT}</p>
     * @param associatedMode The save mode of associated-objects
     * @param <E> The type of saved entities
     * @return The saved result for multiple objects
     */
    default <E> BatchSaveResult<E> saveInputs(@NotNull Iterable<Input<E>> inputs, AssociatedSaveMode associatedMode) {
        return saveInputsCommand(inputs)
                .setAssociatedModeAll(associatedMode)
                .execute();
    }

    /**
     * Save some input DTOs
     * @param inputs The saved input DTOs.
     *
     *               <p>In terms of internal mechanisms, any type of Input DTO is
     *               automatically converted into an entity object of the same type.</p>
     *
     *               <p>Note: The jimmer entity is <b>not POJO</b>,
     *               it can easily express data structures of arbitrary shape,
     *               you can use it to save data structures of arbitrary shape.</p>
     *
     *               <p>Unlike most JVM ORMs, Jimmer does not specified the shape
     *               of the saved data structure by using configuration such as
     *               `insertable`, `updatable` or `cascade`; instead,
     *               it uses the dynamic nature of entity object itself to describe
     *               the shape of saved data structure, <b>without prior design</b></p>
     *
     *               <p>Unspecified properties will be ignored,
     *               only the specified properties <i>(whether null or not)</i> will be saved.
     *               In addition to objects with only id property, any associated objects
     *               will result in deeper recursive saves.</p>
     *
     *               <ul>
     *                  <li>The save mode of aggregate-roots is {@link SaveMode#UPSERT}</li>
     *                  <li>The associated save mode of associated objects is {@link AssociatedSaveMode#REPLACE}</li>
     *               </ul>
     * @param <E> The type of saved entities
     * @return The saved result for multiple objects
     */
    default <E> BatchSaveResult<E> saveInputs(@NotNull Iterable<Input<E>> inputs) {
        return saveInputsCommand(inputs)
                .execute();
    }

    /**
     * Insert some entities objects
     * @param entities The inserted entity objects.
     *
     *               <p>Note: The jimmer entity is <b>not POJO</b>,
     *               it can easily express data structures of arbitrary shape,
     *               you can use it to save data structures of arbitrary shape.</p>
     *
     *               <p>Unlike most JVM ORMs, Jimmer does not specified the shape
     *               of the saved data structure by using configuration such as
     *               `insertable`, `updatable` or `cascade`; instead,
     *               it uses the dynamic nature of entity object itself to describe
     *               the shape of saved data structure, <b>without prior design</b></p>
     *
     *               <p>Unspecified properties will be ignored,
     *               only the specified properties <i>(whether null or not)</i> will be saved.
     *               In addition to objects with only id property, any associated objects
     *               will result in deeper recursive saves.</p>
     * @param associatedMode The save mode of associated-objects
     * @param <E> The type of saved entities
     * @return The saved result for multiple objects
     */
    default <E> BatchSaveResult<E> insertEntities(
            @NotNull Iterable<E> entities,
            @NotNull AssociatedSaveMode associatedMode
    ) {
        return saveEntitiesCommand(entities)
                .setMode(SaveMode.INSERT_ONLY)
                .setAssociatedModeAll(associatedMode)
                .execute();
    }

    /**
     * Insert some entities objects
     * @param entities The inserted entity objects.
     *
     *               <p>Note: The jimmer entity is <b>not POJO</b>,
     *               it can easily express data structures of arbitrary shape,
     *               you can use it to save data structures of arbitrary shape.</p>
     *
     *               <p>Unlike most JVM ORMs, Jimmer does not specified the shape
     *               of the saved data structure by using configuration such as
     *               `insertable`, `updatable` or `cascade`; instead,
     *               it uses the dynamic nature of entity object itself to describe
     *               the shape of saved data structure, <b>without prior design</b></p>
     *
     *               <p>Unspecified properties will be ignored,
     *               only the specified properties <i>(whether null or not)</i> will be saved.
     *               In addition to objects with only id property, any associated objects
     *               will result in deeper recursive saves.</p>
     *
     *               <p>The associated save mode of associated objects is {@link AssociatedSaveMode#APPEND}</p>
     * @param <E> The type of saved entities
     * @return The saved result for multiple objects
     */
    default <E> BatchSaveResult<E> insertEntities(@NotNull Iterable<E> entities) {
        return saveEntitiesCommand(entities)
                .setMode(SaveMode.INSERT_ONLY)
                .setAssociatedModeAll(AssociatedSaveMode.APPEND)
                .execute();
    }

    /**
     * Insert some input DTOs
     * @param inputs The inserted input DTOs.
     *
     *               <p>In terms of internal mechanisms, any type of Input DTO is
     *               automatically converted into an entity object of the same type.</p>
     *
     *               <p>Note: The jimmer entity is <b>not POJO</b>,
     *               it can easily express data structures of arbitrary shape,
     *               you can use it to save data structures of arbitrary shape.</p>
     *
     *               <p>Unlike most JVM ORMs, Jimmer does not specified the shape
     *               of the saved data structure by using configuration such as
     *               `insertable`, `updatable` or `cascade`; instead,
     *               it uses the dynamic nature of entity object itself to describe
     *               the shape of saved data structure, <b>without prior design</b></p>
     *
     *               <p>Unspecified properties will be ignored,
     *               only the specified properties <i>(whether null or not)</i> will be saved.
     *               In addition to objects with only id property, any associated objects
     *               will result in deeper recursive saves.</p>
     * @param associatedMode The save mode of associated-objects
     * @param <E> The type of saved entities
     * @return The saved result for multiple objects
     */
    default <E> BatchSaveResult<E> insertInputs(
            @NotNull Iterable<Input<E>> inputs,
            @NotNull AssociatedSaveMode associatedMode
    ) {
        return saveInputsCommand(inputs)
                .setMode(SaveMode.INSERT_ONLY)
                .setAssociatedModeAll(associatedMode)
                .execute();
    }

    /**
     * Insert some input DTOs
     * @param inputs The inserted input DTOs.
     *
     *               <p>In terms of internal mechanisms, any type of Input DTO is
     *               automatically converted into an entity object of the same type.</p>
     *
     *               <p>Note: The jimmer entity is <b>not POJO</b>,
     *               it can easily express data structures of arbitrary shape,
     *               you can use it to save data structures of arbitrary shape.</p>
     *
     *               <p>Unlike most JVM ORMs, Jimmer does not specified the shape
     *               of the saved data structure by using configuration such as
     *               `insertable`, `updatable` or `cascade`; instead,
     *               it uses the dynamic nature of entity object itself to describe
     *               the shape of saved data structure, <b>without prior design</b></p>
     *
     *               <p>Unspecified properties will be ignored,
     *               only the specified properties <i>(whether null or not)</i> will be saved.
     *               In addition to objects with only id property, any associated objects
     *               will result in deeper recursive saves.</p>
     *
     *               <p>The associated save mode of associated objects is {@link AssociatedSaveMode#APPEND}</p>
     * @param <E> The type of saved entities
     * @return The saved result for multiple objects
     */
    default <E> BatchSaveResult<E> insertInputs(@NotNull Iterable<Input<E>> inputs) {
        return saveInputsCommand(inputs)
                .setMode(SaveMode.INSERT_ONLY)
                .setAssociatedModeAll(AssociatedSaveMode.APPEND_IF_ABSENT)
                .execute();
    }

    /**
     * Insert some entity objects if necessary,
     * if some entity objects exists in database, ignore them.
     * @param entities The inserted entity objects.
     *
     *               <p>Note: The jimmer entity is <b>not POJO</b>,
     *               it can easily express data structures of arbitrary shape,
     *               you can use it to save data structures of arbitrary shape.</p>
     *
     *               <p>Unlike most JVM ORMs, Jimmer does not specified the shape
     *               of the saved data structure by using configuration such as
     *               `insertable`, `updatable` or `cascade`; instead,
     *               it uses the dynamic nature of entity object itself to describe
     *               the shape of saved data structure, <b>without prior design</b></p>
     *
     *               <p>Unspecified properties will be ignored,
     *               only the specified properties <i>(whether null or not)</i> will be saved.
     *               In addition to objects with only id property, any associated objects
     *               will result in deeper recursive saves.</p>
     * @param associatedMode The save mode of associated-objects
     * @param <E> The type of saved entities
     * @return The saved result for multiple objects
     */
    default <E> BatchSaveResult<E> insertEntitiesIfAbsent(
            @NotNull Iterable<E> entities,
            @NotNull AssociatedSaveMode associatedMode
    ) {
        return saveEntitiesCommand(entities)
                .setMode(SaveMode.INSERT_IF_ABSENT)
                .setAssociatedModeAll(associatedMode)
                .execute();
    }

    /**
     * Insert some entity objects if necessary,
     * if some entity objects exists in database, ignore them.
     *
     *               <p>Note: The jimmer entity is <b>not POJO</b>,
     *               it can easily express data structures of arbitrary shape,
     *               you can use it to save data structures of arbitrary shape.</p>
     *
     *               <p>Unlike most JVM ORMs, Jimmer does not specified the shape
     *               of the saved data structure by using configuration such as
     *               `insertable`, `updatable` or `cascade`; instead,
     *               it uses the dynamic nature of entity object itself to describe
     *               the shape of saved data structure, <b>without prior design</b></p>
     *
     *               <p>Unspecified properties will be ignored,
     *               only the specified properties <i>(whether null or not)</i> will be saved.
     *               In addition to objects with only id property, any associated objects
     *               will result in deeper recursive saves.</p>
     *
     *               <p>The associated save mode of associated objects is {@link AssociatedSaveMode#APPEND_IF_ABSENT}</p>
     * @param <E> The type of saved entities
     * @return The saved result for multiple objects
     */
    default <E> BatchSaveResult<E> insertEntitiesIfAbsent(@NotNull Iterable<E> entities) {
        return saveEntitiesCommand(entities)
                .setMode(SaveMode.INSERT_IF_ABSENT)
                .setAssociatedModeAll(AssociatedSaveMode.APPEND_IF_ABSENT)
                .execute();
    }

    /**
     * Insert some DTOs if necessary,
     * if some DTOs exists in database, ignore them.
     * @param inputs The inserted input DTOs.
     *
     *               <p>In terms of internal mechanisms, any type of Input DTO is
     *               automatically converted into an entity object of the same type.</p>
     *
     *               <p>Note: The jimmer entity is <b>not POJO</b>,
     *               it can easily express data structures of arbitrary shape,
     *               you can use it to save data structures of arbitrary shape.</p>
     *
     *               <p>Unlike most JVM ORMs, Jimmer does not specified the shape
     *               of the saved data structure by using configuration such as
     *               `insertable`, `updatable` or `cascade`; instead,
     *               it uses the dynamic nature of entity object itself to describe
     *               the shape of saved data structure, <b>without prior design</b></p>
     *
     *               <p>Unspecified properties will be ignored,
     *               only the specified properties <i>(whether null or not)</i> will be saved.
     *               In addition to objects with only id property, any associated objects
     *               will result in deeper recursive saves.</p>
     * @param associatedMode The save mode of associated-objects
     * @param <E> The type of saved entities
     * @return The saved result for multiple objects
     */
    default <E> BatchSaveResult<E> insertInputsIfAbsent(
            @NotNull Iterable<Input<E>> inputs,
            @NotNull AssociatedSaveMode associatedMode
    ) {
        return saveInputsCommand(inputs)
                .setMode(SaveMode.INSERT_IF_ABSENT)
                .setAssociatedModeAll(associatedMode)
                .execute();
    }

    /**
     * Insert some DTOs if necessary,
     * if some DTOs exists in database, ignore them.
     * @param inputs The inserted input DTOs.
     *
     *               <p>In terms of internal mechanisms, any type of Input DTO is
     *               automatically converted into an entity object of the same type.</p>
     *
     *               <p>Note: The jimmer entity is <b>not POJO</b>,
     *               it can easily express data structures of arbitrary shape,
     *               you can use it to save data structures of arbitrary shape.</p>
     *
     *               <p>Unlike most JVM ORMs, Jimmer does not specified the shape
     *               of the saved data structure by using configuration such as
     *               `insertable`, `updatable` or `cascade`; instead,
     *               it uses the dynamic nature of entity object itself to describe
     *               the shape of saved data structure, <b>without prior design</b></p>
     *
     *               <p>Unspecified properties will be ignored,
     *               only the specified properties <i>(whether null or not)</i> will be saved.
     *               In addition to objects with only id property, any associated objects
     *               will result in deeper recursive saves.</p>
     *
     *               <p>The associated save mode of associated objects is {@link AssociatedSaveMode#APPEND_IF_ABSENT}</p>
     * @param <E> The type of saved entities
     * @return The saved result for multiple objects
     */
    default <E> BatchSaveResult<E> insertInputsIfAbsent(@NotNull Iterable<Input<E>> inputs) {
        return saveInputsCommand(inputs)
                .setMode(SaveMode.INSERT_IF_ABSENT)
                .setAssociatedModeAll(AssociatedSaveMode.APPEND_IF_ABSENT)
                .execute();
    }

    /**
     * Update some entities objects
     * @param entities The updated entity objects.
     *
     *               <p>Note: The jimmer entity is <b>not POJO</b>,
     *               it can easily express data structures of arbitrary shape,
     *               you can use it to save data structures of arbitrary shape.</p>
     *
     *               <p>Unlike most JVM ORMs, Jimmer does not specified the shape
     *               of the saved data structure by using configuration such as
     *               `insertable`, `updatable` or `cascade`; instead,
     *               it uses the dynamic nature of entity object itself to describe
     *               the shape of saved data structure, <b>without prior design</b></p>
     *
     *               <p>Unspecified properties will be ignored,
     *               only the specified properties <i>(whether null or not)</i> will be saved.
     *               In addition to objects with only id property, any associated objects
     *               will result in deeper recursive saves.</p>
     * @param associatedMode The save mode of associated-objects
     * @param <E> The type of saved entities
     * @return The saved result for multiple objects
     */
    default <E> BatchSaveResult<E> updateEntities(
            @NotNull Iterable<E> entities,
            @NotNull AssociatedSaveMode associatedMode
    ) {
        return saveEntitiesCommand(entities)
                .setMode(SaveMode.UPDATE_ONLY)
                .setAssociatedModeAll(associatedMode)
                .execute();
    }

    /**
     * Update some entities objects
     * @param entities The updated entity objects.
     *
     *               <p>Note: The jimmer entity is <b>not POJO</b>,
     *               it can easily express data structures of arbitrary shape,
     *               you can use it to save data structures of arbitrary shape.</p>
     *
     *               <p>Unlike most JVM ORMs, Jimmer does not specified the shape
     *               of the saved data structure by using configuration such as
     *               `insertable`, `updatable` or `cascade`; instead,
     *               it uses the dynamic nature of entity object itself to describe
     *               the shape of saved data structure, <b>without prior design</b></p>
     *
     *               <p>Unspecified properties will be ignored,
     *               only the specified properties <i>(whether null or not)</i> will be saved.
     *               In addition to objects with only id property, any associated objects
     *               will result in deeper recursive saves.</p>
     *
     *               <p>The associated save mode of associated objects is {@link AssociatedSaveMode#UPDATE}</p>
     * @param <E> The type of saved entities
     * @return The saved result for multiple objects
     */
    default <E> BatchSaveResult<E> updateEntities(@NotNull Iterable<E> entities) {
        return saveEntitiesCommand(entities)
                .setMode(SaveMode.UPDATE_ONLY)
                .setAssociatedModeAll(AssociatedSaveMode.UPDATE)
                .execute();
    }

    /**
     * Update some input DTOs
     * @param inputs The updated input DTOs.
     *
     *               <p>In terms of internal mechanisms, any type of Input DTO is
     *               automatically converted into an entity object of the same type.</p>
     *
     *               <p>Note: The jimmer entity is <b>not POJO</b>,
     *               it can easily express data structures of arbitrary shape,
     *               you can use it to save data structures of arbitrary shape.</p>
     *
     *               <p>Unlike most JVM ORMs, Jimmer does not specified the shape
     *               of the saved data structure by using configuration such as
     *               `insertable`, `updatable` or `cascade`; instead,
     *               it uses the dynamic nature of entity object itself to describe
     *               the shape of saved data structure, <b>without prior design</b></p>
     *
     *               <p>Unspecified properties will be ignored,
     *               only the specified properties <i>(whether null or not)</i> will be saved.
     *               In addition to objects with only id property, any associated objects
     *               will result in deeper recursive saves.</p>
     * @param associatedMode The save mode of associated-objects
     * @param <E> The type of saved entities
     * @return The saved result for multiple objects
     */
    default <E> BatchSaveResult<E> updateInputs(
            @NotNull Iterable<Input<E>> inputs,
            @NotNull AssociatedSaveMode associatedMode
    ) {
        return saveInputsCommand(inputs)
                .setMode(SaveMode.UPDATE_ONLY)
                .setAssociatedModeAll(associatedMode)
                .execute();
    }

    /**
     * Update some input DTOs
     * @param inputs The updated input DTOs.
     *
     *               <p>In terms of internal mechanisms, any type of Input DTO is
     *               automatically converted into an entity object of the same type.</p>
     *
     *               <p>Note: The jimmer entity is <b>not POJO</b>,
     *               it can easily express data structures of arbitrary shape,
     *               you can use it to save data structures of arbitrary shape.</p>
     *
     *               <p>Unlike most JVM ORMs, Jimmer does not specified the shape
     *               of the saved data structure by using configuration such as
     *               `insertable`, `updatable` or `cascade`; instead,
     *               it uses the dynamic nature of entity object itself to describe
     *               the shape of saved data structure, <b>without prior design</b></p>
     *
     *               <p>Unspecified properties will be ignored,
     *               only the specified properties <i>(whether null or not)</i> will be saved.
     *               In addition to objects with only id property, any associated objects
     *               will result in deeper recursive saves.</p>
     *
     *               <p>The associated save mode of associated objects is {@link AssociatedSaveMode#UPDATE}</p>
     * @param <E> The type of saved entities
     * @return The saved result for multiple objects
     */
    default <E> BatchSaveResult<E> updateInputs(@NotNull Iterable<Input<E>> inputs) {
        return saveInputsCommand(inputs)
                .setMode(SaveMode.UPDATE_ONLY)
                .setAssociatedModeAll(AssociatedSaveMode.UPDATE)
                .execute();
    }

    /**
     * Merge some entities objects
     * @param entities The merged entity objects.
     *
     *               <p>Note: The jimmer entity is <b>not POJO</b>,
     *               it can easily express data structures of arbitrary shape,
     *               you can use it to save data structures of arbitrary shape.</p>
     *
     *               <p>Unlike most JVM ORMs, Jimmer does not specified the shape
     *               of the saved data structure by using configuration such as
     *               `insertable`, `updatable` or `cascade`; instead,
     *               it uses the dynamic nature of entity object itself to describe
     *               the shape of saved data structure, <b>without prior design</b></p>
     *
     *               <p>Unspecified properties will be ignored,
     *               only the specified properties <i>(whether null or not)</i> will be saved.
     *               In addition to objects with only id property, any associated objects
     *               will result in deeper recursive saves.</p>
     *
     *               <p>The associated save mode of associated objects is {@link AssociatedSaveMode#MERGE}</p>
     * @param <E> The type of saved entities
     * @return The saved result for multiple objects
     */
    default <E> BatchSaveResult<E> mergeEntities(@NotNull Iterable<E> entities) {
        return saveEntitiesCommand(entities)
                .setAssociatedModeAll(AssociatedSaveMode.MERGE)
                .execute();
    }

    /**
     * Merge some input DTOs
     * @param inputs The merged input DTOs.
     *
     *               <p>In terms of internal mechanisms, any type of Input DTO is
     *               automatically converted into an entity object of the same type.</p>
     *
     *               <p>Note: The jimmer entity is <b>not POJO</b>,
     *               it can easily express data structures of arbitrary shape,
     *               you can use it to save data structures of arbitrary shape.</p>
     *
     *               <p>Unlike most JVM ORMs, Jimmer does not specified the shape
     *               of the saved data structure by using configuration such as
     *               `insertable`, `updatable` or `cascade`; instead,
     *               it uses the dynamic nature of entity object itself to describe
     *               the shape of saved data structure, <b>without prior design</b></p>
     *
     *               <p>Unspecified properties will be ignored,
     *               only the specified properties <i>(whether null or not)</i> will be saved.
     *               In addition to objects with only id property, any associated objects
     *               will result in deeper recursive saves.</p>
     *
     *               <p>The associated save mode of associated objects is {@link AssociatedSaveMode#MERGE}</p>
     * @param <E> The type of saved entities
     * @return The saved result for multiple objects
     */
    default <E> BatchSaveResult<E> mergeInputs(@NotNull Iterable<Input<E>> inputs) {
        return saveInputsCommand(inputs)
                .setAssociatedModeAll(AssociatedSaveMode.MERGE)
                .execute();
    }
}
