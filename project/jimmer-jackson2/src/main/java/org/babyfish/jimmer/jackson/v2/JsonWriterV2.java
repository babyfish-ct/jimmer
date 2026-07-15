package org.babyfish.jimmer.jackson.v2;

import com.fasterxml.jackson.databind.ObjectWriter;

/**
 * @deprecated 请使用 {@link org.babyfish.jimmer.json.jackson.v2.JsonWriterV2}。
 */
@Deprecated
public class JsonWriterV2 extends org.babyfish.jimmer.json.jackson.v2.JsonWriterV2 {

    public JsonWriterV2(ObjectWriter objectWriter) {
        super(objectWriter);
    }
}
