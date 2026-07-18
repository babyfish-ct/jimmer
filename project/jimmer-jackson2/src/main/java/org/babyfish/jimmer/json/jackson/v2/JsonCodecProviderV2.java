package org.babyfish.jimmer.json.jackson.v2;

import org.babyfish.jimmer.json.codec.JsonCodec;
import org.babyfish.jimmer.json.codec.JsonCodecProvider;
import org.jspecify.annotations.NonNull;

public class JsonCodecProviderV2 implements JsonCodecProvider {

    @Override
    public int priority() {
        return 200;
    }

    @Override
    @NonNull
    public JsonCodec codec() {
        return new JsonCodecV2();
    }
}
