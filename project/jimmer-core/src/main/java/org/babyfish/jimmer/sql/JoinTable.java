package org.babyfish.jimmer.sql;

import kotlin.annotation.AnnotationTarget;
import org.babyfish.jimmer.sql.meta.LogicalDeletedValueGenerator;

import java.lang.annotation.*;

/**
 * This annotation should typically be used with {@link ManyToMany},
 * but it can also be used with {@link ManyToOne} or {@link OneToOne}.
 *
 * <p>The annotation cannot be used by the reversed(with `mappedBy`) property</p>
 *
 * <p>The middle table should only have two columns:</p>
 * <ul>
 *     <li>one pointing to the foreign key of the entity in which the current property resides</li>
 *     <li>and the other pointing to the foreign key of the entity returned by the current property</li>
 * </ul>
 * The two foreign keys are combined to serve as the primary key.
 *
 * <ul>
 *     <li>
 *         For many-to-many associations,
 *         if you want to add more business fields to the middle table,
 *         this annotation is no longer applicable, please use {@link ManyToManyView}
 *     </li>
 *     <li>
 *         For many-to-one associations, unique constraint should be added to the
 *         foreign key pointing to the entity in which the current property resides.
 *     </li>
 *     <li>
 *         For one-to-one associations, unique constraint should be added to each foreign key.
 *     </li>
 * </ul>
 */
@Retention(RetentionPolicy.RUNTIME)
@kotlin.annotation.Target(allowedTargets = AnnotationTarget.PROPERTY)
@Target(ElementType.METHOD)
public @interface JoinTable {

    /**
     * Middle table name.
     *
     * <p>
     *     If it is not specified, it will be determined by Jimmer.
     *     For example, If the current entity is `Book` and the target entity is `Author`,
     *     the default middle table name is `BOOK_AUTHOR_MAPPING`
     * </p>
     */
    String name() default "";

    /**
     * The column name of the foreign key of the entity in which the current property resides
     *
     * <p>This argument cannot be specified together with {@link #joinColumns()}</p>
     *
     * It is better than {@link #joinColumns()} when foreign key
     * is real database constraint and has only one column
     */
    String joinColumnName() default "";

    /**
     * The column names of the foreign key of the entity returned by the current property
     *
     * <p>This argument cannot be specified together with {@link #inverseJoinColumns()}</p>
     *
     * It is better than {@link #inverseJoinColumns()} when foreign key
     * is real database constraint and has only one column
     */
    String inverseJoinColumnName() default "";

    /**
     * The column name of the foreign key of the entity in which the current property resides
     *
     * <p>This argument cannot be specified together with {@link #joinColumnName()}</p>
     *
     * This argument must be configured when foreign key
     *      * is not real database constraint or has only more than 1 columns
     */
    JoinColumn[] joinColumns() default {};

    /**
     * The column names of the foreign key of the entity returned by the current property
     *
     * <p>This argument cannot be specified together with {@link #inverseJoinColumnName()}</p>
     *
     * This argument must be configured when foreign key
     * is not real database constraint or has only more than 1 column
     */
    JoinColumn[] inverseJoinColumns() default {};

    boolean readonly() default false;

    boolean preventDeletionBySource() default false;

    boolean preventDeletionByTarget() default false;

    /**
     * Should the middle table row be cascade deleted when the source object is deleted.
     *
     * <p>This argument cannot be true when `preventDeletionBySource` is true</p>
     *
     * <p>
     *     This argument is used config the database-level cascade deletion for foreign key in middle table.
     *     for foreign key in entity table, please view {@link DissociateAction#LAX}
     * </p>
     * @return Should the middle table row be cascade deleted when the source object is deleted.
     */
    boolean cascadeDeletedBySource() default false;

    /**
     * Should the middle table row be cascade deleted when the target object is deleted.
     *
     * <p>This argument cannot be true when `preventDeletionByTarget` is true</p>
     *
     * <p>
     *     This argument is used config the database-level cascade deletion for foreign key in middle table.
     *     for foreign key in entity table, please view {@link DissociateAction#LAX}
     * </p>
     * @return Should the middle table row be cascade deleted when the target object is deleted.
     */
    boolean cascadeDeletedByTarget() default false;

    /**
     * In general, if entities on either side support logical deletion,
     * middle tables should also support logical deletion.
     * Otherwise, error will be raised.
     *
     * <p>
     *     When entity on either side is logically deleted,
     *     the rows of middle tables should be logically deleted too.
     * </p>
     *
     * <p>
     *     However, if you don't want intermediate table records to support soft deletion,
     *     turn this switch on. When an object at either side is logically deleted,
     *     the corresponding middle table records will be physically deleted.
     * </p>
     */
    boolean deletedWhenEndpointIsLogicallyDeleted() default false;

    JoinTableFilter filter() default @JoinTableFilter(
            columnName = "<illegal-column-name>",
            values = {}
    );

    LogicalDeletedFilter logicalDeletedFilter() default @LogicalDeletedFilter(
            columnName = "<illegal-column-name>"
    );

    KeyUniqueConstraint keyUniqueConstraint() default @KeyUniqueConstraint(
            noMoreUniqueConstraints = false,
            isNullNotDistinct = false
    );

    @interface JoinTableFilter {

        String columnName();

        Class<?> type() default String.class;

        String[] values();
    }

    @interface LogicalDeletedFilter {

        String columnName();

        Class<?> type() default boolean.class;

        boolean nullable() default false;

        String value() default "";

        Class<? extends LogicalDeletedValueGenerator<?>> generatorType() default LogicalDeletedValueGenerator.None.class;

        String generatorRef() default "";

        String initializedValue() default "";
    }
}
