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
     * For non-array column
     *
     * <div>
     *     If these two conditions are matched
     *     <ol>
     *         <li>The `any(?)` is supported by database, that is, `Dialect.isAnyOfArraySupported()` returns true</li>
     *         <li>The `sqlType` of this annotation is specified, or the current property returns `long` or `Long`</li>
     *     </ol>, the in-collection predicate will be optimized.
     *     For example:
     *     <pre>where(table.id().in(Arrays.asList(1L, 2L, 3L, 4L)))</pre>
     *     generate the SQL
     *     <pre>where tb_1_.ID = any(?)</pre>
     * </div>
     *
     * <ul>
     *     <li>If the property type is `long` or `Long`, it will be considered as `bigint`</li>
     *     <li>Otherwise, please specify it manually</li>
     * </ul>
     */
    String sqlType() default "";

    /**
     * For array column
     */
    String sqlElementType() default "";
}
