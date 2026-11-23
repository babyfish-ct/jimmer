package org.babyfish.jimmer.jackson.v3;

import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.SerializationContext;
import org.babyfish.jimmer.json.EnumJson;

final class EnumScalarV3 {

    static class Serializer extends ValueSerializer<Enum<?>> {

        private final EnumJson mapping;

        Serializer(EnumJson mapping) {
            this.mapping = mapping;
        }

        @Override
        public void serialize(Enum<?> constant, JsonGenerator gen, SerializationContext ctx) {
            Object value = mapping.encode(constant);
            if (value instanceof String) {
                gen.writeString((String) value);
            } else {
                gen.writeNumber(((Number) value).longValue());
            }
        }
    }

    static class Deserializer extends ValueDeserializer<Enum<?>> {

        private final Class<?> type;
        private final EnumJson mapping;

        Deserializer(Class<?> type, EnumJson mapping) {
            this.type = type;
            this.mapping = mapping;
        }

        @Override
        public Enum<?> deserialize(JsonParser parser, DeserializationContext ctx) {
            Object value;
            switch (parser.currentToken()) {
                case VALUE_STRING:
                    value = parser.getString();
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
