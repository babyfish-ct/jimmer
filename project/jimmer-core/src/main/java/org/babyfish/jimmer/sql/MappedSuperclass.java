package org.babyfish.jimmer.sql;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface MappedSuperclass {

    /**
     * If this value is set to true,
     * <ul>
     *     <li>`microServiceName` cannot be specified</li>
     *     <li>Association property cannot be declared</li>
     * </ul>
     */
    boolean acrossMicroServices() default false;
    
    String microServiceName() default "";
}
