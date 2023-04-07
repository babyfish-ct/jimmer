package org.babyfish.jimmer.sql.fetcher.compiler;

public class FetcherCompileException extends RuntimeException {

    private final int line;

    private final int charPositionInLine;

    public FetcherCompileException(String message, int line, int charPositionInLine) {
        super(finalMessage(message, line, charPositionInLine));
        this.line = line;
        this.charPositionInLine = charPositionInLine;
    }

    public FetcherCompileException(String message, Throwable cause, int line, int charPositionInLine) {
        super(finalMessage(message, line, charPositionInLine), cause);
        this.line = line;
        this.charPositionInLine = charPositionInLine;
    }

    private static String finalMessage(String message, int line, int charPositionInLine) {
        return "Cannot compile fetcher(line: " +
                line +
                ", " +
                "position: " +
                charPositionInLine +
                "): " + message;
    }

    public int getLine() {
        return line;
    }

    public int getCharPositionInLine() {
        return charPositionInLine;
    }

    public static class CodeBasedFilterException extends FetcherCompileException {

        public CodeBasedFilterException(String message, int line, int charPositionInLine) {
            super(message, line, charPositionInLine);
        }
    }

    public static class CodeBasedRecursionException extends FetcherCompileException {

        public CodeBasedRecursionException(String message, int line, int charPositionInLine) {
            super(message, line, charPositionInLine);
        }
    }
}
