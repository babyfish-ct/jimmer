package org.babyfish.jimmer.jackson.codec;

import java.io.InputStream;
import java.io.Reader;
import java.util.function.Function;

public class MappingJsonReader<S, T> implements JsonReader<T> {
    private final JsonReader<S> jsonReader;
    private final Function<S, T> mapper;

    public MappingJsonReader(JsonReader<S> jsonReader, Function<S, T> mapper) {
        this.jsonReader = jsonReader;
        this.mapper = mapper;
    }

    @Override
    public T read(String json) throws Exception {
        return mapper.apply(jsonReader.read(json));
    }

    @Override
    public T read(byte[] json) throws Exception {
        return mapper.apply(jsonReader.read(json));
    }

    @Override
    public T read(Reader reader) throws Exception {
        return mapper.apply(jsonReader.read(reader));
    }

    @Override
    public T read(InputStream is) throws Exception {
        return mapper.apply(jsonReader.read(is));
    }
}
