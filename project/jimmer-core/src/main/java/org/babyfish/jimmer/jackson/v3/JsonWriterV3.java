package org.babyfish.jimmer.jackson.v3;

import org.babyfish.jimmer.jackson.codec.JsonWriter;
import tools.jackson.databind.ObjectWriter;

import java.io.OutputStream;
import java.io.Writer;

public class JsonWriterV3 implements JsonWriter {
    private final ObjectWriter objectWriter;

    public JsonWriterV3(ObjectWriter objectWriter) {
        this.objectWriter = objectWriter;
    }

    @Override
    public JsonWriter withDefaultPrettyPrinter() {
        return new JsonWriterV3(objectWriter.withDefaultPrettyPrinter());
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
