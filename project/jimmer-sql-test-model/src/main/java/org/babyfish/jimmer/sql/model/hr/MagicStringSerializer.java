package org.babyfish.jimmer.sql.model.hr;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.jspecify.annotations.NonNull;

import java.io.IOException;

public class MagicStringSerializer extends StdSerializer<String> {

    protected MagicStringSerializer() {
        super(String.class);
    }

    @Override
    public void serialize(String value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        gen.writeString(MagicStringCodec.serialize(value));
    }
}
