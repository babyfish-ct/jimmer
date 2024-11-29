package org.babyfish.jimmer.sql.model.hr;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;

public class MagicStringSerializer extends StdSerializer<String> {

    protected MagicStringSerializer() {
        super(String.class);
    }

    @Override
    public void serialize(String value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        StringBuilder builder = new StringBuilder();
        int size = value.length();
        for (int i = 0; i < size; i++) {
            builder.append((char)(value.charAt(i) + 1));
        }
        gen.writeString(builder.toString());
    }
}
