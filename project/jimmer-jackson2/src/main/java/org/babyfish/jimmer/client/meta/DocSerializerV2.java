package org.babyfish.jimmer.client.meta;

import com.fasterxml.jackson.annotation.JsonCreator;
import org.jetbrains.annotations.Nullable;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class DocSerializerV2 extends com.fasterxml.jackson.databind.JsonSerializer<Doc> {

        @Override
        public void serialize(Doc doc,
                              com.fasterxml.jackson.core.JsonGenerator gen,
                              com.fasterxml.jackson.databind.SerializerProvider provider) throws IOException {
            gen.writeStartObject();
            if (doc.getValue() != null) {
                provider.defaultSerializeField("value", doc.getValue(), gen);
            }
            if (!doc.getParameterValueMap().isEmpty()) {
                provider.defaultSerializeField("parameters", doc.getParameterValueMap(), gen);
            }
            if (doc.getReturnValue() != null) {
                provider.defaultSerializeField("return", doc.getReturnValue(), gen);
            }
            if (!doc.getPropertyValueMap().isEmpty()) {
                provider.defaultSerializeField("properties", doc.getPropertyValueMap(), gen);
            }
            gen.writeEndObject();
        }
    }
