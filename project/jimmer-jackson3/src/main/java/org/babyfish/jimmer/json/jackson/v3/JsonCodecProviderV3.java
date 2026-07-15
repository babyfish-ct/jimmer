package org.babyfish.jimmer.json.jackson.v3;

import org.babyfish.jimmer.json.codec.JsonCodec;
import org.babyfish.jimmer.json.codec.JsonCodecProvider;
import org.jspecify.annotations.NonNull;

public class JsonCodecProviderV3 implements JsonCodecProvider {

    @Override
    public int priority() {
        return 300;
    }

    @Override
    @NonNull
    public JsonCodec codec() {
        return new JsonCodecV3();
    }
}
