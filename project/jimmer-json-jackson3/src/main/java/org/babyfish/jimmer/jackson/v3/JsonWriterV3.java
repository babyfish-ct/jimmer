package org.babyfish.jimmer.jackson.v3;

import org.babyfish.jimmer.json.JsonWriter;
import tools.jackson.databind.ObjectWriter;

import java.io.OutputStream;
import java.io.Writer;

public class JsonWriterV3<T> implements JsonWriter<T> {
    private final ObjectWriter objectWriter;

    public JsonWriterV3(ObjectWriter objectWriter) {
        this.objectWriter = objectWriter.without(tools.jackson.core.StreamWriteFeature.AUTO_CLOSE_TARGET);
    }

    @Override
    public JsonWriter<T> withDefaultPrettyPrinter() {
        return new JsonWriterV3<>(objectWriter.withDefaultPrettyPrinter());
    }

    @Override
    public String writeAsString(T obj) throws Exception {
        return objectWriter.writeValueAsString(obj);
    }

    @Override
    public byte[] writeAsBytes(T obj) throws Exception {
        return objectWriter.writeValueAsBytes(obj);
    }

    @Override
    public void write(Writer writer, T obj) throws Exception {
        objectWriter.writeValue(writer, obj);
    }

    @Override
    public void write(OutputStream os, T obj) throws Exception {
        objectWriter.writeValue(os, obj);
    }
}
