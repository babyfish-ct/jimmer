package org.babyfish.jimmer.jackson.codec;

import java.io.InputStream;
import java.io.Reader;

public interface JsonReader<T> {
    T read(String json) throws Exception;

    T read(byte[] json) throws Exception;

    T read(Reader reader) throws Exception;

    T read(InputStream is) throws Exception;
}