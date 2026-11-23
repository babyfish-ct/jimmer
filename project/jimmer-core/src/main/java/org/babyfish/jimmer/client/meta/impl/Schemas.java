package org.babyfish.jimmer.client.meta.impl;

import org.babyfish.jimmer.client.meta.Schema;
import org.babyfish.jimmer.json.JsonCodec;
import org.babyfish.jimmer.json.Node;

import java.io.FilterReader;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.Set;

/** The on-disk format of META-INF/jimmer/client is independent of application JSON settings. */
public class Schemas {

    /** @deprecated Use {@link #readServicesFrom(Reader)}. */
    @Deprecated
    public static final Object IGNORE_DEFINITIONS = new Object();

    private Schemas() {}

    public static void writeTo(Schema schema, Writer writer) throws IOException {
        writeTo(schema, writer, JsonCodec.jsonCodec());
    }

    public static void writeTo(Schema schema, Writer writer, JsonCodec codec) throws IOException {
        try {
            codec.writeTree(writer, SchemaWriter.schema(schema));
            writer.flush();
        } catch (IOException | RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IOException("Cannot write Jimmer client schema", ex);
        }
    }

    public static Schema readFrom(Reader reader) throws IOException {
        return readFrom(reader, null, JsonCodec.jsonCodec());
    }

    public static Schema readFrom(Reader reader, Set<String> groups) throws IOException {
        return readFrom(reader, groups, JsonCodec.jsonCodec());
    }

    public static Schema readFrom(Reader reader, Set<String> groups, JsonCodec codec) throws IOException {
        return new SchemaReader(groups).schema(readTree(reader, codec), false);
    }

    public static Schema readServicesFrom(Reader reader) throws IOException {
        return readServicesFrom(reader, JsonCodec.jsonCodec());
    }

    public static Schema readServicesFrom(Reader reader, JsonCodec codec) throws IOException {
        return new SchemaReader(null).schema(readTree(reader, codec), true);
    }

    private static Node readTree(Reader reader, JsonCodec codec) throws IOException {
        // Some engines close their input after the end of a JSON document.
        Reader source = new FilterReader(reader) {
            @Override
            public void close() {}
        };
        try {
            return codec.treeReader().read(source);
        } catch (IOException | RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IOException("Cannot read Jimmer client schema", ex);
        }
    }
}
