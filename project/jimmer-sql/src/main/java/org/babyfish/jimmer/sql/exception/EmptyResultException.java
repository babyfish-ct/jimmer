package org.babyfish.jimmer.sql.exception;

public class EmptyResultException extends IllegalResultsException {
    public EmptyResultException() {
        super("The result set is empty");
    }
}
