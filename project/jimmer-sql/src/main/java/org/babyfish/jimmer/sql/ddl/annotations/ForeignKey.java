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
public @interface ForeignKey {

    String name() default "";

    String definition() default "";

    OnDeleteAction action() default OnDeleteAction.NONE;

    Class<? extends ConstraintNamingStrategy> naming() default ConstraintNamingStrategy.class;

}
