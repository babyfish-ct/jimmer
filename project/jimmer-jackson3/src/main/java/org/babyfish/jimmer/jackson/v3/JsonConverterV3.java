package org.babyfish.jimmer.jackson.v3;

import tools.jackson.databind.ObjectMapper;

/**
 * @deprecated 请使用 {@link org.babyfish.jimmer.json.jackson.v3.JsonConverterV3}。
 */
@Deprecated
public class JsonConverterV3 extends org.babyfish.jimmer.json.jackson.v3.JsonConverterV3 {

    public JsonConverterV3(ObjectMapper mapper) {
        super(mapper);
    }
}
