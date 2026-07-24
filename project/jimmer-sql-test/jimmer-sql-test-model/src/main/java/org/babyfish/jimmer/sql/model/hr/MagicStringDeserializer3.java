package org.babyfish.jimmer.sql.model.hr;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.deser.std.StdDeserializer;

public class MagicStringDeserializer3 extends StdDeserializer<String> {

    public MagicStringDeserializer3() {
        super(String.class);
    }

    @Override
    public String deserialize(JsonParser p, DeserializationContext ctxt) throws JacksonException {
        return MagicStringCodec.deserialize(p.readValueAs(String.class));
    }
}
