package org.babyfish.jimmer.sql;

import kotlin.annotation.AnnotationTarget;
import org.babyfish.jimmer.sql.meta.UserIdGenerator;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@kotlin.annotation.Target(allowedTargets = AnnotationTarget.PROPERTY)
@Target(ElementType.METHOD)
public @interface GeneratedValue {

    GenerationType strategy() default GenerationType.AUTO;

    /**
     * @return A class implements
     * `org.babyfish.jimmer.sql.metadata.UserIdGenerator`.
     *
     * <ul>
     *     <li>If this class is manged by IOC framework, the constructors can have any parameters</li>
     *     <li>Otherwise, default constructor must be supported</li>
     * </ul>
     *
     * If this argument is specified, {@link #generatorRef()} cannot be specified
     */
    Class<? extends UserIdGenerator<?>> generatorType() default UserIdGenerator.None.class;

    /**
     * @return The name of bean manged by IOC framework(eg, spring).
     *
     * If this argument is specified, {@link #generatorType()} cannot be specified
     */
    String generatorRef() default "";

    String sequenceName() default "";
}
