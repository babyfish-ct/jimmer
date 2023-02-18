package org.babyfish.jimmer;

import kotlin.annotation.AnnotationTarget;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation is required by simple calculation property.
 *
 * Example1: Simple calculation based on java/kt expression
 * <pre>{@code
 * public interface Employee {
 *
 *     ... omit other properties...
 *
 *     String firstName();
 *
 *     String lastName();
 *
 *     @Formula(dependencies = {"firstName", "lastName"})
 *     default String fullName() {
 *         return firstName() + " " + lastName();
 *     }
 *
 *     @ManyToOne
 *     Department department();
 *
 *     @Formula(dependencies = "department")
 *     default Long getDepartmentId() {
 *         return department() != null ? department.getId() : null;
 *     }
 * }}</pre>
 *
 * Example2: Simple calculation based on SQL expression(jimmer-trigger is not used)
 * <pre>{@code
 * public interface Employee {
 *
 *     ... omit other properties...
 *
 *     String firstName();
 *
 *     String lastName();
 *
 *     @Formula(sql = "concat(FIRST_NAME, LAST_NAME)")
 *     String fullName();
 *
 *     @ManyToOne
 *     Department department();
 *
 *     @Formula(sql = "DEPARTMENT_ID")
 *     Long getDepartmentId();
 * }}</pre>
 *
 * Example3: Simple calculation based on SQL expression(
 * jimmer-trigger is used and you hope
 * the event for formula property will be raised
 * when events for dependency properties are raised)
 *
 * <pre>{@code
 * public interface Employee {
 *
 *     ... omit other properties...
 *
 *     String firstName();
 *
 *     String lastName();
 *
 *     @Formula(
 *         sql = "concat(FIRST_NAME, LAST_NAME)",
 *         dependencies = {"firstName", "lastName"}
 *     )
 *     String fullName();
 *
 *     @ManyToOne
 *     Department department();
 *
 *     @Formula(
 *         sql = "DEPARTMENT_ID",
 *         dependencies = "department"
 *     )
 *     Long getDepartmentId();
 * }}</pre>
 *
 * For complex calculation, please view
 * {@link org.babyfish.jimmer.sql.Transient},
 * `org.babyfish.jimmer.sql.TransientResolver` of jimmer-sql and
 * `org.babyfish.jimmer.sql.kt.KTransientResolver` of jimmer-sql-kotlin
 *
 * @see org.babyfish.jimmer.sql.Transient
 */
@Retention(RetentionPolicy.RUNTIME)
@kotlin.annotation.Target(allowedTargets = AnnotationTarget.PROPERTY)
@Target(ElementType.METHOD)
public @interface Formula {

    /**
     * When the decorated property is abstract,
     * <ul>
     *     <li>It means the current property a simple calculation based on sql expression</li>
     *     <li>The `sql` of this annotation is required</li>
     *     <li>
     *         In this situation, the current property will be generated in
     *         not only `Draft` and `Fetcher` but also `Table`.
     *         That means current formula property can be used by SQL-DSL
     *     </li>
     *     <li>In `Draft`, a function like `setXXX(Type value)` will generated</li>
     * </ul>
     *
     * When the decorated property is non-abstract,
     * <ul>
     *     <li>It means the current property a simple calculation based on java/kotlin expression</li>
     *     <li>The `sql` of the annotation cannot be specified, but `dependencies` must be specified</li>
     *     <li>
     *         In this situation, the current property will be generated in
     *         not only `Draft` and `Fetcher`, but will not be generated in `Table`.
     *         That means current formula property cannot be used by SQL-DSL
     *     </li>
     *     <li>In `Draft`, a function like `useXXX()` will generated</li>
     * </ul>
     */
    String sql() default "";

    /**
     * Property names, not columns names
     *
     * Only need to be specified when any of the following conditions are met
     * <ul>
     *     <li>The decorated property is non-abstract</li>
     *     <li>
     *         jimmer-trigger is used and
     *         you hope the events of the dependency fields can cause the event of current field.
     *     </li>
     * </ul>
     *
     * <ul>
     *     <li>
     *         For abstract formula property based on SQL expression,
     *         `dependencies` are optional, and they can only be scalar properties
     *     </li>
     *     <li>
     *         For non-abstract formula property based on java/kotlin expression,
     *         `dependencies` are required, and they can be not only scalar properties
     *         but also other formula properties.
     *         That means the formula property dependencies can be recursive
     *     </li>
     * </ul>
     */
    String[] dependencies() default {};
}
