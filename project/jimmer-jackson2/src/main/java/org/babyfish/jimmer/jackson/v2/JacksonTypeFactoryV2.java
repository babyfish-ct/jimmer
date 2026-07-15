package org.babyfish.jimmer.jackson.v2;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import org.babyfish.jimmer.json.codec.JsonType;

class JacksonTypeFactoryV2 {

    private final TypeFactory typeFactory;

    JacksonTypeFactoryV2(TypeFactory typeFactory) {
        this.typeFactory = typeFactory;
    }

    JavaType javaType(JsonType type) {
        return typeFactory.constructType(type.getType());
    }
}
