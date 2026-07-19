package org.babyfish.jimmer.jackson.v2;

import org.babyfish.jimmer.jackson.codec.JsonCodec;
import org.babyfish.jimmer.jackson.codec.JsonCodecProvider;
import org.jspecify.annotations.NonNull;

public class JsonCodecProviderV2 implements JsonCodecProvider {
    @Override
    @NonNull
    public JsonCodec<?> create() {
        return new JsonCodecV2();
    }
}
