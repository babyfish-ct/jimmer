package org.babyfish.jimmer.jackson.v2;

import com.fasterxml.jackson.databind.ObjectWriter;
import org.babyfish.jimmer.jackson.codec.JsonWriter;

import java.io.OutputStream;
import java.io.Writer;

public class JsonWriterV2 implements JsonWriter {
    private final ObjectWriter objectWriter;

    public JsonWriterV2(ObjectWriter objectWriter) {
        this.objectWriter = objectWriter;
    }

    @Override
    public JsonWriter withDefaultPrettyPrinter() {
        return new JsonWriterV2(objectWriter.withDefaultPrettyPrinter());
    }

    @Override
    public String writeAsString(Object obj) throws Exception {
        return objectWriter.writeValueAsString(obj);
    }

    @Override
    public byte[] writeAsBytes(Object obj) throws Exception {
        return objectWriter.writeValueAsBytes(obj);
    }

    @Override
    public void write(Writer writer, Object obj) throws Exception {
        objectWriter.writeValue(writer, obj);
    }

    @Override
    public void write(OutputStream os, Object obj) throws Exception {
        objectWriter.writeValue(os, obj);
    }
}
