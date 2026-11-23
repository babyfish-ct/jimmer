package org.babyfish.jimmer.json;

import java.io.OutputStream;
import java.io.Writer;

public interface JsonWriter<T> {
    JsonWriter<T> withDefaultPrettyPrinter();

    String writeAsString(T obj) throws Exception;

    byte[] writeAsBytes(T obj) throws Exception;

    void write(Writer writer, T obj) throws Exception;

    void write(OutputStream os, T obj) throws Exception;
}
