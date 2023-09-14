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
     *     <li>If this class is manged by spring, the constructors can have any parameters</li>
     *     <li>Otherwise, default constructor must be supported</li>
     * </ul>
     */
    Class<? extends UserIdGenerator<?>> generatorType() default UserIdGenerator.None.class;


    String sequenceName() default "";
}
