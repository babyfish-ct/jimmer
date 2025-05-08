package org.babyfish.jimmer.spring.repository;

import org.babyfish.jimmer.spring.repository.config.JimmerRepositoriesRegistrarForAnnotation;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

@Target({ElementType.TYPE, ElementType.METHOD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@Import(JimmerRepositoriesRegistrarForAnnotation.class)
public @interface DynamicQuery {

    /**
     * Enable dynamic query on specified Repository or method
     */
    boolean value() default true;
}
