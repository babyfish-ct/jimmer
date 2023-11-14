package org.babyfish.jimmer.sql;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation is only required by java, not kotlin.
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
 * If the current project does not have any classes decorated by any one of above annotations,
 * the `jimmer-apt` will not be triggered, please write an unuseful empty class and use this
 * annotation to decorate it.
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface EnableDtoGeneration {
}
