package org.babyfish.jimmer.sql.dialect;

import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.kotlin.KotlinModule;
import org.babyfish.jimmer.jackson.ImmutableModule;

class JsonUtils {

    public static final ObjectMapper OBJECT_MAPPER;

    private JsonUtils() {}

    static {
        ObjectMapper mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .registerModule(new ImmutableModule());
        boolean hasKotlinModule;
        try {
            Class.forName("com.fasterxml.jackson.module.kotlin.KotlinModule");
            hasKotlinModule = true;
        } catch (ClassNotFoundException ex) {
            hasKotlinModule = false;
        }
        if (hasKotlinModule) {
            mapper = KotlinModuleRegister.register(mapper);
        }
        OBJECT_MAPPER = mapper;
    }

    private static class KotlinModuleRegister {
        public static ObjectMapper register(ObjectMapper mapper) {
            return mapper.registerModule(new KotlinModule.Builder().build());
        }
    }
}
