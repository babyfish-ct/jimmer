package org.babyfish.jimmer.sql;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface MappedSuperclass {

    /**
     * Can the current type be super type of derived types of any microservices.
     *
     * <ul>
     *     <li>
     *         If this value is set to true,
     *         <ul>
     *             <li>`microServiceName` cannot be specified</li>
     *             <li>Association property cannot be declared</li>
     *         </ul>
     *         , that means current type be super type of derived types of any microservices.
     *     </li>
     *     <li>
     *         Otherwise, the `microServiceName` of this annotation must be equal to
     *         the configuration of derived types
     *     </li>
     * </ul>
     */
    boolean acrossMicroServices() default false;

    /**
     * Can only be specified when `acrossMicroServices` is not specified
     */
    String microServiceName() default "";
}
