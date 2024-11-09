package org.babyfish.jimmer.sql.kt

import org.babyfish.jimmer.Input
import org.babyfish.jimmer.View
import org.babyfish.jimmer.lang.NewChain
import org.babyfish.jimmer.sql.JSqlClient
import org.babyfish.jimmer.sql.ast.mutation.AssociatedSaveMode
import org.babyfish.jimmer.sql.ast.mutation.DeleteMode
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.babyfish.jimmer.sql.event.binlog.BinLog
import org.babyfish.jimmer.sql.exception.EmptyResultException
import org.babyfish.jimmer.sql.exception.TooManyResultsException
import org.babyfish.jimmer.sql.fetcher.DtoMetadata
import org.babyfish.jimmer.sql.fetcher.Fetcher
import org.babyfish.jimmer.sql.kt.ast.KExecutable
import org.babyfish.jimmer.sql.kt.ast.mutation.*
import org.babyfish.jimmer.sql.kt.ast.query.KConfigurableRootQuery
import org.babyfish.jimmer.sql.kt.ast.query.KMutableRootQuery
import org.babyfish.jimmer.sql.kt.cfg.KSqlClientDsl
import org.babyfish.jimmer.sql.kt.filter.KFilterDsl
import org.babyfish.jimmer.sql.kt.filter.KFilters
import org.babyfish.jimmer.sql.kt.impl.KSqlClientImpl
import org.babyfish.jimmer.sql.runtime.EntityManager
import org.babyfish.jimmer.sql.runtime.Executor
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor
import org.babyfish.jimmer.sql.runtime.ScalarProvider
import java.sql.Connection
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

interface KSqlClient {

    fun <E : Any, R> createQuery(
        entityType: KClass<E>,
        block: KMutableRootQuery<E>.() -> KConfigurableRootQuery<E, R>
    ): KConfigurableRootQuery<E, R> =
        queries.forEntity(entityType, block)

    fun <E : Any> createUpdate(
        entityType: KClass<E>,
        block: KMutableUpdate<E>.() -> Unit
    ): KExecutable<Int>

    fun <E : Any> createDelete(
        entityType: KClass<E>,
        block: KMutableDelete<E>.() -> Unit
    ): KExecutable<Int>

    fun <E : Any, R> executeQuery(
        entityType: KClass<E>,
        limit: Int? = null,
        con: Connection? = null,
        block: KMutableRootQuery<E>.() -> KConfigurableRootQuery<E, R>
    ): List<R> = queries
        .forEntity(entityType, block)
        .let { q ->
            limit?.let { q.limit(it) } ?: q
        }
        .execute(con)

    fun <E : Any> executeUpdate(
        entityType: KClass<E>,
        con: Connection? = null,
        block: KMutableUpdate<E>.() -> Unit
    ): Int = createUpdate(entityType, block).execute(con)

    fun <E : Any> executeDelete(
        entityType: KClass<E>,
        con: Connection? = null,
        block: KMutableDelete<E>.() -> Unit
    ): Int = createDelete(entityType, block).execute(con)

    val queries: KQueries

    val entities: KEntities

    val caches: KCaches

    /**
     * This property is equivalent to `getTriggers(false)`
     */
    val triggers: KTriggers

    /**
     * <ul>
     *     <li>
     *         If trigger type is 'BINLOG_ONLY'
     *         <ul>
     *             <li>If `transaction` is true, throws exception</li>
     *             <li>If `transaction` is false, return binlog trigger</li>
     *         </ul>
     *     </li>
     *     <li>
     *         If trigger type is 'TRANSACTION_ONLY', returns transaction trigger
     *         no matter what the `transaction` is
     *     </li>
     *     <li>
     *         If trigger type is 'BOTH'
     *         <ul>
     *             <li>If `transaction` is true, return transaction trigger</li>
     *             <li>If `transaction` is false, return binlog trigger</li>
     *         </ul>
     *         Note that the objects returned by different parameters are independent of each other.
     *     </li>
     * </ul>
     * @param transaction
     * @return Trigger
     */
    fun getTriggers(transaction: Boolean): KTriggers

    val filters: KFilters

    fun getAssociations(prop: KProperty1<*, *>): KAssociations

    @NewChain
    fun caches(block: KCacheDisableDsl.() -> Unit): KSqlClient

    @NewChain
    fun filters(block: KFilterDsl.() -> Unit): KSqlClient

    @NewChain
    fun disableSlaveConnectionManager(): KSqlClient

    @NewChain
    fun executor(executor: Executor?): KSqlClient

    val entityManager: EntityManager

    val binLog: BinLog

    /**
     * @param [T] Entity type or output DTO type
     */
    fun <T : Any> findById(type: KClass<T>, id: Any): T? =
        entities.findById(type, id)

    fun <E : Any> findById(fetcher: Fetcher<E>, id: Any): E? =
        entities.findById(fetcher, id)

    /**
     * @param [T] Entity type or output DTO type
     */
    fun <T : Any> findByIds(type: KClass<T>, ids: Iterable<*>): List<T> =
        entities.findByIds(type, ids)

    fun <E : Any> findByIds(fetcher: Fetcher<E>, ids: Iterable<*>): List<E> =
        entities.findByIds(fetcher, ids)

    /**
     * @param [T] Entity type or output DTO type
     */
    fun <K, T : Any> findMapByIds(type: KClass<T>, ids: Iterable<K>): Map<K, T> =
        entities.findMapByIds(type, ids)

    fun <K, V : Any> findMapByIds(fetcher: Fetcher<V>, ids: Iterable<K>): Map<K, V> =
        entities.findMapByIds(fetcher, ids)

    /**
     * @param [T] Entity type or output DTO type
     */
    fun <T : Any> findOneById(type: KClass<T>, id: Any): T =
        entities.findOneById(type, id)

    fun <E : Any> findOneById(fetcher: Fetcher<E>, id: Any): E =
        entities.findOneById(fetcher, id)

    fun <E : Any> findAll(
        fetcher: Fetcher<E>,
        limit: Int? = null,
        con: Connection? = null,
        block: KMutableRootQuery<E>.() -> Unit = {}
    ): List<E> = executeQuery(fetcher.javaClass.kotlin, limit, con) {
        block()
        select(table.fetch(fetcher))
    }

    fun <E : Any> findOne(
        fetcher: Fetcher<E>,
        con: Connection? = null,
        block: KMutableRootQuery<E>.() -> Unit
    ): E = findAll(fetcher, 2, null, block).let {
        when (it.size) {
            0 -> throw EmptyResultException()
            1 -> it[0]
            else -> throw TooManyResultsException()
        }
    }


    fun <E : Any> findOneOrNull(
        fetcher: Fetcher<E>,
        con: Connection? = null,
        block: KMutableRootQuery<E>.() -> Unit
    ): E? = findAll(fetcher, 2, con, block).let {
        when (it.size) {
            0 -> null
            1 -> return it[0]
            else -> throw TooManyResultsException()
        }
    }

    fun <E : Any, V : View<E>> findAll(
        viewType: KClass<V>,
        limit: Int? = null,
        con: Connection? = null,
        block: KMutableRootQuery<E>.() -> Unit = {}
    ): List<V> {
        val metadata = DtoMetadata.of(viewType.java)
        return findAll(metadata.fetcher, limit, con, block).map(metadata.converter::apply)
    }

    fun <E : Any, V : View<E>> findOne(
        viewType: KClass<V>,
        con: Connection? = null,
        block: KMutableRootQuery<E>.() -> Unit
    ): V {
        val metadata = DtoMetadata.of(viewType.java)
        return findOne(metadata.fetcher, con, block).let(metadata.converter::apply)
    }

    fun <E : Any, V : View<E>> findOneOrNull(
        viewType: KClass<V>,
        con: Connection? = null,
        block: KMutableRootQuery<E>.() -> Unit
    ): V? {
        val metadata = DtoMetadata.of(viewType.java)
        return findOneOrNull(metadata.fetcher, con, block)?.let(metadata.converter::apply)
    }

    /**
     * Save an entity object
     * @param entity The saved entity object.
     *
     * **Note: The jimmer entity is <b>not POJO**,
     * it can easily express data structures of arbitrary shape,
     * you can use it to save data structures of arbitrary shape.
     *
     * Unlike most JVM ORMs, Jimmer does not sepcified the shape
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
     * @param block An optional lambda to add additional configuration.
     *
     * @param [E] The type of saved entity
     *
     * @return The saved result for single object
     */
    fun <E : Any> save(
        entity: E,
        block: (KSaveCommandDsl.() -> Unit)? = null
    ): KSimpleSaveResult<E> =
        entities.save(entity) {
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
     * Unlike most JVM ORMs, Jimmer does not sepcified the shape
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
        block: (KSaveCommandPartialDsl.() -> Unit)? = null
    ): KSimpleSaveResult<E> =
        entities.save(entity) {
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
     * Unlike most JVM ORMs, Jimmer does not sepcified the shape
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
        block: (KSaveCommandPartialDsl.() -> Unit)? = null
    ): KSimpleSaveResult<E> =
        entities.save(entity) {
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
     * Unlike most JVM ORMs, Jimmer does not sepcified the shape
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
     * @param block An optional lambda to add additional configuration.
     *
     * @param [E] The type of saved entity
     *
     * @return The saved result for single object
     */
    fun <E : Any> save(
        input: Input<E>,
        block: (KSaveCommandDsl.() -> Unit)? = null
    ): KSimpleSaveResult<E> =
        entities.save(input) {
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
     * Unlike most JVM ORMs, Jimmer does not sepcified the shape
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
        block: (KSaveCommandPartialDsl.() -> Unit)? = null
    ): KSimpleSaveResult<E> =
        entities.save(input) {
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
     * Unlike most JVM ORMs, Jimmer does not sepcified the shape
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
        associatedMode: AssociatedSaveMode = AssociatedSaveMode.REPLACE,
        block: (KSaveCommandPartialDsl.() -> Unit)? = null
    ): KSimpleSaveResult<E> =
        entities.save(input) {
            setAssociatedModeAll(associatedMode)
            block?.invoke(this)
        }

    /**
     * Insert an entity object
     * @param entity The inserted entity object.
     *
     * **Note: The jimmer entity is <b>not POJO**,
     * it can easily express data structures of arbitrary shape,
     * you can use it to save data structures of arbitrary shape.
     *
     * Unlike most JVM ORMs, Jimmer does not sepcified the shape
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
     * @param block An optional lambda to add additional configuration.
     * In addition to setting the [SaveMode] of aggregate root
     * and the default value of the [AssociatedSaveMode] of associated objects,
     * you can add any other configuration.
     *
     * @param [E] The type of saved entity
     *
     * @return The saved result for single object
     */
    fun <E : Any> insert(
        entity: E,
        associatedMode: AssociatedSaveMode = AssociatedSaveMode.APPEND,
        block: (KSaveCommandPartialDsl.() -> Unit)? = null
    ): KSimpleSaveResult<E> =
        entities.save(entity) {
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
     * Unlike most JVM ORMs, Jimmer does not sepcified the shape
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
     * @param block An optional lambda to add additional configuration.
     * In addition to setting the [SaveMode] of aggregate root
     * and the default value of the [AssociatedSaveMode] of associated objects,
     * you can add any other configuration.
     *
     * @param [E] The type of saved entity
     *
     * @return The saved result for single object
     */
    fun <E : Any> insertIfAbsent(
        entity: E,
        associatedMode: AssociatedSaveMode = AssociatedSaveMode.APPEND_IF_ABSENT,
        block: (KSaveCommandPartialDsl.() -> Unit)? = null
    ): KSimpleSaveResult<E> =
        entities.save(entity) {
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
     * Unlike most JVM ORMs, Jimmer does not sepcified the shape
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
     * @param block An optional lambda to add additional configuration.
     * In addition to setting the [SaveMode] of aggregate root
     * and the default value of the [AssociatedSaveMode] of associated objects,
     * you can add any other configuration.
     *
     * @param [E] The type of saved entity
     *
     * @return The saved result for single object
     */
    fun <E : Any> update(
        entity: E,
        associatedMode: AssociatedSaveMode = AssociatedSaveMode.UPDATE,
        block: (KSaveCommandPartialDsl.() -> Unit)? = null
    ): KSimpleSaveResult<E> =
        entities.save(entity) {
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
     * Unlike most JVM ORMs, Jimmer does not sepcified the shape
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
     * @param block An optional lambda to add additional configuration.
     * In addition to setting the [SaveMode] of aggregate root
     * and the default value of the [AssociatedSaveMode] of associated objects,
     * you can add any other configuration.
     *
     * @param [E] The type of saved entity
     *
     * @return The saved result for single object
     */
    fun <E : Any> merge(
        entity: E,
        associatedMode: AssociatedSaveMode = AssociatedSaveMode.MERGE,
        block: (KSaveCommandPartialDsl.() -> Unit)? = null
    ): KSimpleSaveResult<E> =
        entities.save(entity) {
            setMode(SaveMode.UPSERT)
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
     * Unlike most JVM ORMs, Jimmer does not sepcified the shape
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
     * @param block An optional lambda to add additional configuration.
     * In addition to setting the [SaveMode] of aggregate root
     * and the default value of the [AssociatedSaveMode] of associated objects,
     * you can add any other configuration.
     *
     * @param [E] The type of saved entity
     *
     * @return The saved result for single object
     */
    fun <E : Any> insert(
        input: Input<E>,
        associatedMode: AssociatedSaveMode = AssociatedSaveMode.APPEND,
        block: (KSaveCommandPartialDsl.() -> Unit)? = null
    ): KSimpleSaveResult<E> =
        entities.save(input.toEntity()) {
            setMode(SaveMode.INSERT_ONLY)
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
     * Unlike most JVM ORMs, Jimmer does not sepcified the shape
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
     * @param block An optional lambda to add additional configuration.
     * In addition to setting the [SaveMode] of aggregate root
     * and the default value of the [AssociatedSaveMode] of associated objects,
     * you can add any other configuration.
     *
     * @param [E] The type of saved entity
     *
     * @return The saved result for single object
     */
    fun <E : Any> insertIfAbsent(
        input: Input<E>,
        associatedMode: AssociatedSaveMode = AssociatedSaveMode.APPEND_IF_ABSENT,
        block: (KSaveCommandPartialDsl.() -> Unit)? = null
    ): KSimpleSaveResult<E> =
        entities.save(input.toEntity()) {
            setMode(SaveMode.INSERT_IF_ABSENT)
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
     * Unlike most JVM ORMs, Jimmer does not sepcified the shape
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
     * @param block An optional lambda to add additional configuration.
     * In addition to setting the [SaveMode] of aggregate root
     * and the default value of the [AssociatedSaveMode] of associated objects,
     * you can add any other configuration.
     *
     * @param [E] The type of saved entity
     *
     * @return The saved result for single object
     */
    fun <E : Any> update(
        input: Input<E>,
        associatedMode: AssociatedSaveMode = AssociatedSaveMode.UPDATE,
        block: (KSaveCommandPartialDsl.() -> Unit)? = null
    ): KSimpleSaveResult<E> =
        entities.save(input.toEntity()) {
            setMode(SaveMode.UPDATE_ONLY)
            setAssociatedModeAll(associatedMode)
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
     * Unlike most JVM ORMs, Jimmer does not sepcified the shape
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
     * @param block An optional lambda to add additional configuration.
     * In addition to setting the [SaveMode] of aggregate root
     * and the default value of the [AssociatedSaveMode] of associated objects,
     * you can add any other configuration.
     *
     * @param [E] The type of saved entity
     *
     * @return The saved result for single object
     */
    fun <E : Any> merge(
        input: Input<E>,
        associatedMode: AssociatedSaveMode = AssociatedSaveMode.MERGE,
        block: (KSaveCommandPartialDsl.() -> Unit)? = null
    ): KSimpleSaveResult<E> =
        entities.save(input.toEntity()) {
            setMode(SaveMode.UPSERT)
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
     * Unlike most JVM ORMs, Jimmer does not sepcified the shape
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
        this.entities.saveEntities(entities, con, block)

    fun <E: Any> insertEntities(
        entities: Iterable<E>,
        associatedMode: AssociatedSaveMode = AssociatedSaveMode.APPEND,
        con: Connection? = null,
        block: (KSaveCommandPartialDsl.() -> Unit)? = null
    ): KBatchSaveResult<E> =
        this.entities.saveEntities(entities, con) {
            setMode(SaveMode.INSERT_ONLY)
            setAssociatedModeAll(associatedMode)
            block?.invoke(this)
        }

    fun <E: Any> insertEntitiesIfAbsent(
        entities: Iterable<E>,
        associatedMode: AssociatedSaveMode = AssociatedSaveMode.APPEND_IF_ABSENT,
        con: Connection? = null,
        block: (KSaveCommandPartialDsl.() -> Unit)? = null
    ): KBatchSaveResult<E> =
        this.entities.saveEntities(entities, con) {
            setMode(SaveMode.INSERT_IF_ABSENT)
            setAssociatedModeAll(associatedMode)
            block?.invoke(this)
        }

    fun <E: Any> updateEntities(
        entities: Iterable<E>,
        associatedMode: AssociatedSaveMode = AssociatedSaveMode.UPDATE,
        con: Connection? = null,
        block: (KSaveCommandPartialDsl.() -> Unit)? = null
    ): KBatchSaveResult<E> =
        this.entities.saveEntities(entities, con) {
            setMode(SaveMode.UPDATE_ONLY)
            setAssociatedModeAll(associatedMode)
            block?.invoke(this)
        }

    fun <E: Any> mergeEntities(
        entities: Iterable<E>,
        associatedMode: AssociatedSaveMode = AssociatedSaveMode.MERGE,
        con: Connection? = null,
        block: (KSaveCommandPartialDsl.() -> Unit)? = null
    ): KBatchSaveResult<E> =
        this.entities.saveEntities(entities, con) {
            setMode(SaveMode.UPSERT)
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
     * Unlike most JVM ORMs, Jimmer does not sepcified the shape
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
        this.entities.saveInputs(inputs, con, block)

    fun <E: Any> insertInputs(
        inputs: Iterable<Input<E>>,
        associatedMode: AssociatedSaveMode = AssociatedSaveMode.APPEND,
        con: Connection? = null,
        block: (KSaveCommandPartialDsl.() -> Unit)? = null
    ): KBatchSaveResult<E> =
        this.entities.saveInputs(inputs, con) {
            setMode(SaveMode.INSERT_ONLY)
            setAssociatedModeAll(associatedMode)
            block?.invoke(this)
        }

    fun <E: Any> insertInputsIfAbsent(
        inputs: Iterable<Input<E>>,
        associatedMode: AssociatedSaveMode = AssociatedSaveMode.APPEND_IF_ABSENT,
        con: Connection? = null,
        block: (KSaveCommandPartialDsl.() -> Unit)? = null
    ): KBatchSaveResult<E> =
        this.entities.saveInputs(inputs, con) {
            setMode(SaveMode.INSERT_IF_ABSENT)
            setAssociatedModeAll(associatedMode)
            block?.invoke(this)
        }

    fun <E: Any> updateInputs(
        inputs: Iterable<Input<E>>,
        associatedMode: AssociatedSaveMode = AssociatedSaveMode.UPDATE,
        con: Connection? = null,
        block: (KSaveCommandPartialDsl.() -> Unit)? = null
    ): KBatchSaveResult<E> =
        this.entities.saveInputs(inputs, con) {
            setMode(SaveMode.UPDATE_ONLY)
            setAssociatedModeAll(associatedMode)
            block?.invoke(this)
        }

    fun <E: Any> mergeInputs(
        inputs: Iterable<Input<E>>,
        associatedMode: AssociatedSaveMode = AssociatedSaveMode.MERGE,
        con: Connection? = null,
        block: (KSaveCommandPartialDsl.() -> Unit)? = null
    ): KBatchSaveResult<E> =
        this.entities.saveInputs(inputs, con) {
            setMode(SaveMode.UPSERT)
            setAssociatedModeAll(associatedMode)
            block?.invoke(this)
        }

    fun <E : Any> deleteById(type: KClass<E>, id: Any, mode: DeleteMode = DeleteMode.AUTO): KDeleteResult =
        entities.delete(type, id) {
            setMode(mode)
        }

    fun <E : Any> deleteById(type: KClass<E>, id: Any, block: KDeleteCommandDsl.() -> Unit): KDeleteResult =
        entities.delete(type, id, block = block)

    fun <E : Any> deleteByIds(type: KClass<E>, ids: Iterable<*>, mode: DeleteMode = DeleteMode.AUTO): KDeleteResult =
        entities.deleteAll(type, ids) {
            setMode(mode)
        }

    fun <E : Any> deleteByIds(type: KClass<E>, ids: Iterable<*>, block: KDeleteCommandDsl.() -> Unit): KDeleteResult =
        entities.deleteAll(type, ids, block = block)

    val javaClient: JSqlClientImplementor
}

internal class UnsignedLongScalar : ScalarProvider<ULong, String> {
    override fun toScalar(sqlValue: String): ULong {
        return sqlValue.toULong()
    }

    override fun toSql(scalarValue: ULong): String {
        return scalarValue.toString()
    }
}

internal class UnsignedIntScalar : ScalarProvider<UInt, String> {
    override fun toScalar(sqlValue: String): UInt {
        return sqlValue.toUInt()
    }

    override fun toSql(scalarValue: UInt): String {
        return scalarValue.toString()
    }
}

internal class UnsignedShortScalar : ScalarProvider<UShort, String> {
    override fun toScalar(sqlValue: String): UShort {
        return sqlValue.toUShort()
    }

    override fun toSql(scalarValue: UShort): String {
        return scalarValue.toString()
    }
}

internal class UnsignedByteScalar : ScalarProvider<UByte, String> {
    override fun toScalar(sqlValue: String): UByte {
        return sqlValue.toUByte()
    }

    override fun toSql(scalarValue: UByte): String {
        return scalarValue.toString()
    }
}

fun newKSqlClient(block: KSqlClientDsl.() -> Unit): KSqlClient {
    val javaBuilder = JSqlClient.newBuilder()
    javaBuilder.addScalarProvider(UnsignedLongScalar())
    javaBuilder.addScalarProvider(UnsignedIntScalar())
    javaBuilder.addScalarProvider(UnsignedShortScalar())
    javaBuilder.addScalarProvider(UnsignedByteScalar())
    val dsl = KSqlClientDsl(javaBuilder)
    dsl.block()
    return dsl.buildKSqlClient()
}

fun JSqlClient.toKSqlClient(): KSqlClient =
    KSqlClientImpl(this as JSqlClientImplementor)