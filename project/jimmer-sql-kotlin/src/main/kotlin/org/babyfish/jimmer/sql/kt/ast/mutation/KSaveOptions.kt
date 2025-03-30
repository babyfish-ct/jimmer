package org.babyfish.jimmer.sql.kt.ast.mutation

import org.babyfish.jimmer.Input
import org.babyfish.jimmer.View
import org.babyfish.jimmer.sql.ast.mutation.AssociatedSaveMode
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.babyfish.jimmer.sql.fetcher.Fetcher
import java.sql.Connection
import kotlin.reflect.KClass

interface KSaveOptions : KSaveCommandCreator {

    /**
     * Save an entity object
     * @param entity The saved entity object.
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
     * - The default value of the [SaveMode] of aggregate-root is [SaveMode.UPSERT],
     *   it can be overwritten by the lambda represented by the parameter `block`
     * - The default value of the [AssociatedSaveMode] of associated objects is [AssociatedSaveMode.REPLACE],
     *   it can be overwritten by the lambda represented by the parameter `block`
     *
     * @param con An optional JDBC connection. If it is null,
     * a connection will be brown from [org.babyfish.jimmer.sql.runtime.ConnectionManager]
     *
     * @param block An optional lambda to add additional configuration.
     *
     * @param [E] The type of saved entity
     *
     * @return The saved result for single object
     */
    fun <E : Any> save(
        entity: E,
        con: Connection? = null,
        block: (KSaveCommandDsl.() -> Unit)? = null
    ): KSimpleSaveResult<E> =
        saveCommand(entity, block).execute(con)

    /**
     * Save an entity object
     * @param entity The saved entity object.
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
     * @param mode The save mode of aggregate-root
     *
     * @param associatedMode The save mode of associated-objects.
     * This parameter is optional, its default value is [AssociatedSaveMode.REPLACE]
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
    fun <E : Any> save(
        entity: E,
        mode: SaveMode,
        associatedMode: AssociatedSaveMode = AssociatedSaveMode.REPLACE,
        con: Connection? = null,
        block: (KSaveCommandPartialDsl.() -> Unit)? = null
    ): KSimpleSaveResult<E> =
        saveCommand(entity, mode, associatedMode, block).execute(con)

    /**
     * Save an entity object
     * @param entity The saved entity object.
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
    fun <E : Any> save(
        entity: E,
        associatedMode: AssociatedSaveMode,
        con: Connection? = null,
        block: (KSaveCommandPartialDsl.() -> Unit)? = null
    ): KSimpleSaveResult<E> =
        saveCommand(entity, SaveMode.UPSERT, associatedMode, block).execute(con)

    /**
     * Save an input DTO
     * @param input The saved input DTO.
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
     * - The default value of the [SaveMode] of aggregate-root is [SaveMode.UPSERT],
     *   it can be overwritten by the lambda represented by the parameter `block`
     * - The default value of the [AssociatedSaveMode] of associated objects is [AssociatedSaveMode.REPLACE],
     *   it can be overwritten by the lambda represented by the parameter `block`
     *
     * @param con An optional JDBC connection. If it is null,
     * a connection will be brown from [org.babyfish.jimmer.sql.runtime.ConnectionManager]
     *
     * @param block An optional lambda to add additional configuration.
     *
     * @param [E] The type of saved entity
     *
     * @return The saved result for single object
     */
    fun <E : Any> save(
        input: Input<E>,
        con: Connection? = null,
        block: (KSaveCommandDsl.() -> Unit)? = null
    ): KSimpleSaveResult<E> =
        saveCommand(input, block).execute(con)

    /**
     * Save an input DTO
     * @param input The saved input DTO.
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
     * @param mode The save mode of aggregate-root
     *
     * @param associatedMode The save mode of associated-objects.
     * This parameter is optional, its default value is [AssociatedSaveMode.REPLACE]
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
    fun <E : Any> save(
        input: Input<E>,
        mode: SaveMode,
        associatedMode: AssociatedSaveMode = AssociatedSaveMode.REPLACE,
        con: Connection? = null,
        block: (KSaveCommandPartialDsl.() -> Unit)? = null
    ): KSimpleSaveResult<E> =
        saveCommand(input, mode, associatedMode, block).execute(con)

    /**
     * Save an input DTO
     * @param input The saved input DTO.
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
     * This parameter is optional, its default value is [AssociatedSaveMode.REPLACE]
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
    fun <E : Any> save(
        input: Input<E>,
        associatedMode: AssociatedSaveMode,
        con: Connection? = null,
        block: (KSaveCommandPartialDsl.() -> Unit)? = null
    ): KSimpleSaveResult<E> =
        saveCommand(input, SaveMode.UPSERT, associatedMode, block).execute(con)

    /**
     * Save an entity object
     * @param entity The saved entity object.
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
     * - The default value of the [SaveMode] of aggregate-root is [SaveMode.UPSERT],
     *   it can be overwritten by the lambda represented by the parameter `block`
     * - The default value of the [AssociatedSaveMode] of associated objects is [AssociatedSaveMode.REPLACE],
     *   it can be overwritten by the lambda represented by the parameter `block`
     *
     * @param fetcher The fetcher of modified entity, if it is null, no shape will be guaranteed
     *
     * @param con An optional JDBC connection. If it is null,
     * a connection will be brown from [org.babyfish.jimmer.sql.runtime.ConnectionManager]
     *
     * @param block An optional lambda to add additional configuration.
     *
     * @param [E] The type of saved entity
     *
     * @return The saved result for single object
     */
    fun <E : Any> save(
        entity: E,
        fetcher: Fetcher<E>,
        con: Connection? = null,
        block: (KSaveCommandDsl.() -> Unit)? = null
    ): KSimpleSaveResult<E> =
        saveCommand(entity, block).execute(con, fetcher)

    /**
     * Save an entity object
     * @param entity The saved entity object.
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
     * @param mode The save mode of aggregate-root
     *
     * @param associatedMode The save mode of associated-objects.
     * This parameter is optional, its default value is [AssociatedSaveMode.REPLACE]
     *
     * @param fetcher The fetcher of modified entity, if it is null, no shape will be guaranteed
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
    fun <E : Any> save(
        entity: E,
        mode: SaveMode,
        associatedMode: AssociatedSaveMode = AssociatedSaveMode.REPLACE,
        fetcher: Fetcher<E>,
        con: Connection? = null,
        block: (KSaveCommandPartialDsl.() -> Unit)? = null
    ): KSimpleSaveResult<E> =
        saveCommand(entity, mode, associatedMode, block).execute(con, fetcher)

    /**
     * Save an entity object
     * @param entity The saved entity object.
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
     *
     * @param fetcher The fetcher of modified entity, if it is null, no shape will be guaranteed
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
    fun <E : Any> save(
        entity: E,
        associatedMode: AssociatedSaveMode,
        fetcher: Fetcher<E>,
        con: Connection? = null,
        block: (KSaveCommandPartialDsl.() -> Unit)? = null
    ): KSimpleSaveResult<E> =
        saveCommand(entity, SaveMode.UPSERT, associatedMode, block).execute(con, fetcher)

    /**
     * Save an input DTO
     * @param input The saved input DTO.
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
     * - The default value of the [SaveMode] of aggregate-root is [SaveMode.UPSERT],
     *   it can be overwritten by the lambda represented by the parameter `block`
     * - The default value of the [AssociatedSaveMode] of associated objects is [AssociatedSaveMode.REPLACE],
     *   it can be overwritten by the lambda represented by the parameter `block`
     *
     * @param fetcher The fetcher of modified entity, if it is null, no shape will be guaranteed
     *
     * @param con An optional JDBC connection. If it is null,
     * a connection will be brown from [org.babyfish.jimmer.sql.runtime.ConnectionManager]
     *
     * @param block An optional lambda to add additional configuration.
     *
     * @param [E] The type of saved entity
     *
     * @return The saved result for single object
     */
    fun <E : Any> save(
        input: Input<E>,
        fetcher: Fetcher<E>,
        con: Connection? = null,
        block: (KSaveCommandDsl.() -> Unit)? = null
    ): KSimpleSaveResult<E> =
        saveCommand(input, block).execute(con, fetcher)

    /**
     * Save an input DTO
     * @param input The saved input DTO.
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
     * @param mode The save mode of aggregate-root
     *
     * @param associatedMode The save mode of associated-objects.
     * This parameter is optional, its default value is [AssociatedSaveMode.REPLACE]
     *
     * @param fetcher The fetcher of modified entity, if it is null, no shape will be guaranteed
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
    fun <E : Any> save(
        input: Input<E>,
        mode: SaveMode,
        associatedMode: AssociatedSaveMode = AssociatedSaveMode.REPLACE,
        fetcher: Fetcher<E>,
        con: Connection? = null,
        block: (KSaveCommandPartialDsl.() -> Unit)? = null
    ): KSimpleSaveResult<E> =
        saveCommand(input, mode, associatedMode, block).execute(con, fetcher)

    /**
     * Save an input DTO
     * @param input The saved input DTO.
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
     * @param fetcher The fetcher of modified entity, if it is null, no shape will be guaranteed
     *
     * @param associatedMode The save mode of associated-objects.
     * This parameter is optional, its default value is [AssociatedSaveMode.REPLACE]
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
    fun <E : Any> save(
        input: Input<E>,
        associatedMode: AssociatedSaveMode,
        fetcher: Fetcher<E>,
        con: Connection? = null,
        block: (KSaveCommandPartialDsl.() -> Unit)? = null
    ): KSimpleSaveResult<E> =
        saveCommand(input, SaveMode.UPSERT, associatedMode, block).execute(con, fetcher)

    /**
     * Save an entity object
     * @param entity The saved entity object.
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
     * - The default value of the [SaveMode] of aggregate-root is [SaveMode.UPSERT],
     *   it can be overwritten by the lambda represented by the parameter `block`
     * - The default value of the [AssociatedSaveMode] of associated objects is [AssociatedSaveMode.REPLACE],
     *   it can be overwritten by the lambda represented by the parameter `block`
     *
     * @param viewType The output DTO type of modified entity
     *
     * @param con An optional JDBC connection. If it is null,
     * a connection will be brown from [org.babyfish.jimmer.sql.runtime.ConnectionManager]
     *
     * @param block An optional lambda to add additional configuration.
     *
     * @param [E] The type of saved entity
     *
     * @return The saved result for single object
     */
    fun <E : Any, V: View<E>> save(
        entity: E,
        viewType: KClass<V>,
        con: Connection? = null,
        block: (KSaveCommandDsl.() -> Unit)? = null
    ): KSimpleSaveResult.View<E, V> =
        saveCommand(entity, block).execute(con, viewType);

    /**
     * Save an entity object
     * @param entity The saved entity object.
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
     * @param mode The save mode of aggregate-root
     *
     * @param associatedMode The save mode of associated-objects.
     * This parameter is optional, its default value is [AssociatedSaveMode.REPLACE]
     *
     * @param viewType The output DTO type of modified entity
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
    fun <E : Any, V: View<E>> save(
        entity: E,
        mode: SaveMode,
        associatedMode: AssociatedSaveMode = AssociatedSaveMode.REPLACE,
        viewType: KClass<V>,
        con: Connection? = null,
        block: (KSaveCommandPartialDsl.() -> Unit)? = null
    ): KSimpleSaveResult.View<E, V> =
        save(entity, viewType, con) {
            setMode(mode)
            setAssociatedModeAll(associatedMode)
            block?.invoke(this)
        }

    /**
     * Save an entity object
     * @param entity The saved entity object.
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
     *
     * @param viewType The output DTO type of modified entity
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
    fun <E : Any, V: View<E>> save(
        entity: E,
        associatedMode: AssociatedSaveMode,
        viewType: KClass<V>,
        con: Connection? = null,
        block: (KSaveCommandPartialDsl.() -> Unit)? = null
    ): KSimpleSaveResult.View<E, V> =
        save(entity, viewType, con) {
            setAssociatedModeAll(associatedMode)
            block?.invoke(this)
        }

    /**
     * Save an input DTO
     * @param input The saved input DTO.
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
     * - The default value of the [SaveMode] of aggregate-root is [SaveMode.UPSERT],
     *   it can be overwritten by the lambda represented by the parameter `block`
     * - The default value of the [AssociatedSaveMode] of associated objects is [AssociatedSaveMode.REPLACE],
     *   it can be overwritten by the lambda represented by the parameter `block`
     *
     * @param viewType The output DTO type of modified entity
     *
     * @param con An optional JDBC connection. If it is null,
     * a connection will be brown from [org.babyfish.jimmer.sql.runtime.ConnectionManager]
     *
     * @param block An optional lambda to add additional configuration.
     *
     * @param [E] The type of saved entity
     *
     * @return The saved result for single object
     */
    fun <E : Any, V: View<E>> save(
        input: Input<E>,
        viewType: KClass<V>,
        con: Connection? = null,
        block: (KSaveCommandDsl.() -> Unit)? = null
    ): KSimpleSaveResult.View<E, V> =
        save(input.toEntity(), viewType, con) {
            block?.invoke(this)
        }

    /**
     * Save an input DTO
     * @param input The saved input DTO.
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
     * @param mode The save mode of aggregate-root
     *
     * @param associatedMode The save mode of associated-objects.
     * This parameter is optional, its default value is [AssociatedSaveMode.REPLACE]
     *
     * @param viewType The output DTO type of modified entity
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
    fun <E : Any, V: View<E>> save(
        input: Input<E>,
        mode: SaveMode,
        associatedMode: AssociatedSaveMode = AssociatedSaveMode.REPLACE,
        viewType: KClass<V>,
        con: Connection? = null,
        block: (KSaveCommandPartialDsl.() -> Unit)? = null
    ): KSimpleSaveResult.View<E, V> =
        save(input.toEntity(), viewType, con) {
            setMode(mode)
            setAssociatedModeAll(associatedMode)
            block?.invoke(this)
        }

    /**
     * Save an input DTO
     * @param input The saved input DTO.
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
     * @param viewType The output DTO type of modified entity
     *
     * @param associatedMode The save mode of associated-objects.
     * This parameter is optional, its default value is [AssociatedSaveMode.REPLACE]
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
    fun <E : Any, V: View<E>> save(
        input: Input<E>,
        associatedMode: AssociatedSaveMode,
        viewType: KClass<V>,
        con: Connection? = null,
        block: (KSaveCommandPartialDsl.() -> Unit)? = null
    ): KSimpleSaveResult.View<E, V> =
        save(input.toEntity(), viewType, con) {
            setAssociatedModeAll(associatedMode)
            block?.invoke(this)
        }

    /**
     * Save some entity objects
     * @param entities The saved entity objects.
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
     * - The default value of the [SaveMode] of aggregate-roots is [SaveMode.UPSERT],
     *   it can be overwritten by the lambda represented by the parameter `block`
     * - The default value of the [AssociatedSaveMode] of associated objects is [AssociatedSaveMode.REPLACE],
     *   it can be overwritten by the lambda represented by the parameter `block`
     *
     * @param con An optional JDBC connection. If it is null,
     * a connection will be brown from [org.babyfish.jimmer.sql.runtime.ConnectionManager]
     *
     * @param block An optional lambda to add additional configuration.
     *
     * @param [E] The type of saved entity
     *
     * @return The saved result for multiple objects
     */
    fun <E : Any> saveEntities(
        entities: Iterable<E>,
        con: Connection? = null,
        block: (KSaveCommandDsl.() -> Unit)? = null
    ): KBatchSaveResult<E> =
        saveEntitiesCommand(entities, block).execute(con);

    /**
     * Save some entity objects
     * @param entities The saved entity objects.
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
     * @param mode The save mode of aggregate-roots
     *
     * @param associatedMode The associated save mode for associated objects,
     *      its default value is [AssociatedSaveMode.REPLACE]
     *
     * @param con An optional JDBC connection. If it is null,
     * a connection will be brown from [org.babyfish.jimmer.sql.runtime.ConnectionManager]
     *
     * @param block An optional lambda to add additional configuration.
     *
     * @param [E] The type of saved entity
     *
     * @return The saved result for multiple objects
     */
    fun <E : Any> saveEntities(
        entities: Iterable<E>,
        mode: SaveMode,
        associatedMode: AssociatedSaveMode = AssociatedSaveMode.REPLACE,
        con: Connection? = null,
        block: (KSaveCommandPartialDsl.() -> Unit)? = null
    ): KBatchSaveResult<E> =
        saveEntities(entities, con) {
            setMode(mode)
            setAssociatedModeAll(associatedMode)
            block?.invoke(this)
        }

    /**
     * Save some entity objects
     * @param entities The saved entity objects.
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
     * - The default value of the [SaveMode] of aggregate-roots is [SaveMode.UPSERT],
     *      it can be overwritten by the lambda represented by the parameter `block`
     *
     * @param associatedMode The associated save mode for associated objects,
     *      its default value is [AssociatedSaveMode.REPLACE]
     *
     * @param con An optional JDBC connection. If it is null,
     * a connection will be brown from [org.babyfish.jimmer.sql.runtime.ConnectionManager]
     *
     * @param block An optional lambda to add additional configuration.
     *
     * @param [E] The type of saved entity
     *
     * @return The saved result for multiple objects
     */
    fun <E : Any> saveEntities(
        entities: Iterable<E>,
        associatedMode: AssociatedSaveMode,
        con: Connection? = null,
        block: (KSaveCommandPartialDsl.() -> Unit)? = null
    ): KBatchSaveResult<E> =
        saveEntities(entities, con) {
            setAssociatedModeAll(associatedMode)
            block?.invoke(this)
        }

    /**
     * Save some input DTOs
     *
     * @param inputs The saved entity inputs.
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
     * - The default value of the [SaveMode] of aggregate-roots is [SaveMode.UPSERT],
     *   it can be overwritten by the lambda represented by the parameter `block`
     * - The default value of the [AssociatedSaveMode] of associated objects is [AssociatedSaveMode.REPLACE],
     *   it can be overwritten by the lambda represented by the parameter `block`
     *
     * @param con An optional JDBC connection. If it is null,
     * a connection will be brown from [org.babyfish.jimmer.sql.runtime.ConnectionManager]
     *
     * @param block An optional lambda to add additional configuration.
     *
     * @param [E] The type of saved entity
     *
     * @return The saved result for multiple objects
     */
    fun <E : Any> saveInputs(
        inputs: Iterable<Input<E>>,
        con: Connection? = null,
        block: (KSaveCommandDsl.() -> Unit)? = null
    ): KBatchSaveResult<E> =
        this.saveEntities(inputs.map(Input<E>::toEntity), con, block)

    /**
     * Save some input DTOs
     *
     * @param inputs The saved entity inputs.
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
     * @param mode The save mode of aggregate-roots
     *
     * @param associatedMode The associated save mode of associated objects,
     *  its default value is [AssociatedSaveMode.REPLACE]
     *
     * @param con An optional JDBC connection. If it is null,
     * a connection will be brown from [org.babyfish.jimmer.sql.runtime.ConnectionManager]
     *
     * @param block An optional lambda to add additional configuration.
     *
     * @param [E] The type of saved entity
     *
     * @return The saved result for multiple objects
     */
    fun <E : Any> saveInputs(
        inputs: Iterable<Input<E>>,
        mode: SaveMode,
        associatedMode: AssociatedSaveMode = AssociatedSaveMode.REPLACE,
        con: Connection? = null,
        block: (KSaveCommandPartialDsl.() -> Unit)? = null
    ): KBatchSaveResult<E> =
        this.saveEntities(inputs.map(Input<E>::toEntity), con) {
            setMode(mode)
            setAssociatedModeAll(associatedMode)
            block?.invoke(this)
        }

    /**
     * Save some input DTOs
     *
     * @param inputs The saved entity inputs.
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
     * The default value of the [SaveMode] of aggregate-roots is [SaveMode.UPSERT],
     *   it can be overwritten by the lambda represented by the parameter `block`
     *
     * @param associatedMode The associated save mode of associated objects,
     *  its default value is [AssociatedSaveMode.REPLACE]
     *
     * @param con An optional JDBC connection. If it is null,
     * a connection will be brown from [org.babyfish.jimmer.sql.runtime.ConnectionManager]
     *
     * @param block An optional lambda to add additional configuration.
     *
     * @param [E] The type of saved entity
     *
     * @return The saved result for multiple objects
     */
    fun <E : Any> saveInputs(
        inputs: Iterable<Input<E>>,
        associatedMode: AssociatedSaveMode,
        con: Connection? = null,
        block: (KSaveCommandPartialDsl.() -> Unit)? = null
    ): KBatchSaveResult<E> =
        this.saveEntities(inputs.map(Input<E>::toEntity), con) {
            setAssociatedModeAll(associatedMode)
            block?.invoke(this)
        }

    /**
     * Save some entity objects
     * @param entities The saved entity objects.
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
     * - The default value of the [SaveMode] of aggregate-roots is [SaveMode.UPSERT],
     *   it can be overwritten by the lambda represented by the parameter `block`
     * - The default value of the [AssociatedSaveMode] of associated objects is [AssociatedSaveMode.REPLACE],
     *   it can be overwritten by the lambda represented by the parameter `block`
     *
     * @param fetcher The fetcher of the modified entity, if it is null, no shape can be no shape will be guaranteed
     *
     * @param con An optional JDBC connection. If it is null,
     * a connection will be brown from [org.babyfish.jimmer.sql.runtime.ConnectionManager]
     *
     * @param block An optional lambda to add additional configuration.
     *
     * @param [E] The type of saved entity
     *
     * @return The saved result for multiple objects
     */
    fun <E : Any> saveEntities(
        entities: Iterable<E>,
        fetcher: Fetcher<E>,
        con: Connection? = null,
        block: (KSaveCommandDsl.() -> Unit)? = null
    ): KBatchSaveResult<E> =
        saveEntitiesCommand(entities, null).execute(con, fetcher)

    /**
     * Save some entity objects
     * @param entities The saved entity objects.
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
     * @param mode The save mode of aggregate-roots
     *
     * @param associatedMode The associated save mode for associated objects,
     *      its default value is [AssociatedSaveMode.REPLACE]
     *
     * @param fetcher The fetcher of the modified entity, if it is null, no shape can be no shape will be guaranteed
     *
     * @param con An optional JDBC connection. If it is null,
     * a connection will be brown from [org.babyfish.jimmer.sql.runtime.ConnectionManager]
     *
     * @param block An optional lambda to add additional configuration.
     *
     * @param [E] The type of saved entity
     *
     * @return The saved result for multiple objects
     */
    fun <E : Any> saveEntities(
        entities: Iterable<E>,
        mode: SaveMode,
        associatedMode: AssociatedSaveMode = AssociatedSaveMode.REPLACE,
        fetcher: Fetcher<E>,
        con: Connection? = null,
        block: (KSaveCommandPartialDsl.() -> Unit)? = null
    ): KBatchSaveResult<E> =
        saveEntities(entities, fetcher, con) {
            setMode(mode)
            setAssociatedModeAll(associatedMode)
            block?.invoke(this)
        }

    /**
     * Save some entity objects
     * @param entities The saved entity objects.
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
     * - The default value of the [SaveMode] of aggregate-roots is [SaveMode.UPSERT],
     *      it can be overwritten by the lambda represented by the parameter `block`
     *
     * @param associatedMode The associated save mode for associated objects,
     *      its default value is [AssociatedSaveMode.REPLACE]
     *
     * @param fetcher The fetcher of the modified entity, if it is null, no shape can be no shape will be guaranteed
     *
     * @param con An optional JDBC connection. If it is null,
     * a connection will be brown from [org.babyfish.jimmer.sql.runtime.ConnectionManager]
     *
     * @param block An optional lambda to add additional configuration.
     *
     * @param [E] The type of saved entity
     *
     * @return The saved result for multiple objects
     */
    fun <E : Any> saveEntities(
        entities: Iterable<E>,
        associatedMode: AssociatedSaveMode,
        fetcher: Fetcher<E>,
        con: Connection? = null,
        block: (KSaveCommandPartialDsl.() -> Unit)? = null
    ): KBatchSaveResult<E> =
        saveEntities(entities, fetcher, con) {
            setAssociatedModeAll(associatedMode)
            block?.invoke(this)
        }

    /**
     * Save some input DTOs
     *
     * @param inputs The saved entity inputs.
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
     * - The default value of the [SaveMode] of aggregate-roots is [SaveMode.UPSERT],
     *   it can be overwritten by the lambda represented by the parameter `block`
     * - The default value of the [AssociatedSaveMode] of associated objects is [AssociatedSaveMode.REPLACE],
     *   it can be overwritten by the lambda represented by the parameter `block`
     *
     * @param fetcher The fetcher of the modified entity, if it is null, no shape can be no shape will be guaranteed
     *
     * @param con An optional JDBC connection. If it is null,
     * a connection will be brown from [org.babyfish.jimmer.sql.runtime.ConnectionManager]
     *
     * @param block An optional lambda to add additional configuration.
     *
     * @param [E] The type of saved entity
     *
     * @return The saved result for multiple objects
     */
    fun <E : Any> saveInputs(
        inputs: Iterable<Input<E>>,
        fetcher: Fetcher<E>,
        con: Connection? = null,
        block: (KSaveCommandDsl.() -> Unit)? = null
    ): KBatchSaveResult<E> =
        this.saveEntities(inputs.map(Input<E>::toEntity), fetcher, con, block)

    /**
     * Save some input DTOs
     *
     * @param inputs The saved entity inputs.
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
     * @param mode The save mode of aggregate-roots
     *
     * @param associatedMode The associated save mode of associated objects,
     *  its default value is [AssociatedSaveMode.REPLACE]
     *
     * @param fetcher The fetcher of the modified entity, if it is null, no shape can be no shape will be guaranteed
     *
     * @param con An optional JDBC connection. If it is null,
     * a connection will be brown from [org.babyfish.jimmer.sql.runtime.ConnectionManager]
     *
     * @param block An optional lambda to add additional configuration.
     *
     * @param [E] The type of saved entity
     *
     * @return The saved result for multiple objects
     */
    fun <E : Any> saveInputs(
        inputs: Iterable<Input<E>>,
        mode: SaveMode,
        associatedMode: AssociatedSaveMode = AssociatedSaveMode.REPLACE,
        fetcher: Fetcher<E>,
        con: Connection? = null,
        block: (KSaveCommandPartialDsl.() -> Unit)? = null
    ): KBatchSaveResult<E> =
        this.saveEntities(inputs.map(Input<E>::toEntity), con) {
            setMode(mode)
            setAssociatedModeAll(associatedMode)
            block?.invoke(this)
        }

    /**
     * Save some input DTOs
     *
     * @param inputs The saved entity inputs.
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
     * The default value of the [SaveMode] of aggregate-roots is [SaveMode.UPSERT],
     *   it can be overwritten by the lambda represented by the parameter `block`
     *
     * @param associatedMode The associated save mode of associated objects,
     *  its default value is [AssociatedSaveMode.REPLACE]
     *
     * @param fetcher The fetcher of the modified entity, if it is null, no shape can be no shape will be guaranteed
     *
     * @param con An optional JDBC connection. If it is null,
     * a connection will be brown from [org.babyfish.jimmer.sql.runtime.ConnectionManager]
     *
     * @param block An optional lambda to add additional configuration.
     *
     * @param [E] The type of saved entity
     *
     * @return The saved result for multiple objects
     */
    fun <E : Any> saveInputs(
        inputs: Iterable<Input<E>>,
        associatedMode: AssociatedSaveMode,
        fetcher: Fetcher<E>,
        con: Connection? = null,
        block: (KSaveCommandPartialDsl.() -> Unit)? = null
    ): KBatchSaveResult<E> =
        this.saveEntities(inputs.map(Input<E>::toEntity), fetcher, con) {
            setAssociatedModeAll(associatedMode)
            block?.invoke(this)
        }

    /**
     * Save some entity objects
     * @param entities The saved entity objects.
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
     * - The default value of the [SaveMode] of aggregate-roots is [SaveMode.UPSERT],
     *   it can be overwritten by the lambda represented by the parameter `block`
     * - The default value of the [AssociatedSaveMode] of associated objects is [AssociatedSaveMode.REPLACE],
     *   it can be overwritten by the lambda represented by the parameter `block`
     *
     * @param viewType The output DTO type of modified entity
     *
     * @param con An optional JDBC connection. If it is null,
     * a connection will be brown from [org.babyfish.jimmer.sql.runtime.ConnectionManager]
     *
     * @param block An optional lambda to add additional configuration.
     *
     * @param [E] The type of saved entity
     *
     * @return The saved result for multiple objects
     */
    fun <E : Any, V: View<E>> saveEntities(
        entities: Iterable<E>,
        viewType: KClass<V>,
        con: Connection? = null,
        block: (KSaveCommandDsl.() -> Unit)? = null
    ): KBatchSaveResult.View<E, V> =
        saveEntitiesCommand(entities, block).execute(con, viewType)

    /**
     * Save some entity objects
     * @param entities The saved entity objects.
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
     * @param mode The save mode of aggregate-roots
     *
     * @param associatedMode The associated save mode for associated objects,
     *      its default value is [AssociatedSaveMode.REPLACE]
     *
     * @param viewType The output DTO type of modified entity
     *
     * @param con An optional JDBC connection. If it is null,
     * a connection will be brown from [org.babyfish.jimmer.sql.runtime.ConnectionManager]
     *
     * @param block An optional lambda to add additional configuration.
     *
     * @param [E] The type of saved entity
     *
     * @return The saved result for multiple objects
     */
    fun <E : Any, V: View<E>> saveEntities(
        entities: Iterable<E>,
        mode: SaveMode,
        associatedMode: AssociatedSaveMode = AssociatedSaveMode.REPLACE,
        viewType: KClass<V>,
        con: Connection? = null,
        block: (KSaveCommandPartialDsl.() -> Unit)? = null
    ): KBatchSaveResult.View<E, V> =
        saveEntities(entities, viewType, con) {
            setMode(mode)
            setAssociatedModeAll(associatedMode)
            block?.invoke(this)
        }

    /**
     * Save some entity objects
     * @param entities The saved entity objects.
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
     * - The default value of the [SaveMode] of aggregate-roots is [SaveMode.UPSERT],
     *      it can be overwritten by the lambda represented by the parameter `block`
     *
     * @param associatedMode The associated save mode for associated objects,
     *      its default value is [AssociatedSaveMode.REPLACE]
     *
     * @param viewType The output DTO type of modified entity
     *
     * @param con An optional JDBC connection. If it is null,
     * a connection will be brown from [org.babyfish.jimmer.sql.runtime.ConnectionManager]
     *
     * @param block An optional lambda to add additional configuration.
     *
     * @param [E] The type of saved entity
     *
     * @return The saved result for multiple objects
     */
    fun <E : Any, V: View<E>> saveEntities(
        entities: Iterable<E>,
        associatedMode: AssociatedSaveMode,
        viewType: KClass<V>,
        con: Connection? = null,
        block: (KSaveCommandPartialDsl.() -> Unit)? = null
    ): KBatchSaveResult.View<E, V> =
        saveEntities(entities, viewType, con) {
            setAssociatedModeAll(associatedMode)
            block?.invoke(this)
        }

    /**
     * Save some input DTOs
     *
     * @param inputs The saved entity inputs.
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
     * - The default value of the [SaveMode] of aggregate-roots is [SaveMode.UPSERT],
     *   it can be overwritten by the lambda represented by the parameter `block`
     * - The default value of the [AssociatedSaveMode] of associated objects is [AssociatedSaveMode.REPLACE],
     *   it can be overwritten by the lambda represented by the parameter `block`
     *
     * @param viewType The output DTO type of modified entity
     *
     * @param con An optional JDBC connection. If it is null,
     * a connection will be brown from [org.babyfish.jimmer.sql.runtime.ConnectionManager]
     *
     * @param block An optional lambda to add additional configuration.
     *
     * @param [E] The type of saved entity
     *
     * @return The saved result for multiple objects
     */
    fun <E : Any, V: View<E>> saveInputs(
        inputs: Iterable<Input<E>>,
        viewType: KClass<V>,
        con: Connection? = null,
        block: (KSaveCommandDsl.() -> Unit)? = null
    ): KBatchSaveResult.View<E, V> =
        this.saveEntities(inputs.map(Input<E>::toEntity), viewType, con, block)

    /**
     * Save some input DTOs
     *
     * @param inputs The saved entity inputs.
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
     * @param mode The save mode of aggregate-roots
     *
     * @param associatedMode The associated save mode of associated objects,
     *  its default value is [AssociatedSaveMode.REPLACE]
     *
     * @param viewType The output DTO type of modified entity
     *
     * @param con An optional JDBC connection. If it is null,
     * a connection will be brown from [org.babyfish.jimmer.sql.runtime.ConnectionManager]
     *
     * @param block An optional lambda to add additional configuration.
     *
     * @param [E] The type of saved entity
     *
     * @return The saved result for multiple objects
     */
    fun <E : Any, V: View<E>> saveInputs(
        inputs: Iterable<Input<E>>,
        mode: SaveMode,
        associatedMode: AssociatedSaveMode = AssociatedSaveMode.REPLACE,
        viewType: KClass<V>,
        con: Connection? = null,
        block: (KSaveCommandPartialDsl.() -> Unit)? = null
    ): KBatchSaveResult.View<E, V> =
        this.saveEntities(inputs.map(Input<E>::toEntity), viewType, con) {
            setMode(mode)
            setAssociatedModeAll(associatedMode)
            block?.invoke(this)
        }

    /**
     * Save some input DTOs
     *
     * @param inputs The saved entity inputs.
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
     * The default value of the [SaveMode] of aggregate-roots is [SaveMode.UPSERT],
     *   it can be overwritten by the lambda represented by the parameter `block`
     *
     * @param associatedMode The associated save mode of associated objects,
     *  its default value is [AssociatedSaveMode.REPLACE]
     *
     * @param viewType The output DTO type of modified entity
     *
     * @param con An optional JDBC connection. If it is null,
     * a connection will be brown from [org.babyfish.jimmer.sql.runtime.ConnectionManager]
     *
     * @param block An optional lambda to add additional configuration.
     *
     * @param [E] The type of saved entity
     *
     * @return The saved result for multiple objects
     */
    fun <E : Any, V: View<E>> saveInputs(
        inputs: Iterable<Input<E>>,
        associatedMode: AssociatedSaveMode,
        viewType: KClass<V>,
        con: Connection? = null,
        block: (KSaveCommandPartialDsl.() -> Unit)? = null
    ): KBatchSaveResult.View<E, V> =
        this.saveEntities(inputs.map(Input<E>::toEntity), viewType, con) {
            setAssociatedModeAll(associatedMode)
            block?.invoke(this)
        }
}