package org.babyfish.jimmer.jackson.v3;

import tools.jackson.databind.json.JsonMapper;

/**
 * @deprecated 请使用 {@link org.babyfish.jimmer.json.jackson.v3.JsonCodecV3}。
 */
@Deprecated
public class JsonCodecV3 extends org.babyfish.jimmer.json.jackson.v3.JsonCodecV3 {

    public JsonCodecV3() {
        super();
    }

    public JsonCodecV3(JsonMapper mapper) {
        super(mapper);
    }
}
