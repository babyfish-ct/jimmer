package org.babyfish.jimmer;

/**
 * <p>
 *     jimmer prohibits bidirectional associations between different objects,
 *     any code that attempts to do so will result in this exception
 * </p>
 *
 * <p>
 *     Please view
 *     <a href="https://babyfish-ct.github.io/jimmer-doc/docs/object/immutable/reason/#incorrect-demo">
 *         documentation
 *     </a> to know more.
 * </p>
 */
public class CircularReferenceException extends RuntimeException {
}
