package org.babyfish.jimmer.sql.exception;

public class TooManyResultsException extends IllegalResultsException {

    public TooManyResultsException() {
        super("Too many results");
    }
}
