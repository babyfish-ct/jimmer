package org.babyfish.jimmer.sql2.example.bll.error;

public class BusinessException extends RuntimeException {

    public BusinessException(String message, Throwable cause) {
        super(message, cause);
    }
}
