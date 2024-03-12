package org.babyfish.jimmer.client.meta.impl;

import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.cfg.ContextAttributes;
import org.babyfish.jimmer.client.meta.Schema;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.Collection;
import java.util.Set;

public class Schemas {

    public static final Object IGNORE_DEFINITIONS = new Object();

    private static final Object GROUPS = new Object();

    private static final ObjectMapper READ_SERVICES_MAPPER =
            new ObjectMapper()
                    .setDefaultAttributes(
                            ContextAttributes.getEmpty().withSharedAttribute(IGNORE_DEFINITIONS, true)
                    );

    private static final ObjectWriter WRITER = new ObjectMapper().writerWithDefaultPrettyPrinter();

    private Schemas() {}

    public static void writeTo(Schema schema, Writer writer) throws IOException {
        WRITER.writeValue(writer, schema);
    }

    public static Schema readFrom(Reader reader) throws IOException {
        return readFrom(reader, null);
    }

    public static Schema readFrom(Reader reader, Set<String> groups) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        if (groups != null && !groups.isEmpty()) {
            mapper.setDefaultAttributes(
                ContextAttributes.getEmpty().withSharedAttribute(GROUPS, groups)
            );
        }
        return mapper.readValue(reader, SchemaImpl.class);
    }

    public static Schema readServicesFrom(Reader reader) throws IOException {
        return READ_SERVICES_MAPPER.readValue(reader, SchemaImpl.class);
    }

    @SuppressWarnings("unchecked")
    static boolean isAllowed(DeserializationContext ctx, Collection<String> elementGroups) {
        if (elementGroups == null) {
            return true;
        }
        Set<String> allowedGroups = (Set<String>)ctx.getAttribute(GROUPS);
        if (allowedGroups == null) {
            return true;
        }
        for (String elementGroup : elementGroups) {
            if (allowedGroups.contains(elementGroup)) {
                return true;
            }
        }
        return false;
    }
}
