package org.babyfish.jimmer.sql.model.hr;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ser.std.StdSerializer;

public class MagicStringSerializer3 extends StdSerializer<String> {

    protected MagicStringSerializer3() {
        super(String.class);
    }

    @Override
    public void serialize(String value, JsonGenerator gen, SerializationContext provider) throws JacksonException {
        gen.writeString(MagicStringCodec.serialize(value));
    }
}
