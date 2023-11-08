package org.babyfish.jimmer;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation is only required by java
 *
 * <p>The `jimmer-apt` handles these annotations</p>
 * <ul>
 *     <li>org.babyfish.jimmer.Immutable</li>
 *     <li>org.babyfish.jimmer.sql.Entity</li>
 *     <li>org.babyfish.jimmer.sql.MappedSuperclass</li>
 *     <li>org.babyfish.jimmer.sql.Embeddable</li>
 *     <li>org.babyfish.jimmer.error.ErrorFamily</li>
 * </ul>
 *
 * If your project does not have any classes decorated by any one of above annotations,
 * Please use this annotation to decorate an any class.
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface EnableDtoGeneration {
}
