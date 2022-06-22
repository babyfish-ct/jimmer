package org.babyfish.jimmer;

/**
 * If {@link DraftConsumer} throws an exception
 * that is neither RuntimeException nor Error,
 * wrap that exception and then rethrow.
 */
public class DraftConsumerUncheckedException extends RuntimeException {

    private DraftConsumerUncheckedException(Throwable ex) {
        super(
                "Cannot produce immutable because an checked exception " +
                        "is raised in draft consumer lambda expression",
                ex
        );
        if (ex instanceof RuntimeException || ex instanceof Error) {
            throw new IllegalArgumentException(
                    "DraftConsumerUncheckedException cannot wrap RuntimeException or Error"
            );
        }
    }

    /**
     * <p>
     *  If the original exception is RuntimeException or Error,
     *  throws it directly.
     * </p>
     *
     * <p>
     *  Otherwise, throws a wrapper whose type is
     *  {@link DraftConsumerUncheckedException}
     * </p>
     *
     * @param ex Original exception
     */
    public static void rethrow(Throwable ex) {
        if (ex instanceof RuntimeException) {
            throw (RuntimeException)ex;
        }
        if (ex instanceof Error) {
            throw (Error)ex;
        }
        throw new DraftConsumerUncheckedException(ex);
    }
}
