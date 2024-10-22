package org.babyfish.jimmer.sql.exception;

public abstract class IllegalResultsException extends RuntimeException{

    protected IllegalResultsException(String message) {
        super(message);
    }
}
