package org.babyfish.jimmer.support;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import org.babyfish.jimmer.ImmutableObjects;
import org.babyfish.jimmer.jackson.v2.ImmutableModuleV2;
import org.babyfish.jimmer.jackson.v2.JsonCodecV2;
import org.babyfish.jimmer.jackson.v3.ImmutableModuleV3;
import org.babyfish.jimmer.jackson.v3.JsonCodecV3;
import org.babyfish.jimmer.json.JsonCodec;
import org.junit.jupiter.api.Assertions;

public enum JacksonBackend {
    JACKSON2(new JsonCodecV2()),
    JACKSON3(new JsonCodecV3());

    public enum Naming {
        LOWER_CAMEL_CASE(PropertyNamingStrategies.LOWER_CAMEL_CASE, tools.jackson.databind.PropertyNamingStrategies.LOWER_CAMEL_CASE),
        UPPER_CAMEL_CASE(PropertyNamingStrategies.UPPER_CAMEL_CASE, tools.jackson.databind.PropertyNamingStrategies.UPPER_CAMEL_CASE),
        LOWER_CASE(PropertyNamingStrategies.LOWER_CASE, tools.jackson.databind.PropertyNamingStrategies.LOWER_CASE),
        SNAKE_CASE(PropertyNamingStrategies.SNAKE_CASE, tools.jackson.databind.PropertyNamingStrategies.SNAKE_CASE),
        KEBAB_CASE(PropertyNamingStrategies.KEBAB_CASE, tools.jackson.databind.PropertyNamingStrategies.KEBAB_CASE),
        LOWER_DOT_CASE(PropertyNamingStrategies.LOWER_DOT_CASE, tools.jackson.databind.PropertyNamingStrategies.LOWER_DOT_CASE);

        private final PropertyNamingStrategy jackson2;
        private final tools.jackson.databind.PropertyNamingStrategy jackson3;

        Naming(PropertyNamingStrategy jackson2, tools.jackson.databind.PropertyNamingStrategy jackson3) {
            this.jackson2 = jackson2;
            this.jackson3 = jackson3;
        }
    }

    public final JsonCodec codec;

    JacksonBackend(JsonCodec codec) {
        this.codec = codec;
    }

    public String write(Object immutable) {
        String json = ImmutableObjects.toString(immutable, codec);
        Assertions.assertEquals(immutable.toString(), json);
        return json;
    }

    public <T> T read(Class<T> type, String json) {
        T result = ImmutableObjects.fromString(type, json, codec);
        Assertions.assertEquals(ImmutableObjects.fromString(type, json).toString(), result.toString());
        return result;
    }

    public JsonCodec rawCodec() {
        if (this == JACKSON2) {
            return new JsonCodecV2(JsonMapper.builder().build());
        }
        return new JsonCodecV3(tools.jackson.databind.json.JsonMapper.builder().build());
    }

    public JsonCodec naming(Naming naming) {
        if (this == JACKSON2) {
            return new JsonCodecV2(JsonMapper.builder().addModule(new ImmutableModuleV2())
                    .propertyNamingStrategy(naming.jackson2).build());
        }
        return new JsonCodecV3(tools.jackson.databind.json.JsonMapper.builder().addModule(new ImmutableModuleV3())
                .propertyNamingStrategy(naming.jackson3).build());
    }

    public JsonCodec polymorphicCodec() {
        if (this == JACKSON2) {
            return new JsonCodecV2(JsonMapper.builder().addModule(new ImmutableModuleV2())
                    .activateDefaultTyping(BasicPolymorphicTypeValidator.builder()
                                    .allowIfSubType("org.babyfish.jimmer.")
                                    .allowIfSubType("java.util.").build(),
                            ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY)
                    .build());
        }
        return new JsonCodecV3(tools.jackson.databind.json.JsonMapper.builder().addModule(new ImmutableModuleV3())
                .activateDefaultTyping(tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator.builder()
                                .allowIfSubType("org.babyfish.jimmer.")
                                .allowIfSubType("java.util.").build(),
                        tools.jackson.databind.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY)
                .build());
    }
}
