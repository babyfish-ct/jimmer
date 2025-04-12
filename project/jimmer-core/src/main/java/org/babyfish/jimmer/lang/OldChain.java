package org.babyfish.jimmer.lang;

import java.lang.annotation.*;

/**
 * Annotates chain style method,
 * indicates that the current method
 * modifies the current object and
 * returns itself, like many methods
 * of {@link StringBuilder}
 *
 * @see NewChain
 */
@Documented
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
public @interface OldChain {}
