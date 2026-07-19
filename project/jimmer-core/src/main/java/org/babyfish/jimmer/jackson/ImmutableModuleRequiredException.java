package org.babyfish.jimmer.jackson;

public class ImmutableModuleRequiredException extends RuntimeException {

    public ImmutableModuleRequiredException() {
        super(
                "Immutable object cannot be serialized by ordinary ObjectMapper, " +
                        "please register the ImmutableModuleV1 or ImmutableModuleV2 into the ObjectMapper"
        );
    }
}
