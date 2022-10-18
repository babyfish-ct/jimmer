package org.babyfish.jimmer.sql;

import kotlin.annotation.AnnotationTarget;

import java.lang.annotation.*;

/**
 * The annotation can only be used on reference association
 * based on foreign key.
 *
 * <div>
 *     When the parent object of some child objects is deleted,
 *     or when some child objects of a parent object are discorded,
 *     <ul>
 *         <li>If the attribute of @OnDissociate is NONE, throw exception</li>
 *         <li>
 *             If the attribute of @OnDissociate is SET_NULL,
 *             they parent properties of child objects will be set null
 *         </li>
 *         <li>
 *             If the attribute of @OnDissociate is DELETE,
 *             the child objects will be deleted too.
 *         </li>
 *     </ul>
 * </div>
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@kotlin.annotation.Target(allowedTargets = AnnotationTarget.PROPERTY)
@Target(ElementType.METHOD)
public @interface OnDissociate {

    DissociateAction value();
}
