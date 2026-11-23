package org.babyfish.jimmer.json.fastjson2;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.JSONWriter;
import com.alibaba.fastjson2.modules.ObjectReaderModule;
import com.alibaba.fastjson2.modules.ObjectWriterModule;
import com.alibaba.fastjson2.reader.ObjectReader;
import com.alibaba.fastjson2.reader.ObjectReaderProvider;
import com.alibaba.fastjson2.writer.ObjectWriter;
import com.alibaba.fastjson2.writer.ObjectWriterProvider;
import org.babyfish.jimmer.impl.util.Classes;
import org.babyfish.jimmer.json.AbstractJsonCodec;
import org.babyfish.jimmer.json.ModelJson;

import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Type;

public class Fastjson2Codec extends AbstractJsonCodec {

    private final ObjectReaderProvider readers;
    private final ObjectWriterProvider writers;

    public Fastjson2Codec() {
        this(new ObjectReaderProvider(), new ObjectWriterProvider());
    }

    public Fastjson2Codec(ObjectReaderProvider readers, ObjectWriterProvider writers) {
        this.readers = readers;
        this.writers = writers;
        readers.register(new ObjectReaderModule() {
            @Override
            public ObjectReader<?> getObjectReader(Type type) {
                if (!ModelJson.supports(type)) {
                    return null;
                }
                return (reader, fieldType, name, features) -> ModelJson.decode(type, reader.readAny(),
                        (target, value) -> read(write(value, null, false), target));
            }
        });
        writers.register(new ObjectWriterModule() {
            @Override
            public ObjectWriter<?> getObjectWriter(Type type, Class raw) {
                if (!ModelJson.supports(type)) {
                    return null;
                }
                return (writer, value, name, fieldType, features) -> writer.writeAny(ModelJson.encode(type, value,
                        (target, item) -> read(write(item, target, false), Object.class)));
            }
        });
    }

    @Override
    protected Object read(String json, Type type) {
        try (JSONReader reader = JSONReader.of(json, new JSONReader.Context(readers))) {
            Object result = reader.read(type);
            if (!reader.isEnd()) {
                throw new IllegalArgumentException("Unexpected content after JSON value");
            }
            return result;
        }
    }

    @Override
    public void writeTree(Writer target, Object tree) throws IOException {
        target.write(JSON.toJSONString(tree, JSONWriter.Feature.WriteNulls, JSONWriter.Feature.PrettyFormat));
    }

    @Override
    @SuppressWarnings("unchecked")
    protected String write(Object value, Type type, boolean pretty) {
        try (JSONWriter writer = JSONWriter.of(writers, JSONWriter.Feature.WriteNulls)) {
            if (pretty) {
                writer.config(JSONWriter.Feature.PrettyFormat);
            }
            if (value == null) {
                writer.writeNull();
            } else {
                Type target = type != null ? type : value.getClass();
                if (target instanceof Class<?>) {
                    target = Classes.boxTypeOf((Class<?>) target);
                }
                writers.getObjectWriter(target, value.getClass()).write(writer, value, null, target, 0);
            }
            return writer.toString();
        }
    }
}
