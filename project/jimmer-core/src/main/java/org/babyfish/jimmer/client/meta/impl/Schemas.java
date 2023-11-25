package org.babyfish.jimmer.client.meta.impl;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import org.babyfish.jimmer.client.meta.Schema;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;

public class Schemas {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final ObjectWriter WRITER = MAPPER.writerWithDefaultPrettyPrinter();

    private Schemas() {}

    public static void writeTo(Schema schema, Writer writer) throws IOException {
        WRITER.writeValue(writer, schema);
    }

    public static Schema readFrom(Reader reader) throws IOException {
        return MAPPER.readValue(reader, SchemaImpl.class);
    }
}
