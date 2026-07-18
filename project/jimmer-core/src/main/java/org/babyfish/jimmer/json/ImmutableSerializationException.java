package org.babyfish.jimmer.json;

public class ImmutableSerializationException extends RuntimeException {

    public ImmutableSerializationException() {
        super(
                "Immutable object cannot be serialized by ordinary JSON serializer, " +
                        "please register Jimmer immutable serialization support or use JsonCodec"
                );
    }
}
