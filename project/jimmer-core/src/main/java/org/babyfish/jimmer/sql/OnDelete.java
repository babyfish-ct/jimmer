package org.babyfish.jimmer.sql;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The annotation can only be used on reference association
 * based on foreign key.
 *
 * <div>
 *     When the parent object of some child objects is deleted,
 *     <ul>
 *         <li>If the attribute of @OnDelete is NONE, throw exception</li>
 *         <li>
 *             If the attribute of @OnDelete is SET_NULL,
 *             they parent properties of child objects will be set null
 *         </li>
 *         <li>
 *             If the attribute of @OnDelete is DELETE,
 *             the child objects will be deleted too.
 *         </li>
 *     </ul>
 * </div>
 *
 * <div>
 *     <b>Note:</b>
 *     <p>
 *          Parent object can discard some child objects.
 *          When using the save command, if the parent object in the database
 *          has some child objects, but the user overrides it and the new parent
 *          object argument specified by the user does not contain some existing
 *          child objects, these child objects will be discarded.
 *     </p>
 *     <p>
 *          In this case, although the parent object is not deleted,
 *          the same method is used to clear the parent reference of
 *          the discarded child objects or delete the discarded child objects.
 *     </p>
 * </div>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface OnDelete {

    DeleteAction value();
}
