package org.babyfish.jimmer.sql.ast.impl.mutation;

import org.babyfish.jimmer.sql.KeyUniqueConstraint;

/**
 * Jimmer's save command supports upsert operations and tries to leverage the
 * database's native upsert capabilities whenever possible,
 * rather than executing an additional select statement to determine whether
 * the subsequent operation should be an insert or update.
 *
 * <p>However, the database's native upsert capability is not suitable for
 * all situations. When necessary, Jimmer may still execute a select statement
 * within the save command. When this occurs, this enumeration can inform
 * users why this is happening.</p>
 *
 * <p>In addition to the save command, delete commands and delete statements
 * may also need to execute some select statements in certain scenarios.
 * This enumeration includes these cases as well.</p>
 */
public enum QueryReason {

    NONE,

    /**
     * In-transaction trigger is enabled, meaning Jimmer's trigger type is set to
     * `TRANSACTION_ONLY` or `BOTH`.
     *
     * <p>This mechanism is highly similar to
     * <a href ="https://github.com/apache/incubator-seata">AT mode of Apache Seata</a>,
     * where an additional select statement is executed before modifying the data to get
     * the old values, simulating database trigger-like functionality before the transaction
     * is committed.</p>
     *
     * <p>No handling is required for this situation, as this is precisely what you need.</p>
     */
    TRIGGER,

    /**
     * The entity type being operated on has been applied with a DraftInterceptor to
     * facilitate data adjustment before being saved to the database.
     *
     * <p>The DraftInterceptor's functionality is relatively powerful, capable of
     * informing developers whether an object is about to be saved or modified.
     * At this point, the database has not been modified, so a select statement
     * must be used to determine whether the subsequent operation is an insert
     * or update and to notify the user.</p>
     *
     * <p>If you don't want this behavior, you can use a DraftHandler instead.
     * Its functionality is similar to DraftInterceptor but relatively weaker,
     * not distinguishing between insert and update scenarios, thus avoiding
     * this issue.</p>
     */
    INTERCEPTOR,

    /**
     * Associated objects with only {@link org.babyfish.jimmer.sql.Id} properties
     * are not cascade-saved and merely serve as dependencies for the current entity.
     *
     * <p>However, users can choose to validate whether these id-only associated
     * objects represent valid data before saving the current object. This is
     * particularly useful for {@link org.babyfish.jimmer.sql.ForeignKeyType#FAKE}
     * foreign keys.</p>
     */
    CHECKING,

    /**
     * Jimmer will delete objects in any of the following situations:
     *
     * <ul>
     *     <li>Direct use of the delete command</li>
     *     <li>Direct use of a delete statement, but upon discovering that the
     *     object being deleted has child objects and the user requires Jimmer
     *     to handle these child objects, the delete statement is converted into
     *     a delete command</li>
     *     <li>Using a save command, but Jimmer determines that the current object
     *     has discarded some associated objects, requiring dissociation
     *     operations for these no longer needed associated objects, and the
     *     corresponding dissociation operation is configured for deletion</li>
     * </ul>
     *
     * <p>To delete an object, one must consider that it may have child objects,
     * which need to be deleted first. However, child objects can have even deeper
     * child objects, creating an infinite recursion problem.</p>
     *
     * <p>Although Jimmer handles the deleted object tree according to a
     * fixed depth<i>(set through the global configuration
     * `jimmer.max-mutation-join-depth`, default is 2)</i>,this fixed depth is
     * not infinite, when the object tree might have a greater depth,
     * select statements must be used to recursively delete the object tree.</p>
     */
    TOO_DEEP,

    /**
     * Sometimes, Jimmer requires multi-column in expression, i.e.,
     * <pre>{@code
     * where (C1, C2, ...Cn) in (
     *     (V11, V12, ... V1n),
     *     (V21, V22, ... V2n),
     *     ...
     *     (Vm1, Vm2, ... Vmn)
     * }</pre>
     *
     * However, the current database does not support multi-column in,
     * for example: org.babyfish.jimmer.sql.dialect.SqlServerDialect
     */
    TUPLE_IS_UNSUPPORTED,

    /**
     * If the associated property is
     * {@link org.babyfish.jimmer.sql.OneToMany} association
     * or an inverse {@link org.babyfish.jimmer.sql.OneToOne}
     * <i>(Inverse means {@code mappedBy} is specified,
     * eg: {@code @OneToOne(mappedBy="...")})</i> association, then
     * the associated objects are child objects of the current object.
     *
     * <p>This cascading save behavior poses a business-level risk:
     * assuming some child objects may already belong to other parent
     * objects, the save command would snatch them away, making them
     * become child objects of the current entity.</p>
     *
     * <p>By default, Jimmer adopts a very conservative approach,
     * executing a select query to ensure this child object contention
     * does not occur.</p>
     *
     * <p>If you believe this is not an issue and do not want to execute
     * additional select queries for this purpose, you can disable this
     * behavior. There are two ways to do this:</p>
     *
     * <ol>
     *     <li>Configuration at the current save command level:
     *     <pre>{@code
     *     sqlClient.saveCommand(entity)
     *          .setTargetTransferMode(
     *              TreeNodeProps.CHILD_NODES,
     *              TargetTransferMode.ALLOWED
     *          )
     *     }</pre>
     *     or
     *     <pre>{@code
     *     sqlClient.saveCommand(entity)
     *          .setTargetTransferModeAll(
     *              TargetTransferMode.ALLOWED
     *          )
     *     }</pre>
     *     </li>
     *     <li>Global configuration:
     *     <ul>
     *         <li>Not using jimmer spring starter:
     *         <pre>{@code
     *         JSqlClient sqlClient = JSqlClient
     *              .newBuilder()
     *              .setTargetTransferable(true)
     *              .build()
     *         }</pre>
     *         </li>
     *         <li>Using jimmer spring starter: Set the global configuration
     *         {@code `jimmer.target-transferable`} to true
     *         </li>
     *     </ul>
     *     </li>
     * </ol>
     *
     * <p>Note: If both the current save command level configuration and the global configuration exist simultaneously, the former takes precedence.</p>
     */
    TARGET_NOT_TRANSFERABLE,

    /**
     * The current database does not support upsert operations,
     * or its upsert operation has not been integrated with Jimmer
     */
    UPSERT_NOT_SUPPORTED,

    /**
     * The current database does not support mixing optimistic locking
     * checks in upsert operations (so far, among the dialects implemented
     * in Jimmer, only Postgres supports this)
     */
    OPTIMISTIC_LOCK,

    /**
     * The current entity type being operated on is annotated with
     * {@link org.babyfish.jimmer.sql.KeyUniqueConstraint}.
     *
     * <p>Note that you shouldn't simply add this annotation just to
     * eliminate this query behavior. This annotation represents the
     * developer's promise to Jimmer that corresponding unique constraints
     * or unique indexes exist in the data.</p>
     *
     * <p>Taking unique constraints as an example</p>
     * <pre>{@code
     * alter table your_table
     *     add constraint uq_your_constraint
     *         unique(
     *             K1, K2, ... Kn,
     *             LD
     *         );
     * }</pre>
     * Where {@code K1, K2, ... KN} represent the column set corresponding
     * to the entity's {@link org.babyfish.jimmer.sql.Key} columns;
     * {@code LD} represents the field corresponding to the
     * {@link org.babyfish.jimmer.sql.LogicalDeleted} column.
     *
     * <p>If the entity supports logical deletion, it's recommended to
     * use a method that can well distinguish different versions of data
     * without introducing nullable long type values. For example</p>
     * <pre>{@code
     * @LogicalDeleted
     * long deletedMillis();
     * }</pre>
     *
     * <p>Note that middle tables specified by
     * {@link org.babyfish.jimmer.sql.JoinTable} don't have this
     * configuration, but they definitely need to establish primary keys
     * based on all columns <i>(two required foreign keys, one optional
     * logical deletion column, one optional type discrimination column)</i>.</p>
     */
    KEY_UNIQUE_CONSTRAINT_REQUIRED,

    /**
     * The current database is MySQL, if there save object does
     * not have {@link org.babyfish.jimmer.sql.Id} property,
     * Jimmer uses an {@code insert ... on duplicate update...}
     * statement based on key properties, for example
     *
     * <pre>{@code
     * insert into your_table(NAME, EDITION, A, B, C)
     * values('X', 1, 'a', 'b', 'c')
     * on duplicate update
     * ID = last_inserted_id(ID),
     * A = VALUES(A), B = VALUES(B), C = VALUES(C)
     * }</pre>
     *
     * <p>When the inserted columns do not include the primary key,
     * MySQL will make judgments based on all non-primary key
     * unique constraint fields, which may be far more than the
     * definition of {@link java.security.Key} properties. and
     * {@link org.babyfish.jimmer.sql.LogicalDeleted} property</p>
     *
     * <p>If you want to eliminate queries, please configure
     * {@link KeyUniqueConstraint#noMoreUniqueConstraints()} as true,
     * promising that there are no other uniqueness constraints based
     * on columns other than those defined by
     * {@link org.babyfish.jimmer.sql.Key} properties and
     * {@link org.babyfish.jimmer.sql.LogicalDeleted} in the database.</p>
     */
    NO_MORE_UNIQUE_CONSTRAINTS_REQUIRED,

    /**
     * When saving objects without id properties, Jimmer will
     * make the upsert statement determine whether existing data
     * exists in the database based on {@link org.babyfish.jimmer.sql.Key}
     * properties and {@link org.babyfish.jimmer.sql.LogicalDeleted} property.
     *
     * <p>However, unlike id properties, key properties can be null.
     * In relational databases, null is not equal to anything,
     * including itself, which breaks uniqueness constraints.
     * So, when a key of the current data is null, it will have to
     * execute a select statement containing {@code is null} conditions
     * to make a judgment.</p>
     *
     * <p>Fortunately, some databases can configure the behavior of
     * null values in unique constraints, such as
     * <a href="https://www.postgresql.org/about/featurematrix/detail/392/">
     *     "Nulls not distinct" of Postgres
     * </a></p>
     *
     * If you have already set the uniqueness constraint based on
     * {@link org.babyfish.jimmer.sql.Key} properties and
     * {@link org.babyfish.jimmer.sql.LogicalDeleted} property in
     * the database to {@code nulls not distinct}, you can set
     * {@link KeyUniqueConstraint#isNullNotDistinct()} to true
     * to resolve this issue.
     */
    NULL_NOT_DISTINCT_REQUIRED,

    /**
     * Attempting to save an object without
     * {@link org.babyfish.jimmer.sql.Id} property, but
     * the corresponding entity has not been configured
     * with identity generation strategy.
     *
     * <p>If the {@link org.babyfish.jimmer.sql.Id} type is
     * integer, consider enabling identity increment in the
     * database and modifying the entity code as follows:</p>
     *
     * <pre>{@code
     * @Id
     * @GeneratedValue(strategy = GenerationType.IDENTITY)
     * long id();
     * }</pre>
     */
    IDENTITY_GENERATOR_REQUIRED,

    /**
     * Direct use of a delete statement, but upon discovering that the
     * object being deleted has child objects and the user requires Jimmer
     * to handle these child objects, the delete statement is converted into
     * a delete command
     */
    CANNOT_DELETE_DIRECTLY,

    /**
     * When explicitly updating objects based on
     * {@link org.babyfish.jimmer.sql.Key} properties,
     * the objects being saved do not have any other properties that
     * need to be modified.
     * Simply querying the id data to populate the return result is sufficient.
     *
     * <p>No handling is required for this situation.</p>
     */
    GET_ID_WHEN_UPDATE_NOTHING,

    /**
     * When explicitly modifying objects based on
     * {@link org.babyfish.jimmer.sql.Key} properties, the underlying
     * database lacks the ability to return the ids of existing objects,
     * or this capability has not yet been integrated into Jimmer.
     */
    GET_ID_FOR_KEY_BASE_UPDATE,
}
