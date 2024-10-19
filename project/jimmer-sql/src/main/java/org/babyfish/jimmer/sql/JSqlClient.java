package org.babyfish.jimmer.sql;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.babyfish.jimmer.Input;
import org.babyfish.jimmer.lang.NewChain;
import org.babyfish.jimmer.lang.OldChain;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.TypedProp;
import org.babyfish.jimmer.sql.association.meta.AssociationType;
import org.babyfish.jimmer.sql.ast.mutation.*;
import org.babyfish.jimmer.sql.ast.query.*;
import org.babyfish.jimmer.sql.ast.table.AssociationTable;
import org.babyfish.jimmer.sql.ast.table.spi.TableProxy;
import org.babyfish.jimmer.sql.cache.*;
import org.babyfish.jimmer.sql.di.*;
import org.babyfish.jimmer.sql.event.TriggerType;
import org.babyfish.jimmer.sql.event.Triggers;
import org.babyfish.jimmer.sql.event.binlog.BinLog;
import org.babyfish.jimmer.sql.event.binlog.BinLogPropReader;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.babyfish.jimmer.sql.filter.Filter;
import org.babyfish.jimmer.sql.filter.FilterConfig;
import org.babyfish.jimmer.sql.filter.Filters;
import org.babyfish.jimmer.sql.meta.DatabaseNamingStrategy;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.dialect.Dialect;
import org.babyfish.jimmer.sql.meta.IdGenerator;
import org.babyfish.jimmer.sql.meta.MetaStringResolver;
import org.babyfish.jimmer.sql.runtime.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.ZoneId;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

public interface JSqlClient extends SubQueryProvider {

    static Builder newBuilder() {
        return new JSqlClientImpl.BuilderImpl();
    }

    <T extends TableProxy<?>> MutableRootQuery<T> createQuery(T table);

    MutableUpdate createUpdate(TableProxy<?> table);

    MutableDelete createDelete(TableProxy<?> table);

    <SE, ST extends Table<SE>, TE, TT extends Table<TE>>
    MutableRootQuery<AssociationTable<SE, ST, TE, TT>> createAssociationQuery(
            AssociationTable<SE,ST, TE, TT> table
    );

    Entities getEntities();

    /**
     * This method is equivalent to `getTriggers(false)`
     * @return
     */
    Triggers getTriggers();

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
    Triggers getTriggers(boolean transaction);

    Associations getAssociations(TypedProp.Association<?, ?> prop);

    Associations getAssociations(ImmutableProp immutableProp);

    Associations getAssociations(AssociationType associationType);

    Caches getCaches();

    Filters getFilters();

    BinLog getBinLog();

    @NewChain
    JSqlClient caches(Consumer<CacheDisableConfig> block);

    @NewChain
    JSqlClient filters(Consumer<FilterConfig> block);

    @NewChain
    JSqlClient disableSlaveConnectionManager();

    @NewChain
    JSqlClient executor(Executor executor);

    /**
     * @param <T> Entity type or output DTO type
     */
    @Nullable
    default <T> T findById(Class<T> type, Object id) {
        return getEntities().findById(type, id);
    }

    @Nullable
    default <E> E findById(Fetcher<E> fetcher, Object id) {
        return getEntities().findById(fetcher, id);
    }

    /**
     * @param <T> Entity type or output DTO type
     */
    @NotNull
    default <T> T findOneById(Class<T> type, Object id) {
        return getEntities().findOneById(type, id);
    }

    @NotNull
    default <E> E findOneById(Fetcher<E> fetcher, Object id) {
        return getEntities().findOneById(fetcher, id);
    }

    /**
     * @param <T> Entity type or output DTO type
     */
    @NotNull
    default <T> List<T> findByIds(Class<T> type, Iterable<?> ids) {
        return getEntities().findByIds(type, ids);
    }

    @NotNull
    default <E> List<E> findByIds(Fetcher<E> fetcher, Iterable<?> ids) {
        return getEntities().findByIds(fetcher, ids);
    }

    /**
     * @param [V] Entity type or output DTO type
     */
    @NotNull
    default <K, V> Map<K, V> findMapByIds(Class<V> type, Iterable<K> ids) {
        return getEntities().findMapByIds(type, ids);
    }

    @NotNull
    default <K, V> Map<K, V> findMapByIds(Fetcher<V> fetcher, Iterable<K> ids) {
        return getEntities().findMapByIds(fetcher, ids);
    }

    /**
     * Save an entity object
     * @param <E> The type of saved entity
     * @param entity The saved entity object.
     *               <p>Note: The jimmer entity is <b>not POJO</b>,
     *               it can easily express data structures of arbitrary shape,
     *               you can use it to save data structures of arbitrary shape.</p>
     *
     *               <p>Unlike most JVM ORMs, Jimmer does not sepcified the shape
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
        return getEntities()
                .saveCommand(entity)
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
     *               <p>Unlike most JVM ORMs, Jimmer does not sepcified the shape
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
        return getEntities().saveCommand(entity)
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
     *               <p>Unlike most JVM ORMs, Jimmer does not sepcified the shape
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
        return getEntities()
                .saveCommand(entity)
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
     *               <p>Unlike most JVM ORMs, Jimmer does not sepcified the shape
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
        return getEntities().saveCommand(entity).execute();
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
     *               <p>Unlike most JVM ORMs, Jimmer does not sepcified the shape
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
        return getEntities()
                .saveCommand(input.toEntity())
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
     *               <p>Unlike most JVM ORMs, Jimmer does not sepcified the shape
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
        return getEntities()
                .saveCommand(input.toEntity())
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
     *               <p>Unlike most JVM ORMs, Jimmer does not sepcified the shape
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
        return getEntities()
                .saveCommand(input.toEntity())
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
     *               <p>Unlike most JVM ORMs, Jimmer does not sepcified the shape
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
        return getEntities().saveCommand(input.toEntity()).execute();
    }

    /**
     * Insert an entity object
     * @param entity The inserted entity object
     *
     *               <p>Note: The jimmer entity is <b>not POJO</b>,
     *               it can easily express data structures of arbitrary shape,
     *               you can use it to save data structures of arbitrary shape.</p>
     *
     *               <p>Unlike most JVM ORMs, Jimmer does not sepcified the shape
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
        return save(
                entity,
                SaveMode.INSERT_ONLY,
                AssociatedSaveMode.APPEND
        );
    }

    /**
     * Insert an entity object
     * @param entity The inserted entity object
     *
     *               <p>Note: The jimmer entity is <b>not POJO</b>,
     *               it can easily express data structures of arbitrary shape,
     *               you can use it to save data structures of arbitrary shape.</p>
     *
     *               <p>Unlike most JVM ORMs, Jimmer does not sepcified the shape
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
        return save(entity, SaveMode.INSERT_ONLY, associatedMode);
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
     *               <p>Unlike most JVM ORMs, Jimmer does not sepcified the shape
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
        return save(
                entity,
                SaveMode.INSERT_IF_ABSENT,
                AssociatedSaveMode.APPEND_IF_ABSENT
        );
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
     *               <p>Unlike most JVM ORMs, Jimmer does not sepcified the shape
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
        return save(entity, SaveMode.INSERT_IF_ABSENT, associatedMode);
    }

    /**
     * Update an entity object
     * @param entity The updated entity object
     *
     *               <p>Note: The jimmer entity is <b>not POJO</b>,
     *               it can easily express data structures of arbitrary shape,
     *               you can use it to save data structures of arbitrary shape.</p>
     *
     *               <p>Unlike most JVM ORMs, Jimmer does not sepcified the shape
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
        return save(
                entity,
                SaveMode.UPDATE_ONLY,
                AssociatedSaveMode.UPDATE
        );
    }

    /**
     * Update an entity object
     * @param entity The updated entity object
     *
     *               <p>Note: The jimmer entity is <b>not POJO</b>,
     *               it can easily express data structures of arbitrary shape,
     *               you can use it to save data structures of arbitrary shape.</p>
     *
     *               <p>Unlike most JVM ORMs, Jimmer does not sepcified the shape
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
        return save(entity, SaveMode.UPDATE_ONLY, associatedMode);
    }

    /**
     * Merge an entity object
     * @param entity The merged entity object
     *
     *               <p>Note: The jimmer entity is <b>not POJO</b>,
     *               it can easily express data structures of arbitrary shape,
     *               you can use it to save data structures of arbitrary shape.</p>
     *
     *               <p>Unlike most JVM ORMs, Jimmer does not sepcified the shape
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
        return save(
                entity,
                SaveMode.UPSERT,
                AssociatedSaveMode.MERGE
        );
    }

    /**
     * Merge an entity object
     * @param entity The merged entity object
     *
     *               <p>Note: The jimmer entity is <b>not POJO</b>,
     *               it can easily express data structures of arbitrary shape,
     *               you can use it to save data structures of arbitrary shape.</p>
     *
     *               <p>Unlike most JVM ORMs, Jimmer does not sepcified the shape
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
     * @param associatedMode The associated save mode of associated objects.
     * @return The save result for single object
     * @param <E> The type of inserted entity
     */
    default <E> SimpleSaveResult<E> merge(@NotNull E entity, @NotNull AssociatedSaveMode associatedMode) {
        return save(entity, SaveMode.UPSERT, associatedMode);
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
     *               <p>Unlike most JVM ORMs, Jimmer does not sepcified the shape
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
        return save(
                input.toEntity(),
                SaveMode.INSERT_ONLY,
                AssociatedSaveMode.APPEND
        );
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
     *               <p>Unlike most JVM ORMs, Jimmer does not sepcified the shape
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
        return save(input.toEntity(), SaveMode.INSERT_ONLY, associatedMode);
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
     *               <p>Unlike most JVM ORMs, Jimmer does not sepcified the shape
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
        return save(
                input.toEntity(),
                SaveMode.INSERT_IF_ABSENT,
                AssociatedSaveMode.APPEND_IF_ABSENT
        );
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
     *               <p>Unlike most JVM ORMs, Jimmer does not sepcified the shape
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
        return save(input.toEntity(), SaveMode.INSERT_IF_ABSENT, associatedMode);
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
     *               <p>Unlike most JVM ORMs, Jimmer does not sepcified the shape
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
        return save(
                input.toEntity(),
                SaveMode.UPDATE_ONLY,
                AssociatedSaveMode.UPDATE
        );
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
     *               <p>Unlike most JVM ORMs, Jimmer does not sepcified the shape
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
        return save(input.toEntity(), SaveMode.UPDATE_ONLY, associatedMode);
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
     *               <p>Unlike most JVM ORMs, Jimmer does not sepcified the shape
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
        return save(
                input.toEntity(),
                SaveMode.UPSERT,
                AssociatedSaveMode.MERGE
        );
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
     *               <p>Unlike most JVM ORMs, Jimmer does not sepcified the shape
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
     * @param associatedMode The associated save mode of associated objects.
     * @return The save result for single object
     * @param <E> The type of inserted entity
     */
    default <E> SimpleSaveResult<E> merge(@NotNull Input<E> input, @NotNull AssociatedSaveMode associatedMode) {
        return save(input.toEntity(), SaveMode.UPSERT, associatedMode);
    }

    /**
     * Save some entities objects
     * @param entities The saved entity objects.
     *
     *               <p>Note: The jimmer entity is <b>not POJO</b>,
     *               it can easily express data structures of arbitrary shape,
     *               you can use it to save data structures of arbitrary shape.</p>
     *
     *               <p>Unlike most JVM ORMs, Jimmer does not sepcified the shape
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
        return getEntities()
                .saveEntitiesCommand(entities)
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
     *               <p>Unlike most JVM ORMs, Jimmer does not sepcified the shape
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
        return getEntities()
                .saveEntitiesCommand(entities)
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
     *               <p>Unlike most JVM ORMs, Jimmer does not sepcified the shape
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
        return getEntities()
                .saveEntitiesCommand(entities)
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
     *               <p>Unlike most JVM ORMs, Jimmer does not sepcified the shape
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
        return getEntities()
                .saveEntitiesCommand(entities)
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
     *               <p>Unlike most JVM ORMs, Jimmer does not sepcified the shape
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
        return getEntities()
                .saveInputsCommand(inputs)
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
     *               <p>Unlike most JVM ORMs, Jimmer does not sepcified the shape
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
        return getEntities()
                .saveInputsCommand(inputs)
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
     *               <p>Unlike most JVM ORMs, Jimmer does not sepcified the shape
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
        return getEntities()
                .saveInputsCommand(inputs)
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
     *               <p>Unlike most JVM ORMs, Jimmer does not sepcified the shape
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
        return getEntities()
                .saveInputsCommand(inputs)
                .execute();
    }

    default DeleteResult deleteById(@NotNull Class<?> type, @NotNull Object id, @NotNull DeleteMode mode) {
        return getEntities().delete(type, id, mode);
    }

    default DeleteResult deleteById(Class<?> type, Object id) {
        return getEntities().delete(type, id, DeleteMode.AUTO);
    }

    default DeleteResult deleteByIds(Class<?> type, Iterable<?> ids, DeleteMode mode) {
        return getEntities().deleteAll(type, ids, mode);
    }

    default DeleteResult deleteByIds(Class<?> type, Iterable<?> ids) {
        return getEntities().deleteAll(type, ids, DeleteMode.AUTO);
    }

    interface Builder {

        int DEFAULT_BATCH_SIZE = 128;

        int DEFAULT_LIST_BATCH_SIZE = 16;

        @OldChain
        Builder setConnectionManager(ConnectionManager connectionManager);

        @OldChain
        Builder setSlaveConnectionManager(ConnectionManager connectionManager);

        @OldChain
        Builder setDialect(Dialect dialect);

        @OldChain
        Builder setExecutor(Executor executor);

        /**
         * <p>If this option is configured, when jimmer calls back
         * `org.babyfish.jimmer.sql.runtime.Executor.execute` before executing SQL,
         * it will check the stack trace information of the current thread.</p>
         *
         * <p>However, these stack traces have too much information, including
         * infrastructure call frames represented by jdk, jdbc driver, jimmer, and spring,
         * and the business-related information you care about will be submerged in the ocean of information.</p>
         *
         * <p>Through this configuration, you can specify multiple package or class prefixes, and jimmer will
         * judge whether there are some call frames in the stack trace whose class names start with some
         * of these prefixes. If the judgment is true, jimmer believes that the current callback is related
         * to your business, and the `ctx` parameter of `org.babyfish.jimmer.sql.runtime.Executor.execute`
         * will be passed as non-null.</p>
         *
         * <p>If the SQL logging configuration is enabled at the same time, when a SQL statement is caused by
         * the business you care about, the business call frame will be printed together with the SQL log.</p>
         */
        @OldChain
        Builder setExecutorContextPrefixes(Collection<String> prefixes);

        @OldChain
        Builder setSqlFormatter(SqlFormatter formatter);

        @OldChain
        Builder setZoneId(@Nullable ZoneId zoneId);

        @OldChain
        Builder setUserIdGeneratorProvider(UserIdGeneratorProvider userIdGeneratorProvider);

        @OldChain
        Builder setLogicalDeletedValueGeneratorProvider(LogicalDeletedValueGeneratorProvider logicalDeletedValueGeneratorProvider);

        @OldChain
        Builder setTransientResolverProvider(TransientResolverProvider transientResolverProvider);

        @OldChain
        Builder setIdGenerator(IdGenerator idGenerator);

        @OldChain
        Builder setIdGenerator(Class<?> entityType, IdGenerator idGenerator);

        @OldChain
        Builder addScalarProvider(ScalarProvider<?, ?> scalarProvider);

        @OldChain
        Builder setScalarProvider(TypedProp<?, ?> prop, ScalarProvider<?, ?> scalarProvider);

        @OldChain
        Builder setScalarProvider(ImmutableProp prop, ScalarProvider<?, ?> scalarProvider);

        @OldChain
        Builder setDefaultSerializedTypeObjectMapper(ObjectMapper mapper);

        @OldChain
        Builder setSerializedTypeObjectMapper(Class<?> type, ObjectMapper mapper);

        @OldChain
        Builder setSerializedPropObjectMapper(TypedProp<?, ?> prop, ObjectMapper mapper);

        @OldChain
        Builder setSerializedPropObjectMapper(ImmutableProp prop, ObjectMapper mapper);

        @OldChain
        Builder setDefaultJsonProviderCreator(Function<ImmutableProp, ScalarProvider<?, ?>> creator);

        @OldChain
        Builder setDefaultEnumStrategy(EnumType.Strategy strategy);

        @OldChain
        Builder setDatabaseNamingStrategy(DatabaseNamingStrategy strategy);

        @OldChain
        Builder setMetaStringResolver(MetaStringResolver resolver);

        @OldChain
        Builder setDefaultBatchSize(int size);

        @OldChain
        Builder setDefaultListBatchSize(int size);

        @OldChain
        Builder setInListPaddingEnabled(boolean enabled);

        @OldChain
        Builder setExpandedInListPaddingEnabled(boolean enabled);

        /**
         * For RDBMS, pagination is slow if `offset` is large, especially for MySQL.
         *
         * If `offset` >= $thisArgument
         *
         * <pre>{@code
         *  select t.* from Table t ... limit ? offset ?
         * }</pre>
         *
         * will be automatically changed to
         *
         * <pre>{@code
         *  select t.* from (
         *      select
         *          t.id as optimized_core_id_
         *      from Table t ... limit ? offset ?
         *  ) optimized_core_
         *  inner join Table as optimized_
         *      on optimized_.optimized_core_id_ = optimized_core_.optimized_core_id_
         * }</pre>
         *
         * @return An integer which is greater than 0
         */
        @OldChain
        Builder setOffsetOptimizingThreshold(int threshold);

        /**
         * Set deault lock mode of save command
         * @param lockMode
         */
        @OldChain
        Builder setDefaultLockMode(LockMode lockMode);

        @OldChain
        Builder setMaxCommandJoinCount(int maxMutationSubQueryDepth);

        /**
         * Under normal circumstances, users do not need to set the entity manager.
         *
         * <p>This configuration is for compatibility with version 0.7.47 and earlier.</p>
         */
        @OldChain
        Builder setEntityManager(EntityManager entityManager);

        @OldChain
        Builder setCaches(Consumer<CacheConfig> block);

        @OldChain
        Builder setCacheFactory(CacheFactory cacheFactory);

        @OldChain
        Builder setCacheOperator(CacheOperator cacheOperator);

        @OldChain
        Builder addCacheAbandonedCallback(CacheAbandonedCallback callback);

        @OldChain
        Builder addCacheAbandonedCallbacks(Collection<? extends CacheAbandonedCallback> callbacks);

        @OldChain
        Builder setTriggerType(TriggerType triggerType);

        @OldChain
        Builder setLogicalDeletedBehavior(LogicalDeletedBehavior behavior);

        @OldChain
        Builder addFilters(Filter<?>... filters);

        @OldChain
        Builder addFilters(Collection<? extends Filter<?>> filters);

        @OldChain
        Builder addDisabledFilters(Filter<?>... filters);

        @OldChain
        Builder addDisabledFilters(Collection<? extends Filter<?>> filters);

        @OldChain
        Builder setDefaultDissociateActionCheckable(boolean checkable);

        @OldChain
        Builder setIdOnlyTargetCheckingLevel(IdOnlyTargetCheckingLevel checkingLevel);

        @OldChain
        Builder addDraftPreProcessor(DraftPreProcessor<?> processor);

        @OldChain
        Builder addDraftPreProcessors(DraftPreProcessor<?>... processors);

        @OldChain
        Builder addDraftPreProcessors(Collection<DraftPreProcessor<?>> processors);

        @OldChain
        Builder addDraftInterceptor(DraftInterceptor<?, ?> interceptor);

        @OldChain
        Builder addDraftInterceptors(DraftInterceptor<?, ?>... interceptors);

        @OldChain
        Builder addDraftInterceptors(Collection<? extends DraftInterceptor<?, ?>> interceptors);

        Builder setDefaultBinLogObjectMapper(ObjectMapper mapper);

        @OldChain
        Builder setBinLogPropReader(ImmutableProp prop, BinLogPropReader reader);

        @OldChain
        Builder setBinLogPropReader(TypedProp.Scalar<?, ?> prop, BinLogPropReader reader);

        @OldChain
        Builder setBinLogPropReader(Class<?> propType, BinLogPropReader reader);

        @OldChain
        Builder setForeignKeyEnabledByDefault(boolean enabled);

        @OldChain
        Builder setTargetTransferable(boolean targetTransferable);

        @OldChain
        Builder addExceptionTranslator(ExceptionTranslator<?> translator);

        @OldChain
        Builder addExceptionTranslators(Collection<ExceptionTranslator<?>> translators);

        @OldChain
        Builder addCustomizers(Customizer ... customizers);

        @OldChain
        Builder addCustomizers(Collection<? extends Customizer> customizers);

        @OldChain
        Builder addInitializers(Initializer ... initializers);

        @OldChain
        Builder addInitializers(Collection<? extends Initializer> initializers);

        @OldChain
        Builder setDatabaseValidationMode(DatabaseValidationMode mode);

        @OldChain
        Builder setDatabaseValidationCatalog(String catalog);

        @OldChain
        Builder setDatabaseValidationSchema(String schema);

        @OldChain
        Builder setAopProxyProvider(AopProxyProvider provider);

        @OldChain
        Builder setMicroServiceName(String microServiceName);

        @OldChain
        Builder setMicroServiceExchange(MicroServiceExchange exchange);

        @OldChain
        Builder setInitializationType(InitializationType type);

        JSqlClient build();
    }
}
