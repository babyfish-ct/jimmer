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
 * {@code
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
 * }
 *
 * Example2: Simple calculation based on SQL expression(jimmer-trigger is not used)
 * {@code
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
 * }
 *
 * Example3: Simple calculation based on SQL expression(jimmer-trigger is used)
 * {@code
 * public interface Employee {
 *
 *     ... omit other properties...
 *
 *     String firstName();
 *
 *     String lastName();
 *
 *     @Formula(sql = "concat(FIRST_NAME, LAST_NAME)", dependencies = {"firstName", "lastName"}})
 *     String fullName();
 *
 *     @ManyToOne
 *     Department department();
 *
 *     @Formula(sql = "DEPARTMENT_ID", dependencies = "department")
 *     Long getDepartmentId();
 * }
 *
 * </code></pre>
 *
 * For complex calculation, please view
 * {@link org.babyfish.jimmer.sql.Transient},
 * `org.babyfish.jimmer.sql.TransientResolver` of jimmer-sql and
 * `org.babyfish.jimmer.sql.kt.KTransientResolver` of jimmer-sql-kotlin
 */
@Retention(RetentionPolicy.RUNTIME)
@kotlin.annotation.Target(allowedTargets = AnnotationTarget.PROPERTY)
@Target(ElementType.METHOD)
public @interface Formula {

    /** When `sql` is specified,
     * <ul>
     *     <li>it means the current property a simple calculated based on sql expression</li>
     *     <li>it requires current property to be abstract</li>
     *     <li>In this situation, the current property will be generated in both `Fetcher` and `Table`</li>
     * </ul>
     *
     * When `sql` is not specified,
     * <ul>
     *     <li>it means the current property a simple calculated based on java/kt expression</li>
     *     <li>it requires current property to be non-abstract, and `dependencies` must be specified</li>
     *     <li>In this situation, the current property will be generated in only `Fetcher`</li>
     * </ul>
     */
    String sql() default "";

    /**
     * Property names, not columns names
     *
     * Only need to be specified when any of the following conditions are met
     * <ul>
     *     <li>`sql` is NOT specified</li>
     *     <li>
     *         jimmer-trigger is used and
     *         you hope the events of the dependency fields can cause the event of current field.
     *     </li>
     * </ul>
     */
    String[] dependencies() default {};
}
