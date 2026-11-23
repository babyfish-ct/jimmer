package org.babyfish.jimmer.jackson.v2;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.babyfish.jimmer.json.EnumJson;

import java.io.IOException;

final class EnumScalarV2 {

    static class Serializer extends JsonSerializer<Enum<?>> {

        private final EnumJson mapping;

        Serializer(EnumJson mapping) {
            this.mapping = mapping;
        }

        @Override
        public void serialize(Enum<?> constant, JsonGenerator gen, SerializerProvider ctx) throws IOException {
            Object value = mapping.encode(constant);
            if (value instanceof String) {
                gen.writeString((String) value);
            } else {
                gen.writeNumber(((Number) value).longValue());
            }
        }
    }

    static class Deserializer extends JsonDeserializer<Enum<?>> {

        private final Class<?> type;
        private final EnumJson mapping;

        Deserializer(Class<?> type, EnumJson mapping) {
            this.type = type;
            this.mapping = mapping;
        }

        @Override
        public Enum<?> deserialize(JsonParser parser, DeserializationContext ctx) throws IOException {
            Object value;
            switch (parser.currentToken()) {
                case VALUE_STRING:
                    value = parser.getText();
                    break;
                case VALUE_NUMBER_INT:
                case VALUE_NUMBER_FLOAT:
                    value = parser.getDecimalValue();
                    break;
                default:
                    return ctx.reportInputMismatch(type, "Expected a string or integer for %s", type.getName());
            }
            try {
                return mapping.decode(value);
            } catch (IllegalArgumentException ex) {
                return ctx.reportInputMismatch(type, "%s", ex.getMessage());
            }
        }
    }
}
