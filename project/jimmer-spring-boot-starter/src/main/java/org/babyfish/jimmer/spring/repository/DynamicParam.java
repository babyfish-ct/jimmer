package org.babyfish.jimmer.spring.repository;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This method should only be used to decorate
 * the parameters of abstract methods in derived
 * interfaces of {@link JRepository} or {@link KRepository}.
 *
 * <p>When a parameter is null</p>
 * <ul>
 *     <li>
 *         If this annotation is present,
 *         the SQL condition is ignored, meaning dynamic query is performed.
 *     </li>
 *     <li>
 *         If this annotation is not present,
 *         {@link NullPointerException} will be raised
 *     </li>
 * </ul>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface DynamicParam {
}
