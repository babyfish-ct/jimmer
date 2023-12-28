package org.babyfish.jimmer.client;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Java-only annotation
 *
 * This type is unnecessary for kotlin, pelase use `?` of kotlin, like this
 * <p>List&lt;String?&gt;</p>
 *
 * However,it is useful for java, for example
 * <p>List&lt;@TNullable String&gt;</p>
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE_USE)
public @interface TNullable {
}
