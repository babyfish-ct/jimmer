package org.babyfish.jimmer.jackson.v2;

import org.babyfish.jimmer.json.codec.JsonCodec;
import org.babyfish.jimmer.json.codec.JsonCodecFamily;
import org.babyfish.jimmer.json.codec.JsonCodecProvider;
import org.jspecify.annotations.NonNull;

public class JsonCodecProviderV2 implements JsonCodecProvider {

    @Override
    @NonNull
    public JsonCodecFamily family() {
        return JsonCodecFamily.JACKSON2;
    }

    @Override
    @NonNull
    public JsonCodec create() {
        return new JsonCodecV2();
    }
}
