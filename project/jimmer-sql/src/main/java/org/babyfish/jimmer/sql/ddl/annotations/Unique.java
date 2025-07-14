package org.babyfish.jimmer.sql.ddl.annotations;

import org.babyfish.jimmer.sql.ddl.ConstraintNamingStrategy;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * @author honhimW
 */
@Target({})
@Retention(RUNTIME)
public @interface Unique {

    String name() default "";

    /**
     * (Required) An array of the column names that make up the constraint.
     */
    String[] columns();

    Kind kind() default Kind.PATH;

    Class<? extends ConstraintNamingStrategy> naming() default ConstraintNamingStrategy.class;

}
