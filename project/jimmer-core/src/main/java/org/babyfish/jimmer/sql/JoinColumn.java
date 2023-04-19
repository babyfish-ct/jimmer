package org.babyfish.jimmer.sql;

import kotlin.annotation.AnnotationTarget;

import java.lang.annotation.*;

/**
 * Optional annotation to specify the column name for foreign key properties,
 * that means many-to-one or one-to-one property which is neither reversed
 * (with `mappedBy`) nor based on middle table.
 *
 * <p>If the column name inferred according to the Java/Kotlin property name based
 * on Rule `word1Word2...WordN -> WORD1_WORD2_..._WORDN_ID` is different from the
 * column name in the database, or the foreign key is fake(Not a real foreign key
 * constraint in the database, but a concept only in the minds of developers),
 * this annotation must be used.</p>
 *
 * <p>Note: This annotation can only be used to map foreign key.
 * For scalar column, {@link Column} is useful</p>
 *
 * <p></p>
 */
@Retention(RetentionPolicy.RUNTIME)
@kotlin.annotation.Target(allowedTargets = AnnotationTarget.PROPERTY)
@Target(ElementType.METHOD)
@Repeatable(JoinColumns.class)
public @interface JoinColumn {

    /**
     * A column name of foreign key
     */
    String name();

    /**
     * A column name of the primary key of the parent table referenced by the foreign key
     *
     * <p>If the foreign key has only one column, it is unnecessary to specify it</p>
     */
    String referencedColumnName() default "";

    /**
     * <ul>
     * <li>If ture(default), the foreign key is real, that means it a real foreign key
     * constraint in the database</li>
     *
     * <li>Otherwise, the foreign key is fake, which is not a real foreign key
     * constraint in database but a concept only in the minds of developers</li>
     * </ul>
     */
    boolean foreignKey() default true;
}
