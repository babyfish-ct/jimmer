package org.babyfish.jimmer.sql.kt.ast.mutation

import org.babyfish.jimmer.Input
import org.babyfish.jimmer.View
import org.babyfish.jimmer.sql.ast.mutation.AssociatedSaveMode
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.babyfish.jimmer.sql.fetcher.Fetcher
import kotlin.reflect.KClass

interface KDeprecatedSaveOperations : KSaveOperations {

    /**
     * Insert an entity object
     * @param entity The inserted entity object.
     *
     * **Note: The jimmer entity is <b>not POJO**,
     * it can easily express data structures of arbitrary shape,
     * you can use it to save data structures of arbitrary shape.
     *
     * Unlike most JVM ORMs, Jimmer does not specified the shape
     * of the saved data structure by using configuration such as
     * `insertable`, `updatable` or `cascade`; instead,
     * it uses the dynamic nature of entity object itself to describe
     * the shape of saved data structure, **without prior design**
     *
     * Unspecified properties will be ignored,
     * only the specified properties *(whether null or not)* will be saved.
     * In addition to objects with only id property, any associated objects
     * will result in deeper recursive saves.
     *
     * The [SaveMode] of aggregate-root is [SaveMode.INSERT_ONLY]
     *
     * @param associatedMode The save mode of associated-objects.
     * This parameter is optional and its default value [AssociatedSaveMode.APPEND]
     *
     * @param con An optional JDBC connection. If it is null,
     * a connection will be brown from [org.babyfish.jimmer.sql.runtime.ConnectionManager]
     *
     * @param block An optional lambda to add additional configuration.
     * In addition to setting the [SaveMode] of aggregate root
     * and the default value of the [AssociatedSaveMode] of associated objects,
     * you can add any other configuration.
     *
     * @param [E] The type of saved entity
     *
     * @return The saved result for single object
     */
    @Deprecated("Will be removed")
    fun <E : Any> insert(
        entity: E,
        associatedMode: AssociatedSaveMode = AssociatedSaveMode.APPEND,
        block: (KSaveCommandPartialDsl.() -> Unit)? = null
    ): KSimpleSaveResult<E> =
        save(entity) {
            setMode(SaveMode.INSERT_ONLY)
            setAssociatedModeAll(associatedMode)
            block?.invoke(this)
        }

    /**
     * Insert an input DTO
     * @param input The inserted input DTOs.
     *
     * In terms of internal mechanisms, any type of Input DTO is
     * automatically converted into an entity object of the same type
     *
     * **Note: The jimmer entity is <b>not POJO**,
     * it can easily express data structures of arbitrary shape,
     * you can use it to save data structures of arbitrary shape.
     *
     * Unlike most JVM ORMs, Jimmer does not specified the shape
     * of the saved data structure by using configuration such as
     * `insertable`, `updatable` or `cascade`; instead,
     * it uses the dynamic nature of entity object itself to describe
     * the shape of saved data structure, **without prior design**
     *
     * Unspecified properties will be ignored,
     * only the specified properties *(whether null or not)* will be saved.
     * In addition to objects with only id property, any associated objects
     * will result in deeper recursive saves.
     *
     * The [SaveMode] of aggregate-root is [SaveMode.INSERT_ONLY]
     *
     * @param associatedMode The save mode of associated-objects.
     * This parameter is optional and its default value [AssociatedSaveMode.APPEND]
     *
     * @param con An optional JDBC connection. If it is null,
     * a connection will be brown from [org.babyfish.jimmer.sql.runtime.ConnectionManager]
     *
     * @param block An optional lambda to add additional configuration.
     * In addition to setting the [SaveMode] of aggregate root
     * and the default value of the [AssociatedSaveMode] of associated objects,
     * you can add any other configuration.
     *
     * @param [E] The type of saved entity
     *
     * @return The saved result for single object
     */
    @Deprecated("Will be removed")
    fun <E : Any> insert(
        input: Input<E>,
        associatedMode: AssociatedSaveMode = AssociatedSaveMode.APPEND,
        block: (KSaveCommandPartialDsl.() -> Unit)? = null
    ): KSimpleSaveResult<E> =
        save(input.toEntity()) {
            setMode(SaveMode.INSERT_ONLY)
            setAssociatedModeAll(associatedMode)
            block?.invoke(this)
        }

    /**
     * Insert an entity object if necessary,
     * if the entity object exists in database, ignore it.
     * - If the value of id property decorated by [Id] is specified,
     *   use id value to check whether the entity object exists in database</li>
     * - otherwise, if the values of key properties decorated by [Key] is specified
     *   use key values to check whether the entity object exists in database</li>
     * - If neither value of id property nor values of key properties is specified,
     *   exception will be raised.
     *
     * @param entity The inserted entity object.
     *
     * **Note: The jimmer entity is <b>not POJO**,
     * it can easily express data structures of arbitrary shape,
     * you can use it to save data structures of arbitrary shape.
     *
     * Unlike most JVM ORMs, Jimmer does not specified the shape
     * of the saved data structure by using configuration such as
     * `insertable`, `updatable` or `cascade`; instead,
     * it uses the dynamic nature of entity object itself to describe
     * the shape of saved data structure, **without prior design**
     *
     * Unspecified properties will be ignored,
     * only the specified properties *(whether null or not)* will be saved.
     * In addition to objects with only id property, any associated objects
     * will result in deeper recursive saves.
     *
     * The [SaveMode] of aggregate-root is [SaveMode.INSERT_IF_ABSENT]
     *
     * @param associatedMode The save mode of associated-objects.
     * This parameter is optional and its default value [AssociatedSaveMode.APPEND_IF_ABSENT]
     *
     * @param con An optional JDBC connection. If it is null,
     * a connection will be brown from [org.babyfish.jimmer.sql.runtime.ConnectionManager]
     *
     * @param block An optional lambda to add additional configuration.
     * In addition to setting the [SaveMode] of aggregate root
     * and the default value of the [AssociatedSaveMode] of associated objects,
     * you can add any other configuration.
     *
     * @param [E] The type of saved entity
     *
     * @return The saved result for single object
     */
    @Deprecated("Will be removed")
    fun <E : Any> insertIfAbsent(
        entity: E,
        associatedMode: AssociatedSaveMode = AssociatedSaveMode.APPEND_IF_ABSENT,
        block: (KSaveCommandPartialDsl.() -> Unit)? = null
    ): KSimpleSaveResult<E> =
        save(entity) {
            setMode(SaveMode.INSERT_IF_ABSENT)
            setAssociatedModeAll(associatedMode)
            block?.invoke(this)
        }

    /**
     * Insert an input DTO if necessary, that means to convert
     * the input DTO to entity object and save it.
     * If the entity object exists in database, ignore it.
     *
     * - If the value of id property decorated by [Id] is specified,
     *   use id value to check whether the entity object exists in database</li>
     * - otherwise, if the values of key properties decorated by [Key] is specified
     *   use key values to check whether the entity object exists in database</li>
     * - If neither value of id property nor values of key properties is specified,
     *   exception will be raised.
     *
     * @param input The inserted input DTOs.
     *
     * In terms of internal mechanisms, any type of Input DTO is
     * automatically converted into an entity object of the same type
     *
     * **Note: The jimmer entity is <b>not POJO**,
     * it can easily express data structures of arbitrary shape,
     * you can use it to save data structures of arbitrary shape.
     *
     * Unlike most JVM ORMs, Jimmer does not specified the shape
     * of the saved data structure by using configuration such as
     * `insertable`, `updatable` or `cascade`; instead,
     * it uses the dynamic nature of entity object itself to describe
     * the shape of saved data structure, **without prior design**
     *
     * Unspecified properties will be ignored,
     * only the specified properties *(whether null or not)* will be saved.
     * In addition to objects with only id property, any associated objects
     * will result in deeper recursive saves.
     *
     * The [SaveMode] of aggregate-root is [SaveMode.INSERT_IF_ABSENT]
     *
     * @param associatedMode The save mode of associated-objects.
     * This parameter is optional and its default value [AssociatedSaveMode.APPEND_IF_ABSENT]
     *
     * @param con An optional JDBC connection. If it is null,
     * a connection will be brown from [org.babyfish.jimmer.sql.runtime.ConnectionManager]
     *
     * @param block An optional lambda to add additional configuration.
     * In addition to setting the [SaveMode] of aggregate root
     * and the default value of the [AssociatedSaveMode] of associated objects,
     * you can add any other configuration.
     *
     * @param [E] The type of saved entity
     *
     * @return The saved result for single object
     */
    @Deprecated("Will be removed")
    fun <E : Any> insertIfAbsent(
        input: Input<E>,
        associatedMode: AssociatedSaveMode = AssociatedSaveMode.APPEND_IF_ABSENT,
        block: (KSaveCommandPartialDsl.() -> Unit)? = null
    ): KSimpleSaveResult<E> =
        save(input.toEntity()) {
            setMode(SaveMode.INSERT_IF_ABSENT)
            setAssociatedModeAll(associatedMode)
            block?.invoke(this)
        }

    /**
     * Update an entity object
     * @param entity The updated entity object.
     *
     * **Note: The jimmer entity is <b>not POJO**,
     * it can easily express data structures of arbitrary shape,
     * you can use it to save data structures of arbitrary shape.
     *
     * Unlike most JVM ORMs, Jimmer does not specified the shape
     * of the saved data structure by using configuration such as
     * `insertable`, `updatable` or `cascade`; instead,
     * it uses the dynamic nature of entity object itself to describe
     * the shape of saved data structure, **without prior design**
     *
     * Unspecified properties will be ignored,
     * only the specified properties *(whether null or not)* will be saved.
     * In addition to objects with only id property, any associated objects
     * will result in deeper recursive saves.
     *
     * The [SaveMode] of aggregate-root is [SaveMode.UPDATE_ONLY]
     *
     * @param associatedMode The save mode of associated-objects.
     * This parameter is optional and its default value [AssociatedSaveMode.UPDATE]
     *
     * @param con An optional JDBC connection. If it is null,
     * a connection will be brown from [org.babyfish.jimmer.sql.runtime.ConnectionManager]
     *
     * @param block An optional lambda to add additional configuration.
     * In addition to setting the [SaveMode] of aggregate root
     * and the default value of the [AssociatedSaveMode] of associated objects,
     * you can add any other configuration.
     *
     * @param [E] The type of saved entity
     *
     * @return The saved result for single object
     */
    @Deprecated("Will be removed")
    fun <E : Any> update(
        entity: E,
        associatedMode: AssociatedSaveMode = AssociatedSaveMode.UPDATE,
        block: (KSaveCommandPartialDsl.() -> Unit)? = null
    ): KSimpleSaveResult<E> =
        save(entity) {
            setMode(SaveMode.UPDATE_ONLY)
            setAssociatedModeAll(associatedMode)
            block?.invoke(this)
        }

    /**
     * Update an input DTO
     * @param input The updated input DTOs.
     *
     * In terms of internal mechanisms, any type of Input DTO is
     * automatically converted into an entity object of the same type
     *
     * **Note: The jimmer entity is <b>not POJO**,
     * it can easily express data structures of arbitrary shape,
     * you can use it to save data structures of arbitrary shape.
     *
     * Unlike most JVM ORMs, Jimmer does not specified the shape
     * of the saved data structure by using configuration such as
     * `insertable`, `updatable` or `cascade`; instead,
     * it uses the dynamic nature of entity object itself to describe
     * the shape of saved data structure, **without prior design**
     *
     * Unspecified properties will be ignored,
     * only the specified properties *(whether null or not)* will be saved.
     * In addition to objects with only id property, any associated objects
     * will result in deeper recursive saves.
     *
     * The [SaveMode] of aggregate-root is [SaveMode.UPDATE_ONLY]
     *
     * @param associatedMode The save mode of associated-objects.
     * This parameter is optional and its default value [AssociatedSaveMode.UPDATE]
     *
     * @param con An optional JDBC connection. If it is null,
     * a connection will be brown from [org.babyfish.jimmer.sql.runtime.ConnectionManager]
     *
     * @param block An optional lambda to add additional configuration.
     * In addition to setting the [SaveMode] of aggregate root
     * and the default value of the [AssociatedSaveMode] of associated objects,
     * you can add any other configuration.
     *
     * @param [E] The type of saved entity
     *
     * @return The saved result for single object
     */
    @Deprecated("Will be removed")
    fun <E : Any> update(
        input: Input<E>,
        associatedMode: AssociatedSaveMode = AssociatedSaveMode.UPDATE,
        block: (KSaveCommandPartialDsl.() -> Unit)? = null
    ): KSimpleSaveResult<E> =
        save(input.toEntity()) {
            setMode(SaveMode.UPDATE_ONLY)
            setAssociatedModeAll(associatedMode)
            block?.invoke(this)
        }

    /**
     * Merge an entity object
     * @param entity The merged entity object.
     *
     * **Note: The jimmer entity is <b>not POJO**,
     * it can easily express data structures of arbitrary shape,
     * you can use it to save data structures of arbitrary shape.
     *
     * Unlike most JVM ORMs, Jimmer does not specified the shape
     * of the saved data structure by using configuration such as
     * `insertable`, `updatable` or `cascade`; instead,
     * it uses the dynamic nature of entity object itself to describe
     * the shape of saved data structure, **without prior design**
     *
     * Unspecified properties will be ignored,
     * only the specified properties *(whether null or not)* will be saved.
     * In addition to objects with only id property, any associated objects
     * will result in deeper recursive saves.
     *
     * The [SaveMode] of aggregate-root is [SaveMode.UPSERT]
     *
     * @param associatedMode The save mode of associated-objects.
     * This parameter is optional and its default value [AssociatedSaveMode.UPDATE]
     *
     * @param con An optional JDBC connection. If it is null,
     * a connection will be brown from [org.babyfish.jimmer.sql.runtime.ConnectionManager]
     *
     * @param block An optional lambda to add additional configuration.
     * In addition to setting the [SaveMode] of aggregate root
     * and the default value of the [AssociatedSaveMode] of associated objects,
     * you can add any other configuration.
     *
     * @param [E] The type of saved entity
     *
     * @return The saved result for single object
     */
    @Deprecated("Will be removed")
    fun <E : Any> merge(
        entity: E,
        block: (KSaveCommandPartialDsl.() -> Unit)? = null
    ): KSimpleSaveResult<E> =
        save(entity) {
            setMode(SaveMode.UPSERT)
            setAssociatedModeAll(AssociatedSaveMode.MERGE)
            block?.invoke(this)
        }

    /**
     * Merge an input DTO
     * @param input The merged input DTOs.
     *
     * In terms of internal mechanisms, any type of Input DTO is
     * automatically converted into an entity object of the same type
     *
     * **Note: The jimmer entity is <b>not POJO**,
     * it can easily express data structures of arbitrary shape,
     * you can use it to save data structures of arbitrary shape.
     *
     * Unlike most JVM ORMs, Jimmer does not specified the shape
     * of the saved data structure by using configuration such as
     * `insertable`, `updatable` or `cascade`; instead,
     * it uses the dynamic nature of entity object itself to describe
     * the shape of saved data structure, **without prior design**
     *
     * Unspecified properties will be ignored,
     * only the specified properties *(whether null or not)* will be saved.
     * In addition to objects with only id property, any associated objects
     * will result in deeper recursive saves.
     *
     * The [SaveMode] of aggregate-root is [SaveMode.UPSERT]
     *
     * @param associatedMode The save mode of associated-objects.
     * This parameter is optional and its default value [AssociatedSaveMode.MERGE]
     *
     * @param con An optional JDBC connection. If it is null,
     * a connection will be brown from [org.babyfish.jimmer.sql.runtime.ConnectionManager]
     *
     * @param block An optional lambda to add additional configuration.
     * In addition to setting the [SaveMode] of aggregate root
     * and the default value of the [AssociatedSaveMode] of associated objects,
     * you can add any other configuration.
     *
     * @param [E] The type of saved entity
     *
     * @return The saved result for single object
     */
    @Deprecated("Will be removed")
    fun <E : Any> merge(
        input: Input<E>,
        block: (KSaveCommandPartialDsl.() -> Unit)? = null
    ): KSimpleSaveResult<E> =
        save(input.toEntity()) {
            setMode(SaveMode.UPSERT)
            setAssociatedModeAll(AssociatedSaveMode.MERGE)
            block?.invoke(this)
        }

    @Deprecated("Will be removed")
    fun <E: Any> insertEntities(
        entities: Iterable<E>,
        associatedMode: AssociatedSaveMode = AssociatedSaveMode.APPEND,
        block: (KSaveCommandPartialDsl.() -> Unit)? = null
    ): KBatchSaveResult<E> =
        this.saveEntities(entities) {
            setMode(SaveMode.INSERT_ONLY)
            setAssociatedModeAll(associatedMode)
            block?.invoke(this)
        }

    @Deprecated("Will be removed")
    fun <E: Any> insertInputs(
        inputs: Iterable<Input<E>>,
        associatedMode: AssociatedSaveMode = AssociatedSaveMode.APPEND,
        block: (KSaveCommandPartialDsl.() -> Unit)? = null
    ): KBatchSaveResult<E> =
        this.saveInputs(inputs) {
            setMode(SaveMode.INSERT_ONLY)
            setAssociatedModeAll(associatedMode)
            block?.invoke(this)
        }

    @Deprecated("Will be removed")
    fun <E: Any> insertEntitiesIfAbsent(
        entities: Iterable<E>,
        associatedMode: AssociatedSaveMode = AssociatedSaveMode.APPEND_IF_ABSENT,
        block: (KSaveCommandPartialDsl.() -> Unit)? = null
    ): KBatchSaveResult<E> =
        this.saveEntities(entities) {
            setMode(SaveMode.INSERT_IF_ABSENT)
            setAssociatedModeAll(associatedMode)
            block?.invoke(this)
        }

    @Deprecated("Will be removed")
    fun <E: Any> insertInputsIfAbsent(
        inputs: Iterable<Input<E>>,
        associatedMode: AssociatedSaveMode = AssociatedSaveMode.APPEND_IF_ABSENT,
        block: (KSaveCommandPartialDsl.() -> Unit)? = null
    ): KBatchSaveResult<E> =
        this.saveInputs(inputs) {
            setMode(SaveMode.INSERT_IF_ABSENT)
            setAssociatedModeAll(associatedMode)
            block?.invoke(this)
        }

    @Deprecated("Will be removed")
    fun <E: Any> updateEntities(
        entities: Iterable<E>,
        associatedMode: AssociatedSaveMode = AssociatedSaveMode.UPDATE,
        block: (KSaveCommandPartialDsl.() -> Unit)? = null
    ): KBatchSaveResult<E> =
        this.saveEntities(entities) {
            setMode(SaveMode.UPDATE_ONLY)
            setAssociatedModeAll(associatedMode)
            block?.invoke(this)
        }

    @Deprecated("Will be removed")
    fun <E: Any> updateInputs(
        inputs: Iterable<Input<E>>,
        associatedMode: AssociatedSaveMode = AssociatedSaveMode.UPDATE,
        block: (KSaveCommandPartialDsl.() -> Unit)? = null
    ): KBatchSaveResult<E> =
        this.saveInputs(inputs) {
            setMode(SaveMode.UPDATE_ONLY)
            setAssociatedModeAll(associatedMode)
            block?.invoke(this)
        }

    @Deprecated("Will be removed")
    fun <E: Any> mergeEntities(
        entities: Iterable<E>,
        block: (KSaveCommandPartialDsl.() -> Unit)? = null
    ): KBatchSaveResult<E> =
        this.saveEntities(entities) {
            setMode(SaveMode.UPSERT)
            setAssociatedModeAll(AssociatedSaveMode.MERGE)
            block?.invoke(this)
        }

    @Deprecated("Will be removed")
    fun <E: Any> mergeInputs(
        inputs: Iterable<Input<E>>,
        block: (KSaveCommandPartialDsl.() -> Unit)? = null
    ): KBatchSaveResult<E> =
        this.saveInputs(inputs) {
            setMode(SaveMode.UPSERT)
            setAssociatedModeAll(AssociatedSaveMode.MERGE)
            block?.invoke(this)
        }

    @Deprecated(
        "fetcher/viewType is advanced feature, please use saveCommand",
        replaceWith = ReplaceWith("saveCommand(entity, block).execute(fetcher)")
    )
    fun <E: Any> save(
        entity: E,
        fetcher: Fetcher<E>,
        block: (KSaveCommandDsl.() -> Unit)? = null
    ): KSimpleSaveResult<E> =
        saveCommand(entity, block)
            .execute(fetcher)

    @Deprecated(
        "fetcher/viewType is advanced feature, please use saveCommand",
        replaceWith = ReplaceWith("saveCommand(entity, mode, associatedMode, block).execute(fetcher)")
    )
    fun <E: Any> save(
        entity: E,
        mode: SaveMode,
        associatedMode: AssociatedSaveMode,
        fetcher: Fetcher<E>,
        block: (KSaveCommandPartialDsl.() -> Unit)? = null
    ): KSimpleSaveResult<E> =
        saveCommand(entity, mode, associatedMode, block)
            .execute(fetcher)

    @Deprecated(
        "fetcher/viewType is advanced feature, please use saveCommand",
        replaceWith = ReplaceWith("saveEntitiesCommand(entities, block).execute(fetcher)")
    )
    fun <E: Any> saveEntities(
        entities: Iterable<E>,
        fetcher: Fetcher<E>,
        block: (KSaveCommandDsl.() -> Unit)? = null
    ): KBatchSaveResult<E> =
        saveEntitiesCommand(entities, block)
            .execute(fetcher)

    @Deprecated(
        "fetcher/viewType is advanced feature, please use saveCommand",
        replaceWith = ReplaceWith("saveEntitiesCommand(entities, mode, associatedMode, block).execute(fetcher)")
    )
    fun <E: Any> saveEntities(
        entities: Iterable<E>,
        mode: SaveMode,
        associatedMode: AssociatedSaveMode,
        fetcher: Fetcher<E>,
        block: (KSaveCommandPartialDsl.() -> Unit)? = null
    ): KBatchSaveResult<E> =
        saveEntitiesCommand(entities, mode, associatedMode, block)
            .execute(fetcher)

    @Deprecated(
        "fetcher/viewType is advanced feature, please use saveCommand",
        replaceWith = ReplaceWith("saveCommand(input, block).execute(fetcher)")
    )
    fun <E: Any> save(
        input: Input<E>,
        fetcher: Fetcher<E>,
        block: (KSaveCommandDsl.() -> Unit)? = null
    ): KSimpleSaveResult<E> =
        saveCommand(input, block)
            .execute(fetcher)

    @Deprecated(
        "fetcher/viewType is advanced feature, please use saveCommand",
        replaceWith = ReplaceWith("saveCommand(input, mode, associatedMode, block).execute(fetcher)")
    )
    fun <E: Any> save(
        input: Input<E>,
        mode: SaveMode,
        associatedMode: AssociatedSaveMode,
        fetcher: Fetcher<E>,
        block: (KSaveCommandPartialDsl.() -> Unit)? = null
    ): KSimpleSaveResult<E> =
        saveCommand(input, mode, associatedMode, block)
            .execute(fetcher)

    @Deprecated(
        "fetcher/viewType is advanced feature, please use saveCommand",
        replaceWith = ReplaceWith("saveInputsCommand(inputs, block).execute(fetcher)")
    )
    fun <E: Any> saveInputs(
        inputs: Iterable<Input<E>>,
        fetcher: Fetcher<E>,
        block: (KSaveCommandDsl.() -> Unit)? = null
    ): KBatchSaveResult<E> =
        saveInputsCommand(inputs, block)
            .execute(fetcher)

    @Deprecated(
        "fetcher/viewType is advanced feature, please use saveCommand",
        replaceWith = ReplaceWith("saveInputsCommand(inputs, mode, associatedMode, block).execute(fetcher)")
    )
    fun <E: Any> saveInputs(
        inputs: Iterable<Input<E>>,
        mode: SaveMode,
        associatedMode: AssociatedSaveMode,
        fetcher: Fetcher<E>,
        block: (KSaveCommandPartialDsl.() -> Unit)? = null
    ): KBatchSaveResult<E> =
        saveInputsCommand(inputs, mode, associatedMode, block)
            .execute(fetcher)

    @Deprecated(
        "fetcher/viewType is advanced feature, please use saveCommand",
        replaceWith = ReplaceWith("saveCommand(entity, block).execute(viewType)")
    )
    fun <E: Any, V: View<E>> save(
        entity: E,
        viewType: KClass<V>,
        block: (KSaveCommandDsl.() -> Unit)? = null
    ): KSimpleSaveResult.View<E, V> =
        saveCommand(entity, block)
            .execute(viewType)

    @Deprecated(
        "fetcher/viewType is advanced feature, please use saveCommand",
        replaceWith = ReplaceWith("saveCommand(entity, mode, associatedMode, block).execute(viewType)")
    )
    fun <E: Any, V: View<E>> save(
        entity: E,
        mode: SaveMode,
        associatedMode: AssociatedSaveMode,
        viewType: KClass<V>,
        block: (KSaveCommandPartialDsl.() -> Unit)? = null
    ): KSimpleSaveResult.View<E, V> =
        saveCommand(entity, mode, associatedMode, block)
            .execute(viewType)

    @Deprecated(
        "fetcher/viewType is advanced feature, please use saveCommand",
        replaceWith = ReplaceWith("saveEntitiesCommand(entities, block).execute(viewType)")
    )
    fun <E: Any, V: View<E>> saveEntities(
        entities: Iterable<E>,
        viewType: KClass<V>,
        block: (KSaveCommandDsl.() -> Unit)? = null
    ): KBatchSaveResult.View<E, V> =
        saveEntitiesCommand(entities, block)
            .execute(viewType)

    @Deprecated(
        "fetcher/viewType is advanced feature, please use saveCommand",
        replaceWith = ReplaceWith("saveEntitiesCommand(entities, mode, associatedMode, block).execute(viewType)")
    )
    fun <E: Any, V: View<E>> saveEntities(
        entities: Iterable<E>,
        mode: SaveMode,
        associatedMode: AssociatedSaveMode,
        viewType: KClass<V>,
        block: (KSaveCommandPartialDsl.() -> Unit)? = null
    ): KBatchSaveResult.View<E, V> =
        saveEntitiesCommand(entities, mode, associatedMode, block)
            .execute(viewType)

    @Deprecated(
        "fetcher/viewType is advanced feature, please use saveCommand",
        replaceWith = ReplaceWith("saveCommand(input, block).execute(viewType)")
    )
    fun <E: Any, V: View<E>> save(
        input: Input<E>,
        viewType: KClass<V>,
        block: (KSaveCommandDsl.() -> Unit)? = null
    ): KSimpleSaveResult.View<E, V> =
        saveCommand(input, block)
            .execute(viewType)

    @Deprecated(
        "fetcher/viewType is advanced feature, please use saveCommand",
        replaceWith = ReplaceWith("saveCommand(input, mode, associatedMode, block).execute(viewType)")
    )
    fun <E: Any, V: View<E>> save(
        input: Input<E>,
        mode: SaveMode,
        associatedMode: AssociatedSaveMode,
        viewType: KClass<V>,
        block: (KSaveCommandPartialDsl.() -> Unit)? = null
    ): KSimpleSaveResult.View<E, V> =
        saveCommand(input, mode, associatedMode, block)
            .execute(viewType)

    @Deprecated(
        "fetcher/viewType is advanced feature, please use saveCommand",
        replaceWith = ReplaceWith("saveInputsCommand(inputs, block).execute(viewType)")
    )
    fun <E: Any, V: View<E>> saveInputs(
        inputs: Iterable<Input<E>>,
        viewType: KClass<V>,
        block: (KSaveCommandDsl.() -> Unit)? = null
    ): KBatchSaveResult.View<E, V> =
        saveInputsCommand(inputs, block)
            .execute(viewType)

    @Deprecated(
        "fetcher/viewType is advanced feature, please use saveCommand",
        replaceWith = ReplaceWith("saveInputsCommand(inputs, mode, associatedMode, block).execute(viewType)")
    )
    fun <E: Any, V: View<E>> saveInputs(
        inputs: Iterable<Input<E>>,
        mode: SaveMode,
        associatedMode: AssociatedSaveMode,
        viewType: KClass<V>,
        block: (KSaveCommandPartialDsl.() -> Unit)? = null
    ): KBatchSaveResult.View<E, V> =
        saveInputsCommand(inputs, mode, associatedMode, block)
            .execute(viewType)
}
