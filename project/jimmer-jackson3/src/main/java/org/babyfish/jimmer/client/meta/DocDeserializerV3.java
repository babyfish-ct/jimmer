package org.babyfish.jimmer.client.meta;

import com.fasterxml.jackson.annotation.JsonCreator;
import org.jetbrains.annotations.Nullable;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class DocDeserializerV3 extends tools.jackson.databind.ValueDeserializer<Doc> {

        @Override
        public Doc deserialize(tools.jackson.core.JsonParser jp,
                               tools.jackson.databind.DeserializationContext ctx) {

            String value;
            Map<String, String> parameters;
            String returnValue;
            Map<String, String> properties;

            tools.jackson.databind.JsonNode jsonNode = ctx.readTree(jp);
            if (jsonNode.has("value")) {
                value = jsonNode.get("value").asText();
            } else {
                value = null;
            }
            if (jsonNode.has("parameters")) {
                parameters = Collections.unmodifiableMap(ctx.readTreeAsValue(jsonNode.get("parameters"),
                        ctx.getTypeFactory().constructMapType(LinkedHashMap.class, String.class, String.class)));
            } else {
                parameters = Collections.emptyMap();
            }
            if (jsonNode.has("return")) {
                returnValue = jsonNode.get("return").asText();
            } else {
                returnValue = null;
            }
            if (jsonNode.has("properties")) {
                properties = Collections.unmodifiableMap(ctx.readTreeAsValue(jsonNode.get("properties"),
                        ctx.getTypeFactory().constructMapType(LinkedHashMap.class, String.class, String.class)));
            } else {
                properties = Collections.emptyMap();
            }

            return new Doc(value, parameters, returnValue, properties);
        }
    }
