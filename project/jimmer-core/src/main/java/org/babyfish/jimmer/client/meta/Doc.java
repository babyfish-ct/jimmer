package org.babyfish.jimmer.client.meta;

import com.fasterxml.jackson.annotation.JsonCreator;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

@com.fasterxml.jackson.databind.annotation.JsonSerialize(using = Doc.SerializerV2.class)
@com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = Doc.DeserializerV2.class)
@tools.jackson.databind.annotation.JsonSerialize(using = Doc.SerializerV3.class)
@tools.jackson.databind.annotation.JsonDeserialize(using = Doc.DeserializerV3.class)
public class Doc {

    private final String value;

    private final Map<String, String> parameterValueMap;

    private final String returnValue;

    private final Map<String, String> propertyValueMap;

    private String toString;

    @JsonCreator
    public Doc(
            String value,
            Map<String, String> parameterValueMap,
            String returnValue,
            Map<String, String> propertyValueMap
    ) {
        this.value = value;
        if (parameterValueMap == null || parameterValueMap.isEmpty()) {
            this.parameterValueMap = Collections.emptyMap();
        } else {
            this.parameterValueMap = Collections.unmodifiableMap(parameterValueMap);
        }
        this.returnValue = returnValue;
        if (propertyValueMap == null || propertyValueMap.isEmpty()) {
            this.propertyValueMap = Collections.emptyMap();
        } else {
            this.propertyValueMap = Collections.unmodifiableMap(propertyValueMap);
        }
    }

    public String getValue() {
        return value;
    }

    public Map<String, String> getParameterValueMap() {
        return parameterValueMap;
    }

    public String getReturnValue() {
        return returnValue;
    }

    public Map<String, String> getPropertyValueMap() {
        return propertyValueMap;
    }

    @Override
    public String toString() {
        String str = toString;
        if (str == null) {
            this.toString = str = toStringImpl();
        }
        return str;
    }

    private String toStringImpl() {
        StringBuilder builder = new StringBuilder();
        if (value != null) {
            builder.append(value).append('\n');
        }
        for (Map.Entry<String, String> e : parameterValueMap.entrySet()) {
            builder.append("@param ").append(e.getKey()).append(' ').append(e.getValue()).append('\n');
        }
        for (Map.Entry<String, String> e : propertyValueMap.entrySet()) {
            builder.append("@property ").append(e.getKey()).append(' ').append(e.getValue()).append('\n');
        }
        if (returnValue != null) {
            builder.append("@return ").append(returnValue).append('\n');
        }
        return builder.toString();
    }

    public static Doc parse(String doc) {
        if (doc != null) {
            doc = doc.trim(); // KSP does not trim the documentation
        }
        if (doc == null || doc.isEmpty()) {
            return null;
        }

        Builder builder = new Builder();
        try (BufferedReader reader = new BufferedReader(new StringReader(doc))) {
            String line;
            while ((line = reader.readLine()) != null) {
                int start = indexOfNonWhiteSpace(line, 0);
                if (start == -1) {
                    builder.append(line);
                    continue;
                }
                if (line.startsWith("@param", start) &&
                        line.length() > 6 &&
                        Character.isWhitespace(line.charAt(start + 6))) {
                    int begin = indexOfNonWhiteSpace(line, start + 6);
                    if (begin != -1) {
                        int end = indexOfWhiteSpace(line, begin + 1);
                        if (end == -1) {
                            builder.switchToParam(line.substring(begin));
                        } else {
                            builder.switchToParam(line.substring(begin, end));
                            int rest = indexOfNonWhiteSpace(line, end);
                            if (rest != -1) {
                                builder.append(line.substring(rest));
                            } else {
                                builder.append(line.substring(end));
                            }
                        }
                    } else {
                        int rest = indexOfNonWhiteSpace(line, start + 6);
                        if (rest != -1) {
                            builder.append(line.substring(rest));
                        } else {
                            builder.append(line.substring(start + 6));
                        }
                    }
                } else if (line.startsWith("@property", start) &&
                        line.length() > 9 &&
                        Character.isWhitespace(line.charAt(start + 9))
                ) {
                    int begin = indexOfNonWhiteSpace(line, start + 9);
                    if (begin != -1) {
                        int end = indexOfWhiteSpace(line, begin + 1);
                        if (end == -1) {
                            builder.switchToProperty(line.substring(begin));
                        } else {
                            builder.switchToProperty(line.substring(begin, end));
                            int rest = indexOfNonWhiteSpace(line, end);
                            if (rest != -1) {
                                builder.append(line.substring(rest));
                            } else {
                                builder.append(line.substring(end));
                            }
                        }
                    } else {
                        builder.append(line.substring(start + 9));
                    }
                } else if (line.startsWith("@return", start)) {
                    int begin = indexOfNonWhiteSpace(line, start + 7);
                    builder.switchToReturn();
                    if (begin != -1) {
                        builder.append(line.substring(begin));
                    } else {
                        builder.append(line.substring(start + 7));
                    }
                } else if (line.startsWith("@", start)) {
                    builder.switchToIgnored();
                } else {
                    if (line.charAt(0) <= ' ') {
                        builder.append(line.substring(1));
                    } else {
                        builder.append(line);
                    }
                }
            }
        } catch (IOException ex) {
            throw new AssertionError("Cannot parse documentation comment");
        }
        return builder.build();
    }

    public static String valueOf(@Nullable Doc doc) {
        return doc != null ? doc.getValue() : null;
    }

    public static String returnOf(@Nullable Doc doc) {
        return doc != null ? doc.getReturnValue() : null;
    }

    public static String paramOf(@Nullable Doc doc, String param) {
        return doc != null ? doc.getParameterValueMap().get(param) : null;
    }

    public static String propertyOf(@Nullable Doc doc, String property) {
        return doc != null ? doc.getPropertyValueMap().get(property) : null;
    }

    private static int indexOfNonWhiteSpace(String line, int start) {
        int size = line.length();
        for (int i = start; i < size; i++) {
            if (line.charAt(i) > ' ') {
                return i;
            }
        }
        return -1;
    }

    private static int indexOfWhiteSpace(String line, int start) {
        int size = line.length();
        for (int i = start; i < size; i++) {
            if (line.charAt(i) <= ' ') {
                return i;
            }
        }
        return -1;
    }

    private static class Builder {

        private String value;

        private final Map<String, String> parameterValueMap = new LinkedHashMap<>();

        private final Map<String, String> propertyValueMap = new LinkedHashMap<>();

        private String returnValue;

        private String currentParamName;

        private String currentPropertyName;

        private boolean currentReturn;

        private boolean currentIgnored;

        private StringBuilder sb = new StringBuilder();

        public void switchToParam(String name) {
            commit();
            this.currentParamName = name != null ? name : "<unknown>";
        }

        public void switchToProperty(String name) {
            commit();
            this.currentPropertyName = name != null ? name : "<unknown>";
        }

        public void switchToReturn() {
            commit();
            currentReturn = true;
        }

        public void switchToIgnored() {
            commit();
            currentIgnored = true;
        }

        public void append(String text) {
            if (!currentIgnored) {
                sb.append(text).append('\n');
            }
        }

        public Doc build() {
            commit();
            return new Doc(
                    value != null && !value.isEmpty() ? value : null,
                    parameterValueMap,
                    returnValue != null && !returnValue.isEmpty() ? returnValue : null,
                    propertyValueMap
            );
        }

        private void commit() {
            if (sb.length() != 0 && sb.charAt(sb.length() - 1) == '\n') {
                sb.setLength(sb.length() - 1);
            }
            if (currentParamName != null) {
                parameterValueMap.put(currentParamName, sb.toString());
                currentParamName = null;
            } else if (currentPropertyName != null) {
                propertyValueMap.put(currentPropertyName, sb.toString());
                currentPropertyName = null;
            } else if (currentReturn) {
                returnValue = sb.toString();
                currentReturn = false;
            } else if (!currentIgnored) {
                value = sb.toString();
            }
            sb = new StringBuilder();
        }
    }

    static class SerializerV2 extends com.fasterxml.jackson.databind.JsonSerializer<Doc> {

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

    static class DeserializerV2 extends com.fasterxml.jackson.databind.JsonDeserializer<Doc> {

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

    static class SerializerV3 extends tools.jackson.databind.ValueSerializer<Doc> {

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

    static class DeserializerV3 extends tools.jackson.databind.ValueDeserializer<Doc> {

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
}