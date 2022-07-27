package org.babyfish.jimmer;

/**
 * When get the property value of immutable object,
 * if the property is not loaded, this exception will be thrown.
 */
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

    public Class<?> getType() {
        return type;
    }

    public String getProp() {
        return prop;
    }
}
