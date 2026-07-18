package org.babyfish.jimmer.jackson.v2;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @deprecated 请使用 {@link org.babyfish.jimmer.json.jackson.v2.JsonConverterV2}。
 */
@Deprecated
public class JsonConverterV2 extends org.babyfish.jimmer.json.jackson.v2.JsonConverterV2 {

    public JsonConverterV2(ObjectMapper mapper) {
        super(mapper);
    }
}
