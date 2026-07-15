package org.babyfish.jimmer.jackson.v3;

import org.babyfish.jimmer.json.codec.JsonType;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.type.TypeFactory;

class JacksonTypeFactoryV3 {

    private final TypeFactory typeFactory;

    JacksonTypeFactoryV3(TypeFactory typeFactory) {
        this.typeFactory = typeFactory;
    }

    JavaType javaType(JsonType type) {
        return typeFactory.constructType(type.getType());
    }
}
