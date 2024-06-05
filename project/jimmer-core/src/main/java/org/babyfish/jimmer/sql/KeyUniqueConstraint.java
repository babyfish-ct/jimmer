package org.babyfish.jimmer.sql;

import java.lang.annotation.*;

/**
 * This annotation can affect the behavior of the save command.
 *
 * <ul>
 *
 * <li>If an entity class is decorated with this annotation,
 * it means that there is a uniqueness constraint in the database
 * that contains the '@Key' fields and logical deleted flag field,
 * and it is possible (but not absolute) that the SQL level ability
 * to to implement upsert operations.</li>
 *
 * <li>Otherwise, it is up to jimmer to determine whether an inserting
 * or updating should be executed through a SQL-query.</li>
 *
 * </ul>
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface KeyUniqueConstraint {

    /**
     * This argument is designed for MySQL.
     *
     * <p>If you save an object with an ID proeprty,
     * MySQL's `insert... on duplicate...` operation can complete the
     * upsert operation well, and there is no need configure it.</p>
     *
     * <p>However, this argument is useful for saving objects that
     * do not have id property.</p>
     *
     * <p>For object without id property,
     * MySQL's `insert... on duplicate...` statement indicates whether
     * an insert or update should be executed based on all other unique
     * fields except the primary key. These uniqueness fields may not
     * belong to one same uniqueness constraint, and may belong to
     * different uniqueness constraints. If the latter is the case,
     * jimmer will give up the ability to use the SQL-level upsert.</p>
     *
     * <p>In this case, you can configure this parameter to true to tell
     * jimmer that the current entity has only one unique constraint in
     * the database, thus increasing the probability that jimmer will use
     * the upsert capability at the SQL level</p>
     */
    boolean onlyOneUniqueConstraint() default false;

    /**
     * This argument is designed for Postgres.
     *
     * <p>Postgres' `insert... on conflict(f1, f2, ..., fn) do ...`
     * statement requires conflicting columns 'f1, f2,... fn' must be
     * members of a uniqueness constraint.</p>
     *
     * <p>In most databases, null is treated as a special treat,
     * and null is not equal to itself. Even if there is a uniqueness
     * constraint, null fields can lead to duplicate data.
     * Therefore, if some objects contain null `@key` properties,
     * by default, jimmer will also give up the upsert capability of
     * SQL-level and execute an additional SQL query.</p>
     *
     * <p>Fortunately, postgres is very powerful, it allows developers
     * to specify how to handle nullable fields of unique constraint.
     * If you have already set the 'NULLS NOT DISTINCT' option for the
     * unique constraint in database, you can configure this argument
     * to true to increase the probability that jimmer will use the
     * upsert capability at the SQL level</p>
     */
    boolean isNullNotDistinct() default false;
}
