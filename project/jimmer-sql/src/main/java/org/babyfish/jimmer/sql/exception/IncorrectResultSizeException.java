package org.babyfish.jimmer.sql.exception;

import org.jetbrains.annotations.Nullable;

public class IncorrectResultSizeException extends RuntimeException {

    private final int expectedSize;
    private final int actualSize;

    public IncorrectResultSizeException(int expectedSize, int actualSize) {
        super("Incorrect result size: expected " + expectedSize + ", actual " + actualSize);
        this.expectedSize = expectedSize;
        this.actualSize = actualSize;
    }

    public IncorrectResultSizeException(@Nullable String msg, int expectedSize, int actualSize) {
        super(msg);
        this.expectedSize = expectedSize;
        this.actualSize = actualSize;
    }
}
