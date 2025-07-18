package org.babyfish.jimmer.sql.ddl.annotations;

import org.babyfish.jimmer.meta.ImmutableProp;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.sql.Types;

/**
 * @author honhimW
 */

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ColumnDef {

    /**
     * Override {@link ImmutableProp#isNullable()} if not Nullable.Null
     */
    Nullable nullable() default Nullable.NULL;

    /**
     * Column type definition, e.g. datetime, json, varchar(255), nvarchar($l)
     */
    String sqlType() default "";

    int jdbcType() default Types.OTHER;

    /**
     * Less than 0 represent using dialect default.
     */
    long length() default -1;

    /**
     * Less than 0 represent using dialect default.
     */
    int precision() default -1;

    /**
     * Less than 0 represent using dialect default.
     */
    int scale() default -1;

    String comment() default "";

    /**
     * if not blank generate as: column_name + `definition`
     */
    String definition() default "";

    ForeignKey foreignKey() default @ForeignKey;

    enum Nullable {
        TRUE, FALSE, NULL
    }

}
