package org.babyfish.jimmer.jackson.v3;

import tools.jackson.databind.ObjectWriter;

/**
 * @deprecated 请使用 {@link org.babyfish.jimmer.json.jackson.v3.JsonWriterV3}。
 */
@Deprecated
public class JsonWriterV3 extends org.babyfish.jimmer.json.jackson.v3.JsonWriterV3 {

    public JsonWriterV3(ObjectWriter objectWriter) {
        super(objectWriter);
    }
}
