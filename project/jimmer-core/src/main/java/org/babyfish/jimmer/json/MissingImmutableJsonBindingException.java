package org.babyfish.jimmer.json;

public class MissingImmutableJsonBindingException extends RuntimeException {

    public MissingImmutableJsonBindingException() {
        super(
                "The JSON mapper has no binding for Jimmer immutable objects. " +
                        "For Jackson, register ImmutableModuleV2 or ImmutableModuleV3 with the ObjectMapper."
        );
    }
}
