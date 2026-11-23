package org.babyfish.jimmer.jackson.v3;

import org.babyfish.jimmer.json.JsonReader;
import tools.jackson.databind.ObjectReader;

import java.io.InputStream;
import java.io.Reader;

public class JsonReaderV3<T> implements JsonReader<T> {
    private final ObjectReader objectReader;

    public JsonReaderV3(ObjectReader objectReader) {
        this.objectReader = objectReader.without(tools.jackson.core.StreamReadFeature.AUTO_CLOSE_SOURCE);
    }

    @Override
    public T read(String json) throws Exception {
        return objectReader.readValue(json);
    }

    @Override
    public T read(byte[] json) throws Exception {
        return objectReader.readValue(json);
    }

    @Override
    public T read(Reader reader) throws Exception {
        return objectReader.readValue(reader);
    }

    @Override
    public T read(InputStream is) throws Exception {
        return objectReader.readValue(is);
    }
}
