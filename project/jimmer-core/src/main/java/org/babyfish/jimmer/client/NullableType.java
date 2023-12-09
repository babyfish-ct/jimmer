package org.babyfish.jimmer.client;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Kotlin: List&lt;String?&gt;
 * Java: List&lt;@NullableType String&gt;
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE_USE)
public @interface NullableType {
}
