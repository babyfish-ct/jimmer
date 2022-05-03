package org.babyfish.jimmer;

public class UnloadedException extends RuntimeException {

    private final Class<?> type;

    private final String prop;

    public UnloadedException(Class<?> type, String prop) {
        super(
                String.format(
                        "The property \"%s.%s\" is unloaded",
                        type.getName(),
                        prop
                )
        );
        this.type = type;
        this.prop = prop;
    }
}
