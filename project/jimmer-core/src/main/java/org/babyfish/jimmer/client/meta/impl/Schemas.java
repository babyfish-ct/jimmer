package org.babyfish.jimmer.client.meta.impl;

import org.babyfish.jimmer.client.meta.Schema;
import org.babyfish.jimmer.json.codec.JsonCodec;
import org.babyfish.jimmer.json.codec.SharedAttributesCustomization;

import java.io.Reader;
import java.io.Writer;
import java.util.Collection;
import java.util.Set;

import static java.util.Collections.singletonMap;
import static org.babyfish.jimmer.json.codec.JsonCodec.jsonCodec;

public class Schemas {

    public static final Object IGNORE_DEFINITIONS = new Object();

    public static final Object GROUPS = new Object();

    private Schemas() {
    }

    public static void writeTo(Schema schema, Writer writer) throws Exception {
        writeTo(schema, writer, jsonCodec());
    }

    public static void writeTo(Schema schema, Writer writer, JsonCodec jsonCodec) throws Exception {
        jsonCodec.writer().withDefaultPrettyPrinter().write(writer, schema);
    }

    public static Schema readFrom(Reader reader) throws Exception {
        return readFrom(reader, null);
    }

    public static Schema readFrom(Reader reader, Set<String> groups) throws Exception {
        return readFrom(reader, groups, jsonCodec());
    }

    public static Schema readFrom(Reader reader, Set<String> groups, JsonCodec jsonCodec) throws Exception {
        return jsonCodec
                .withCustomizations(new SharedAttributesCustomization(singletonMap(GROUPS, groups)))
                .readerFor(SchemaImpl.class)
                .read(reader);
    }

    public static Schema readServicesFrom(Reader reader) throws Exception {
        return readServicesFrom(reader, jsonCodec());
    }

    public static Schema readServicesFrom(Reader reader, JsonCodec jsonCodec) throws Exception {
        return jsonCodec
                .withCustomizations(new SharedAttributesCustomization(singletonMap(IGNORE_DEFINITIONS, true)))
                .readerFor(SchemaImpl.class)
                .read(reader);
    }

    public static boolean isAllowed(Collection<String> elementGroups, Set<String> allowedGroups) {
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
