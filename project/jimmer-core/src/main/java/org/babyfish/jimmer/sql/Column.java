package org.babyfish.jimmer.sql;

import kotlin.annotation.AnnotationTarget;

import java.lang.annotation.*;

/**
 * Optional annotation to specify the column name for scalar properties.
 *
 * <p>If the column name inferred according to the Java/Kotlin property name based
 * on Rule `word1Word2...WordN -> WORD1_WORD2_..._WORDN` is different from the
 * column name in the database, this annotation must be used.</p>
 *
 * <p>Note: This annotation can not be used to map foreign key.
 * For foreign key, {@link JoinColumn} is useful</p>
 */
@Retention(RetentionPolicy.RUNTIME)
@kotlin.annotation.Target(allowedTargets = AnnotationTarget.PROPERTY)
@Target(ElementType.METHOD)
public @interface Column {

    String name() default "";

    /**
     * Optional configuration for non-array column.
     *
     * <p>In most cases, there is no need to
     * specify this attribute unless one of
     * the following conditions applies:</p>
     * <ul>
     *  <li>
     *      The current property type is {@link java.util.UUID}
     *      and you want Jimmer to automatically guess its
     *      {@code ScalarProvider}. That is, automatically decide
     *      whether to use {@code ScalarProvider.uuidByString} or
     *      {@code ScalarProvider.uuidByByteArray} without explicitly
     *      specifying a {@code ScalarProvider}.
     *  </li>
     *  <li>
     *      <p>The {@code IN} condition can easily make SQL
     *      length unstable, thereby reducing SQL cache performance.
     *      To address this, some databases support equality checks
     *      for arrays <i>(i.e., the `= any(?)` syntax)</i>.
     *      In Jimmer, this capability is indicated by having
     *      `Dialect.isAnyEqualityOfArraySupported()` return `true`.</p>
     *
     *      <p>If the current database supports this capability,
     *      `IN (?, ?, ...?)` will automatically be replaced with
     *      `= any(?)`. In this case, the SQL parameter will be an array,
     *      and JDBC's
     *      {@link java.sql.Connection#createArrayOf(String, Object[])}
     *      will be invoked. Clearly, specifying SQL type makes
     *      Jimmer work better.</p>
     *  </li>
     * </ul>
     */
    String sqlType() default "";

    /**
     * For array column
     */
    String sqlElementType() default "";
}
