package org.babyfish.jimmer.client.meta;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.type.MapType;
import com.fasterxml.jackson.databind.type.SimpleType;
import org.jetbrains.annotations.Nullable;

import javax.lang.model.element.TypeElement;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.*;

@JsonSerialize(using = Doc.Serializer.class)
@JsonDeserialize(using = Doc.Deserializer.class)
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
                if (line.startsWith("@param", start) && Character.isWhitespace(line.charAt(start + 6))) {
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
                } else if (line.startsWith("@property", start) && Character.isWhitespace(line.charAt(start + 9))) {
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

    public static class Serializer extends JsonSerializer<Doc> {

        @Override
        public void serialize(Doc doc, JsonGenerator gen, SerializerProvider provider) throws IOException {
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

    public static class Deserializer extends JsonDeserializer<Doc> {

        private static final JavaType DOC_MAP_TYPE = MapType.construct(
                LinkedHashMap.class,
                null,
                null,
                null,
                SimpleType.constructUnsafe(String.class),
                SimpleType.constructUnsafe(String.class)
        );

        @Override
        public Doc deserialize(JsonParser jp, DeserializationContext ctx) throws IOException, JacksonException {

            String value;
            Map<String, String> parameters;
            String returnValue;
            Map<String, String> properties;

            JsonNode jsonNode = jp.getCodec().readTree(jp);
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
}
