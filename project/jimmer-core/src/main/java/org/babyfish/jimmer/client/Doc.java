package org.babyfish.jimmer.client;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Doc comment save supports three ways:
 * 1. Save only the comment information of the current class.
 * 2. Save all classes on the dependency tree of the current class.
 * 3. Save the classes on the dependency tree of the current class (and only the classes contained in the current module)
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface Doc {
    /**
     * same as saveAstNode
     */
    boolean value() default false;

    /**
     * only save the current class or the classes on the dependency tree
     */
    boolean saveAstNode() default false;

    /**
     * Pre-Condition: saveAstNode is true
     * whether the dependency tree only store the classes contained in the current module
     */
    boolean onlySaveCurrentModuleClass() default true;
}
