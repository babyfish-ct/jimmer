package org.babyfish.jimmer.client;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <b>Java-only</b> annotation
 *
 * <p>Kotlin uses `?` to describe the nullity metadata,
 * so it is unnecessary to use some annotations such as
 * {@link org.jetbrains.annotations.Nullable}</p>
 *
 * <p>For java, {@link org.jetbrains.annotations.Nullable}
 * can be used to describe nullity metadata, and intellij
 * can support it well, like this</p>
 * <pre>{@code
 * @Nullable
 * Type value;
 * }</pre>
 *
 * <p>Unfortunately, this annotation can only be used to
 * decorate field, parameter or return type. it cannot
 * be used decorated generic type arguments like this</p>
 * <pre>{@code
 * List<@Nullable ElementType> values; //Compilation error
 * }</pre>
 *
 * <p>Be different with that annotation, this {@code TNullable}
 * can be used decorated generic type arguments</p>
 * <pre>{@code
 * List<@TNullable ElementType> values;
 * }</pre>
 */
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.TYPE_USE, ElementType.METHOD})
public @interface TNullable {
}
