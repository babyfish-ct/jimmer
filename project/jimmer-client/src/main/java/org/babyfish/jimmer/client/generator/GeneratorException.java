package org.babyfish.jimmer.client.generator;

public class GeneratorException extends RuntimeException {

    public GeneratorException(Throwable cause) {
        super("Cannot generate code, " + cause.getMessage(), cause);
    }
}
