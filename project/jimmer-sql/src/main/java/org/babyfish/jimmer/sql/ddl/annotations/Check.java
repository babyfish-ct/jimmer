package org.babyfish.jimmer.sql.ddl.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * @author honhimW
 */
@Target({})
@Retention(RUNTIME)
public @interface Check {

    String name() default "";

    String constraint();

}
