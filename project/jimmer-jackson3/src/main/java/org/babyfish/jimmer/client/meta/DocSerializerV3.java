package org.babyfish.jimmer.client.meta;

import com.fasterxml.jackson.annotation.JsonCreator;
import org.jetbrains.annotations.Nullable;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class DocSerializerV3 extends tools.jackson.databind.ValueSerializer<Doc> {

        @Override
        public void serialize(Doc doc,
                              tools.jackson.core.JsonGenerator gen,
                              tools.jackson.databind.SerializationContext ctx) {
            gen.writeStartObject();
            if (doc.getValue() != null) {
                ctx.defaultSerializeProperty("value", doc.getValue(), gen);
            }
            if (!doc.getParameterValueMap().isEmpty()) {
                ctx.defaultSerializeProperty("parameters", doc.getParameterValueMap(), gen);
            }
            if (doc.getReturnValue() != null) {
                ctx.defaultSerializeProperty("return", doc.getReturnValue(), gen);
            }
            if (!doc.getPropertyValueMap().isEmpty()) {
                ctx.defaultSerializeProperty("properties", doc.getPropertyValueMap(), gen);
            }
            gen.writeEndObject();
        }
    }
