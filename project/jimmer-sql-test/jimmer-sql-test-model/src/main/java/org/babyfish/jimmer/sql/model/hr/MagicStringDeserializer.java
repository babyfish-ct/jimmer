package org.babyfish.jimmer.sql.model.hr;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;

public class MagicStringDeserializer extends StdDeserializer<String> {

    public MagicStringDeserializer() {
        super(String.class);
    }

    @Override
    public String deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        return MagicStringCodec.deserialize(p.readValueAs(String.class));
    }
}
