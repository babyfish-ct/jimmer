package org.babyfish.jimmer.pojo;

import kotlin.annotation.AnnotationTarget;

import java.lang.annotation.*;

@Retention(RetentionPolicy.SOURCE)
@kotlin.annotation.Target(allowedTargets = AnnotationTarget.PROPERTY)
@Target(ElementType.METHOD)
@Repeatable(Statics.class)
public @interface Static {

    /**
     * If the alias is not empty, it specifies field for only 1 static types;
     *
     * Otherwise, it specifies a field for all possible static types.
     *
     */
    String alias() default "";

    /**
     * True means static type has this field,
     *
     * false means static type excludes this field.
     */
    boolean enabled() default true;

    /**
     * It can only be true when the original dynamic property is not null,
     *
     * otherwise, error will be raised during compilation.
     *
     * If a non-null dynamic property is mapped to an optional static property,
     * when static object is used to create dynamic object, null value will not
     * be set into the dynamic instance, that means keep that dynamic property
     * be "unloaded"
     */
    boolean optional() default false;

    /**
     * If static field name is not specified,
     * the name of original dynamic field is used
     */
    String name() default "";

    /**
     * It can only be set to true when the original dynamic property is
     * association(@OneToOne, @OneToMany, @ManyToOne, @ManyToMany)
     *
     * Map the object reference/collection to id(s)
     */
    boolean idOnly() default false;

    /**
     * It can only be set to true when the original dynamic property is
     * association(@OneToOne, @OneToMany, @ManyToOne, @ManyToMany)
     *
     * It tells jimmer how to map the target entity type
     *
     * <ul>
     *      <li>
     *          If the `@StaticType.topLevelName` of target is specified,
     *          reference the target static class directly
     *      </li>
     *      <li>
     *          otherwise,
     *          a nested target static class will be generated in current static class
     *      </li>
     * </ul>
     *
     * <ul>
     *     <li>
     *         If `targetAlias` is not specified, it means a default target,
     *         object shape with only scalar fields.
     *     </li>
     *     <li>
     *         otherwise, the target entity must has a `@StaticType` whose `alias`
     *         matches the `targetAlias` of this annotation. If not, compilation
     *         error will be raised.
     *     </li>
     * </ul>
     */
    String targetAlias() default "";
}
