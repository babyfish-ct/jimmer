package org.babyfish.jimmer.jackson;

public class ImmutableModuleRequiredException extends RuntimeException {

    public ImmutableModuleRequiredException() {
        super(
                "Immutable object cannot be serialized by ordinary ObjectMapper, " +
                        "please register the \"" +
                        ImmutableModule.class +
                        "\" into the ObjectMapper"
        );
    }
}
