package org.babyfish.jimmer.client.meta.impl;

import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.cfg.ContextAttributes;
import org.babyfish.jimmer.client.meta.Schema;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.Map;
import java.util.TreeMap;

public class Schemas {

    public static final Object IGNORE_DEFINITIONS = new Object();

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final ObjectMapper READ_SERVICES_MAPPER =
            new ObjectMapper()
                    .setDefaultAttributes(
                            ContextAttributes.getEmpty().withSharedAttribute(IGNORE_DEFINITIONS, true)
                    );

    private static final ObjectWriter WRITER = MAPPER.writerWithDefaultPrettyPrinter();

    private Schemas() {}

    public static void writeTo(Schema schema, Writer writer) throws IOException {
        WRITER.writeValue(writer, schema);
    }

    public static Schema readFrom(Reader reader) throws IOException {
        return MAPPER.readValue(reader, SchemaImpl.class);
    }

    public static Schema readServicesFrom(Reader reader) throws IOException {
        return READ_SERVICES_MAPPER.readValue(reader, SchemaImpl.class);
    }
}
