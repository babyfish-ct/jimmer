package org.babyfish.jimmer.client.meta.impl;

import org.babyfish.jimmer.client.meta.Schema;
import org.babyfish.jimmer.jackson.codec.JsonCodec;
import org.babyfish.jimmer.jackson.codec.JsonWriter;
import org.babyfish.jimmer.jackson.codec.SharedAttributesCustomization;

import java.io.Reader;
import java.io.Writer;
import java.util.Collection;
import java.util.Set;

import static java.util.Collections.singletonMap;
import static org.babyfish.jimmer.jackson.codec.JsonCodec.jsonCodec;

public class Schemas {

    public static final Object IGNORE_DEFINITIONS = new Object();

    private static final Object GROUPS = new Object();

    private static final JsonCodec<?> READ_SERVICES_JSON_CODEC = jsonCodec()
            .withCustomizations(new SharedAttributesCustomization(singletonMap(IGNORE_DEFINITIONS, true)));

    private static final JsonWriter WRITER = jsonCodec().writer().withDefaultPrettyPrinter();

    private Schemas() {
    }

    public static void writeTo(Schema schema, Writer writer) throws Exception {
        WRITER.write(writer, schema);
    }

    public static Schema readFrom(Reader reader) throws Exception {
        return readFrom(reader, null);
    }

    public static Schema readFrom(Reader reader, Set<String> groups) throws Exception {
        return jsonCodec()
                .withCustomizations(new SharedAttributesCustomization(singletonMap(GROUPS, groups)))
                .readerFor(SchemaImpl.class)
                .read(reader);
    }

    public static Schema readServicesFrom(Reader reader) throws Exception {
        return READ_SERVICES_JSON_CODEC.readerFor(SchemaImpl.class).read(reader);
    }

    @SuppressWarnings("unchecked")
    static boolean isAllowed(com.fasterxml.jackson.databind.DeserializationContext ctx, Collection<String> elementGroups) {
        return isAllowed(elementGroups, (Set<String>) ctx.getAttribute(GROUPS));
    }

    @SuppressWarnings("unchecked")
    static boolean isAllowed(tools.jackson.databind.DeserializationContext ctx, Collection<String> elementGroups) {
        return isAllowed(elementGroups, (Set<String>) ctx.getAttribute(GROUPS));
    }

    private static boolean isAllowed(Collection<String> elementGroups, Set<String> allowedGroups) {
        if (elementGroups == null || elementGroups.isEmpty()) {
            return true;
        }
        if (allowedGroups == null || allowedGroups.isEmpty()) {
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
