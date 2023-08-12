package org.babyfish.jimmer.dto.compiler;

public class DtoAstException extends RuntimeException {

    private final String path;

    private final int lineNumber;

    public DtoAstException(String path, int lineNumber, String message) {
        super("Error at line " + lineNumber + " of \"" + path + "\": " + message);
        this.path = path;
        this.lineNumber = lineNumber;
    }

    public String getPath() {
        return path;
    }

    public int getLineNumber() {
        return lineNumber;
    }
}
