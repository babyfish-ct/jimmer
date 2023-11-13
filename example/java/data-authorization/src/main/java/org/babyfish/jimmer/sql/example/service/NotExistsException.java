package org.babyfish.jimmer.sql.example.service;

public class NotExistsException extends RuntimeException {

    private final String fullPath;

    private final String errorPath;

    public NotExistsException(String path, String errorPath) {
        this.fullPath = path;
        this.errorPath = errorPath;
    }

    public String getFullPath() {
        return fullPath;
    }

    public String getErrorPath() {
        return errorPath;
    }
}
