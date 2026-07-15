package org.babyfish.jimmer.client.meta;

import com.fasterxml.jackson.annotation.JsonCreator;
import org.jetbrains.annotations.Nullable;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class DocDeserializerV2 extends com.fasterxml.jackson.databind.JsonDeserializer<Doc> {

        private static final com.fasterxml.jackson.databind.JavaType DOC_MAP_TYPE =
                com.fasterxml.jackson.databind.type.MapType.construct(
                        LinkedHashMap.class,
                        null,
                        null,
                        null,
                        com.fasterxml.jackson.databind.type.SimpleType.constructUnsafe(String.class),
                        com.fasterxml.jackson.databind.type.SimpleType.constructUnsafe(String.class)
                );

        @Override
        public Doc deserialize(com.fasterxml.jackson.core.JsonParser jp,
                               com.fasterxml.jackson.databind.DeserializationContext ctx) throws IOException {

            String value;
            Map<String, String> parameters;
            String returnValue;
            Map<String, String> properties;

            com.fasterxml.jackson.databind.JsonNode jsonNode = jp.getCodec().readTree(jp);
            if (jsonNode.has("value")) {
                value = jsonNode.get("value").asText();
            } else {
                value = null;
            }
            if (jsonNode.has("parameters")) {
                parameters = Collections.unmodifiableMap(ctx.readTreeAsValue(jsonNode.get("parameters"), DOC_MAP_TYPE));
            } else {
                parameters = Collections.emptyMap();
            }
            if (jsonNode.has("return")) {
                returnValue = jsonNode.get("return").asText();
            } else {
                returnValue = null;
            }
            if (jsonNode.has("properties")) {
                properties = Collections.unmodifiableMap(ctx.readTreeAsValue(jsonNode.get("properties"), DOC_MAP_TYPE));
            } else {
                properties = Collections.emptyMap();
            }

            return new Doc(value, parameters, returnValue, properties);
        }
    }
