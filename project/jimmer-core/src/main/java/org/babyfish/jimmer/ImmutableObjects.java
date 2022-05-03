package org.babyfish.jimmer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.babyfish.jimmer.jackson.ImmutableModule;

public class ImmutableObjects {

    private static final ObjectMapper MAPPER;

    private ImmutableObjects() {}

    public static String toString(Object immutable) {
        try {
            return MAPPER.writeValueAsString(immutable);
        } catch (JsonProcessingException ex) {
            throw new RuntimeException(ex);
        }
    }

    @SuppressWarnings("unchecked")
    public static <I> I fromString(Class<I> type, String json) throws JsonProcessingException {
        return (I)MAPPER.readValue(json, type);
    }

    static {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.registerModule(new ImmutableModule());
        MAPPER = mapper;
    }
}
