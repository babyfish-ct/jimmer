package org.babyfish.jimmer.sql.ddl.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author honhimW
 */

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface TableDef {

    Unique[] uniques() default {};

    Index[] indexes() default {};

    String comment() default "";

    Check[] checks() default {};

    /**
     * MySQL engine type
     */
    String tableType() default "";


}
