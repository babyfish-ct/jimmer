package org.babyfish.jimmer.sql.ast.mutation;

import org.babyfish.jimmer.Input;
import org.babyfish.jimmer.View;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.jetbrains.annotations.NotNull;

public interface SaveOptions extends SaveCommandCreator {

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
     * @param fetcher The fetcher of modified entity, if it is null, no shape will be guaranteed
     * @return The saved result for single object
     */
    default <E> SimpleSaveResult<E> save(
            E entity,
            SaveMode mode,
            @NotNull AssociatedSaveMode associatedMode,
            Fetcher<E> fetcher
    ) {
        return saveCommand(entity)
                .setMode(mode)
                .setAssociatedModeAll(associatedMode)
                .execute(fetcher);
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
     * @param fetcher The fetcher of modified entity, if it is null, no shape will be guaranteed
     * @return The saved result for single object
     */
    default <E> SimpleSaveResult<E> save(
            E entity,
            SaveMode mode,
            Fetcher<E> fetcher
    ) {
        return saveCommand(entity)
                .setMode(mode)
                .execute(fetcher);
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
     * @param fetcher The fetcher of modified entity, if it is null, no shape will be guaranteed
     * @return The saved result for single object
     */
    default <E> SimpleSaveResult<E> save(
            E entity,
            AssociatedSaveMode associatedMode,
            Fetcher<E> fetcher
    ) {
        return saveCommand(entity)
                .setAssociatedModeAll(associatedMode)
                .execute(fetcher);
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
     * @param fetcher The fetcher of modified entity, if it is null, no shape will be guaranteed
     * @return The saved result for single object
     */
    default <E> SimpleSaveResult<E> save(E entity, Fetcher<E> fetcher) {
        return saveCommand(entity).execute(fetcher);
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
     * @param fetcher The fetcher of modified entity, if it is null, no shape will be guaranteed
     * @return The saved result for single object
     */
    default <E> SimpleSaveResult<E> save(
            Input<E> input,
            SaveMode mode,
            AssociatedSaveMode associatedMode,
            Fetcher<E> fetcher
    ) {
        return saveCommand(input.toEntity())
                .setMode(mode)
                .setAssociatedModeAll(associatedMode)
                .execute(fetcher);
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
     * @param fetcher The fetcher of modified entity, if it is null, no shape will be guaranteed
     * @return The saved result for single object
     */
    default <E> SimpleSaveResult<E> save(Input<E> input, SaveMode mode, Fetcher<E> fetcher) {
        return saveCommand(input.toEntity())
                .setMode(mode)
                .execute(fetcher);
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
     * @param fetcher The fetcher of modified entity, if it is null, no shape will be guaranteed
     * @return The saved result for single object
     */
    default <E> SimpleSaveResult<E> save(
            Input<E> input,
            AssociatedSaveMode associatedMode,
            Fetcher<E> fetcher
    ) {
        return saveCommand(input.toEntity())
                .setAssociatedModeAll(associatedMode)
                .execute(fetcher);
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
     * @param fetcher The fetcher of modified entity, if it is null, no shape will be guaranteed
     * @return The saved result for single object
     */
    default <E> SimpleSaveResult<E> save(Input<E> input, Fetcher<E> fetcher) {
        return saveCommand(input.toEntity()).execute(fetcher);
    }

    /**
     * Save an entity object
     * @param <E> The type of saved entity
     * @param <V> The output DTO type of modified entity
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
     * @param viewType The Ouptut DTO Type of modified entity,it cannot be null
     * @return The saved result for single object
     */
    default <E, V extends View<E>> SimpleSaveResult.View<E, V> save(
            E entity,
            SaveMode mode,
            AssociatedSaveMode associatedMode,
            Class<V> viewType
    ) {
        return saveCommand(entity)
                .setMode(mode)
                .setAssociatedModeAll(associatedMode)
                .execute(viewType);
    }

    /**
     * Save an entity object
     * @param <E> The type of saved entity
     * @param <V> The output DTO type of modified entity
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
     * @param viewType The Ouptut DTO Type of modified entity,it cannot be null
     * @return The saved result for single object
     */
    default <E, V extends View<E>> SimpleSaveResult.View<E, V> save(
            E entity,
            SaveMode mode,
            Class<V> viewType
    ) {
        return saveCommand(entity)
                .setMode(mode)
                .execute(viewType);
    }

    /**
     * Save an entity object
     * @param <E> The type of saved entity
     * @param <V> The output DTO type of modified entity
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
     * @param viewType The Ouptut DTO Type of modified entity,it cannot be null
     * @return The saved result for single object
     */
    default <E, V extends View<E>> SimpleSaveResult.View<E, V> save(
            E entity,
            AssociatedSaveMode associatedMode,
            Class<V> viewType
    ) {
        return saveCommand(entity)
                .setAssociatedModeAll(associatedMode)
                .execute(viewType);
    }

    /**
     * Save an entity object
     * @param <E> The type of saved entity
     * @param <V> The output DTO type of modified entity
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
     * @param viewType The Ouptut DTO Type of modified entity,it cannot be null
     * @return The saved result for single object
     */
    default <E, V extends View<E>> SimpleSaveResult.View<E, V> save(E entity, Class<V> viewType) {
        return saveCommand(entity).execute(viewType);
    }

    /**
     * Save an input DTO
     * @param <E> The type of saved entity
     * @param <V> The output DTO type of modified entity
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
     * @param viewType The Ouptut DTO Type of modified entity,it cannot be null
     * @return The saved result for single object
     */
    default <E, V extends View<E>> SimpleSaveResult.View<E, V> save(
            Input<E> input,
            SaveMode mode,
            AssociatedSaveMode associatedMode,
            Class<V> viewType
    ) {
        return saveCommand(input.toEntity())
                .setMode(mode)
                .setAssociatedModeAll(associatedMode)
                .execute(viewType);
    }

    /**
     * Save an input DTO
     * @param <E> The type of saved entity
     * @param <V> The output DTO type of modified entity
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
     * @param viewType The Ouptut DTO Type of modified entity,it cannot be null
     * @return The saved result for single object
     */
    default <E, V extends View<E>> SimpleSaveResult.View<E, V> save(Input<E> input, SaveMode mode, Class<V> viewType) {
        return saveCommand(input.toEntity())
                .setMode(mode)
                .execute(viewType);
    }

    /**
     * Save an input DTO
     * @param <E> The type of saved entity
     * @param <V> The output DTO type of modified entity
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
     * @param viewType The Ouptut DTO Type of modified entity,it cannot be null
     * @return The saved result for single object
     */
    default <E, V extends View<E>> SimpleSaveResult.View<E, V> save(
            Input<E> input,
            AssociatedSaveMode associatedMode,
            Class<V> viewType
    ) {
        return saveCommand(input.toEntity())
                .setAssociatedModeAll(associatedMode)
                .execute(viewType);
    }

    /**
     * Save an input DTO
     * @param <E> The type of saved entity
     * @param <V> The output DTO type of modified entity
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
     * @param viewType The Ouptut DTO Type of modified entity,it cannot be null
     * @return The saved result for single object
     */
    default <E, V extends View<E>> SimpleSaveResult.View<E, V> save(Input<E> input, Class<V> viewType) {
        return saveCommand(input.toEntity()).execute(viewType);
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
            @NotNull Iterable<? extends Input<E>> inputs,
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
    default <E> BatchSaveResult<E> saveInputs(@NotNull Iterable<? extends Input<E>> inputs, @NotNull SaveMode mode) {
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
    default <E> BatchSaveResult<E> saveInputs(@NotNull Iterable<? extends Input<E>> inputs, AssociatedSaveMode associatedMode) {
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
    default <E> BatchSaveResult<E> saveInputs(@NotNull Iterable<? extends Input<E>> inputs) {
        return saveInputsCommand(inputs)
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
     * @param fetcher The fetcher of modified entity, if it is null, no shape will be guaranteed
     * @param <E> The type of saved entities
     * @return The saved result for multiple objects
     */
    default <E> BatchSaveResult<E> saveEntities(
            @NotNull Iterable<E> entities,
            @NotNull SaveMode mode,
            @NotNull AssociatedSaveMode associatedMode,
            Fetcher<E> fetcher
    ) {
        return saveEntitiesCommand(entities)
                .setMode(mode)
                .setAssociatedModeAll(associatedMode)
                .execute(fetcher);
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
     * @param fetcher The fetcher of modified entity, if it is null, no shape will be guaranteed
     * @param <E> The type of saved entities
     * @return The saved result for multiple objects
     */
    default <E> BatchSaveResult<E> saveEntities(
            @NotNull Iterable<E> entities,
            @NotNull SaveMode mode,
            Fetcher<E> fetcher
    ) {
        return saveEntitiesCommand(entities)
                .setMode(mode)
                .execute(fetcher);
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
     * @param fetcher The fetcher of modified entity, if it is null, no shape will be guaranteed
     * @param <E> The type of saved entities
     * @return The saved result for multiple objects
     */
    default <E> BatchSaveResult<E> saveEntities(
            @NotNull Iterable<E> entities,
            @NotNull AssociatedSaveMode associatedMode,
            Fetcher<E> fetcher
    ) {
        return saveEntitiesCommand(entities)
                .setAssociatedModeAll(associatedMode)
                .execute(fetcher);
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
     * @param fetcher The fetcher of modified entity, if it is null, no shape will be guaranteed
     * @param <E> The type of saved entities
     * @return The saved result for multiple objects
     */
    default <E> BatchSaveResult<E> saveEntities(
            @NotNull Iterable<E> entities,
            Fetcher<E> fetcher
    ) {
        return saveEntitiesCommand(entities)
                .execute(fetcher);
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
     * @param fetcher The fetcher of modified entity, if it is null, no shape will be guaranteed
     * @param <E> The type of saved entities
     * @return The saved result for multiple objects
     */
    default <E> BatchSaveResult<E> saveInputs(
            @NotNull Iterable<? extends Input<E>> inputs,
            @NotNull SaveMode mode,
            @NotNull AssociatedSaveMode associatedMode,
            Fetcher<E> fetcher
    ) {
        return saveInputsCommand(inputs)
                .setMode(mode)
                .setAssociatedModeAll(associatedMode)
                .execute(fetcher);
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
     * @param fetcher The fetcher of modified entity, if it is null, no shape will be guaranteed
     * @param <E> The type of saved entities
     * @return The saved result for multiple objects
     */
    default <E> BatchSaveResult<E> saveInputs(
            @NotNull Iterable<? extends Input<E>> inputs,
            @NotNull SaveMode mode,
            Fetcher<E> fetcher
    ) {
        return saveInputsCommand(inputs)
                .setMode(mode)
                .execute(fetcher);
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
     * @param fetcher The fetcher of modified entity, if it is null, no shape will be guaranteed
     * @param <E> The type of saved entities
     * @return The saved result for multiple objects
     */
    default <E> BatchSaveResult<E> saveInputs(
            @NotNull Iterable<? extends Input<E>> inputs,
            @NotNull AssociatedSaveMode associatedMode,
            Fetcher<E> fetcher
    ) {
        return saveInputsCommand(inputs)
                .setAssociatedModeAll(associatedMode)
                .execute(fetcher);
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
     * @param fetcher The fetcher of modified entity, if it is null, no shape will be guaranteed
     * @param <E> The type of saved entities
     * @return The saved result for multiple objects
     */
    default <E> BatchSaveResult<E> saveInputs(
            @NotNull Iterable<? extends Input<E>> inputs,
            Fetcher<E> fetcher
    ) {
        return saveInputsCommand(inputs)
                .execute(fetcher);
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
     * @param viewType The output DTO type of modified entity, it cannot be null
     * @param <E> The type of saved entities
     * @param <V> The output DTO type of modified entity
     * @return The saved result for multiple objects
     */
    default <E, V extends View<E>> BatchSaveResult.View<E, V> saveEntities(
            @NotNull Iterable<E> entities,
            @NotNull SaveMode mode,
            @NotNull AssociatedSaveMode associatedMode,
            @NotNull Class<V> viewType
    ) {
        return saveEntitiesCommand(entities)
                .setMode(mode)
                .setAssociatedModeAll(associatedMode)
                .execute(viewType);
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
     * @param viewType The output DTO type of modified entity, it cannot be null
     * @param <E> The type of saved entities
     * @param <V> The output DTO type of modified entity
     * @return The saved result for multiple objects
     */
    default <E, V extends View<E>> BatchSaveResult.View<E, V> saveEntities(
            @NotNull Iterable<E> entities,
            @NotNull SaveMode mode,
            @NotNull Class<V> viewType
    ) {
        return saveEntitiesCommand(entities)
                .setMode(mode)
                .execute(viewType);
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
     * @param viewType The output DTO type of modified entity, it cannot be null
     * @param <E> The type of saved entities
     * @param <V> The output DTO type of modified entity
     * @return The saved result for multiple objects
     */
    default <E, V extends View<E>> BatchSaveResult.View<E, V> saveEntities(
            @NotNull Iterable<E> entities,
            @NotNull AssociatedSaveMode associatedMode,
            @NotNull Class<V> viewType
    ) {
        return saveEntitiesCommand(entities)
                .setAssociatedModeAll(associatedMode)
                .execute(viewType);
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
     * @param viewType The output DTO type of modified entity, it cannot be null
     * @param <E> The type of saved entities
     * @param <V> The output DTO type of modified entity
     * @return The saved result for multiple objects
     */
    default <E, V extends View<E>> BatchSaveResult.View<E, V> saveEntities(
            @NotNull Iterable<E> entities,
            @NotNull Class<V> viewType
    ) {
        return saveEntitiesCommand(entities)
                .execute(viewType);
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
     * @param viewType The output DTO type of modified entity, it cannot be null
     * @param <E> The type of saved entities
     * @param <V> The output DTO type of modified entity
     * @return The saved result for multiple objects
     */
    default <E, V extends View<E>> BatchSaveResult.View<E, V> saveInputs(
            @NotNull Iterable<? extends Input<E>> inputs,
            @NotNull SaveMode mode,
            @NotNull AssociatedSaveMode associatedMode,
            @NotNull Class<V> viewType
    ) {
        return saveInputsCommand(inputs)
                .setMode(mode)
                .setAssociatedModeAll(associatedMode)
                .execute(viewType);
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
     * @param viewType The output DTO type of modified entity, it cannot be null
     * @param <E> The type of saved entities
     * @param <V> The output DTO type of modified entity
     * @return The saved result for multiple objects
     */
    default <E, V extends View<E>> BatchSaveResult.View<E, V> saveInputs(
            @NotNull Iterable<? extends Input<E>> inputs,
            @NotNull SaveMode mode,
            @NotNull Class<V> viewType
    ) {
        return saveInputsCommand(inputs)
                .setMode(mode)
                .execute(viewType);
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
     * @param viewType The output DTO type of modified entity, it cannot be null
     * @param <E> The type of saved entities
     * @param <V> The output DTO type of modified entity
     * @return The saved result for multiple objects
     */
    default <E, V extends View<E>> BatchSaveResult.View<E, V> saveInputs(
            @NotNull Iterable<? extends Input<E>> inputs,
            @NotNull AssociatedSaveMode associatedMode,
            @NotNull Class<V> viewType
    ) {
        return saveInputsCommand(inputs)
                .setAssociatedModeAll(associatedMode)
                .execute(viewType);
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
     * @param viewType The output DTO type of modified entity, it cannot be null
     * @param <E> The type of saved entities
     * @param <V> The output DTO type of modified entity
     * @return The saved result for multiple objects
     */
    default <E, V extends View<E>> BatchSaveResult.View<E, V> saveInputs(
            @NotNull Iterable<? extends Input<E>> inputs,
            @NotNull Class<V> viewType
    ) {
        return saveInputsCommand(inputs)
                .execute(viewType);
    }
}
