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
public @interface Index {

    String name() default "";

    /**
     * (Required) The names of the columns to be included in the index,
     * in order.
     */
    String[] columns();

    /**
     * (Optional) Whether the index is unique.
     */
    boolean unique() default false;

    Kind kind() default Kind.PATH;

    Class<? extends ConstraintNamingStrategy> naming() default ConstraintNamingStrategy.class;

}
