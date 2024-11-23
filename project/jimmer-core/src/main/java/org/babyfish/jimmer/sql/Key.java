package org.babyfish.jimmer.sql;

import kotlin.annotation.AnnotationTarget;

import java.lang.annotation.*;

/**
 * In ORM, there are two concepts: Surrogate id and Natural id.
 *
 * <ul>
 *     <li>{@link Id} is used to specify surrogate id</li>
 *     <li>This annotation is used to specify natural id</li>
 * </ul>
 *
 * <b>Notes</b>
 * <ol>
 *     <li>
 *         Multiple properties based on column(s) of an entity
 *         can be decorated by this annotation
 *     </li>
 *     <li>
 *         One-to-one/Many-to-one association property
 *         based on foreign key can be a part of key
 *         <div>For example</div>
 *         <pre><code>
 * &#64;Entity
 * public interface TreeNode {
 *     &#64;Id
 *     long id();
 *
 *     &#64;Key
 *     &#64;ManyToOne
 *     &#64;Nullable
 *     TreeNode parent();
 *
 *     &#64;Key
 *     String name();
 * }
 *         </code></pre>
 *         Like the file system of OS, file name is not globally unique,
 *         but unique under one parent directory.
 *     </li>
 * </ol>
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@kotlin.annotation.Target(allowedTargets = AnnotationTarget.PROPERTY)
@Target(ElementType.METHOD)
@Repeatable(Keys.class)
public @interface Key {

    /**
     * In general, this configuration is not needed, unless
     * you need to configure multiple Natural Key constraints
     * for an entity. A good choice is the unique constraint
     * name in the database.
     */
    String group() default "";
}
