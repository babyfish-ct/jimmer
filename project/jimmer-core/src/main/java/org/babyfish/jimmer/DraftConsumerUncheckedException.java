package org.babyfish.jimmer;

public class DraftConsumerUncheckedException extends RuntimeException {

    public DraftConsumerUncheckedException(Throwable ex) {
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
}
