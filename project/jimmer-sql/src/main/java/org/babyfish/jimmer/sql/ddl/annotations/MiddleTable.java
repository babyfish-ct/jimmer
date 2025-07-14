package org.babyfish.jimmer.sql.ddl.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author honhimW
 */

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface MiddleTable {

    /**
     * auto-increment id instead of composite-primary-key.
     * <p>
     * <pre>
     * -- If true
     * create table middle_table (
     *   id integer not null auto_increment,
     *   join_id ...,
     *   inverse_join_id ...,
     *   primary key (id),
     *   constraint uk_join_id_inverse_join_id unique (join_id, inverse_join_id)
     * )
     * -- If false
     * create table middle_table (
     *   join_id ...,
     *   inverse_join_id ...,
     *   primary key (join_id, inverse_join_id)
     * )
     * </pre>
     */
    boolean useAutoId() default false;

    boolean useRealForeignKey() default true;

    ForeignKey joinColumnForeignKey() default @ForeignKey;

    ForeignKey inverseJoinColumnForeignKey() default @ForeignKey;

    String comment() default "";

    /**
     * MySQL engine type
     */
    String tableType() default "";

}
