package org.babyfish.jimmer.jackson.codec;

import java.io.OutputStream;
import java.io.Writer;

public interface JsonWriter {
    JsonWriter withDefaultPrettyPrinter();

    String writeAsString(Object obj) throws Exception;

    byte[] writeAsBytes(Object obj) throws Exception;

    void write(Writer writer, Object obj) throws Exception;

    void write(OutputStream os, Object obj) throws Exception;
}