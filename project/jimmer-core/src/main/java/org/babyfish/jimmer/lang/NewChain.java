package org.babyfish.jimmer.lang;

import java.lang.annotation.*;

/**
 * Annotates chain style method,
 * indicates that the current method
 * does not modify the current object
 * and returns new object, like
 * many methods of {@link String}
 *
 * @see OldChain
 */
@Documented
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
public @interface NewChain {}
