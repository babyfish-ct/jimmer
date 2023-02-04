package org.babyfish.jimmer.dto.compiler;

public class DtoAstException extends RuntimeException {

    private final int lineNumber;

    public DtoAstException(int lineNumber, String message) {
        super("Error at line " + lineNumber + ": " + message);
        this.lineNumber = lineNumber;
    }

    public int getLineNumber() {
        return lineNumber;
    }
}
