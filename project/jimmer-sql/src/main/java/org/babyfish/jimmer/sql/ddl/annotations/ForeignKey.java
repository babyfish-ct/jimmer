package org.babyfish.jimmer.sql.ddl.annotations;


import org.babyfish.jimmer.sql.ddl.ConstraintNamingStrategy;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.annotation.RetentionPolicy;

/**
 * @author honhimW
 */
@Target({})
@Retention(RetentionPolicy.RUNTIME)
public @interface ForeignKey {

    String name() default "";

    String definition() default "";

    OnDeleteAction action() default OnDeleteAction.NONE;

    Class<? extends ConstraintNamingStrategy> naming() default ConstraintNamingStrategy.class;

}
