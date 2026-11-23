package org.babyfish.jimmer.json;

import org.babyfish.jimmer.jackson.v2.JsonCodecV2;
import org.babyfish.jimmer.jackson.v3.JsonCodecV3;
import org.babyfish.jimmer.json.fastjson2.Fastjson2Codec;
import org.babyfish.jimmer.json.gson.GsonCodec;

import java.util.List;

final class JsonTestProviders {

    static final List<JsonCodec> CODECS = List.of(
            new JsonCodecV2(), new JsonCodecV3(), new GsonCodec(), new Fastjson2Codec()
    );

    private JsonTestProviders() {}
}
