package org.babyfish.jimmer.jackson.v2;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @deprecated 请使用 {@link org.babyfish.jimmer.json.jackson.v2.JsonCodecV2}。
 */
@Deprecated
public class JsonCodecV2 extends org.babyfish.jimmer.json.jackson.v2.JsonCodecV2 {

    public JsonCodecV2() {
        super();
    }

    public JsonCodecV2(ObjectMapper mapper) {
        super(mapper);
    }
}
