package org.babyfish.jimmer.sql.exception;

import org.jetbrains.annotations.Nullable;

public class EmptyResultException extends IncorrectResultSizeException {
    public EmptyResultException(int expectedSize) {
        super(expectedSize, 0);
    }

    public EmptyResultException(@Nullable String msg, int expectedSize) {
        super(msg, expectedSize, 0);
    }
}
