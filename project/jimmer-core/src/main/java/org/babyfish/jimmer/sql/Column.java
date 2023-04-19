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
    String name();
}
