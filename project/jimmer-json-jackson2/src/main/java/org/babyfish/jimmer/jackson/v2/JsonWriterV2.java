package org.babyfish.jimmer.jackson.v2;

import com.fasterxml.jackson.databind.ObjectWriter;
import org.babyfish.jimmer.json.JsonWriter;

import java.io.OutputStream;
import java.io.Writer;

public class JsonWriterV2<T> implements JsonWriter<T> {
    private final ObjectWriter objectWriter;

    public JsonWriterV2(ObjectWriter objectWriter) {
        this.objectWriter = objectWriter.without(com.fasterxml.jackson.core.StreamWriteFeature.AUTO_CLOSE_TARGET.mappedFeature());
    }

    @Override
    public JsonWriter<T> withDefaultPrettyPrinter() {
        return new JsonWriterV2<>(objectWriter.withDefaultPrettyPrinter());
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
