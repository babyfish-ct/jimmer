package org.babyfish.jimmer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.babyfish.jimmer.jackson.ImmutableModule;
import org.babyfish.jimmer.runtime.ImmutableSpi;

public class ImmutableObjects {

    private static final ObjectMapper MAPPER;

    private ImmutableObjects() {}

    /**
     * Jimmer object is dynamic, none properties are mandatory.
     *
     * This method can ask whether a property of the object is specified.
     *
     * @param immutable Object instance
     * @param prop Property name
     * @return Whether the property of the object is specified.
     * @exception IllegalArgumentException The first argument is immutable object created by jimmer
     */
    public static boolean isLoaded(Object immutable, String prop) {
        if (immutable instanceof ImmutableSpi) {
            return ((ImmutableSpi) immutable).__isLoaded(prop);
        }
        throw new IllegalArgumentException("The first argument is immutable object created by jimmer");
    }

    /**
     * Get the property value of immutable object,
     * if the property is not loaded, exception will be thrown.
     *
     * @param immutable Object instance
     * @param prop Property name
     * @return Whether the property of the object is specified.
     * @exception IllegalArgumentException There are two possibilities
     *      <ul>
     *          <li>The first argument is immutable object created by jimmer</li>
     *          <li>The second argument is not a valid property name of immutable object</li>
     *      </ul>
     * @exception UnloadedException The property is not loaded
     */
    public static Object get(Object immutable, String prop) {
        if (immutable instanceof ImmutableSpi) {
            return ((ImmutableSpi) immutable).__get(prop);
        }
        throw new IllegalArgumentException("The first argument is immutable object created by jimmer");
    }

    /**
     * Convert an object to a JSON string.
     * If the object is jimmer immutable object, unspecified properties can be automatically ignored.
     *
     * @param immutable Any object
     * @return JSON string
     */
    public static String toString(Object immutable) {
        try {
            return MAPPER.writeValueAsString(immutable);
        } catch (JsonProcessingException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Convert a JSON string to an object.
     *
     * @param type Object type, can be interface type.
     * @return Deserialized object
     */
    @SuppressWarnings("unchecked")
    public static <I> I fromString(Class<I> type, String json) throws JsonProcessingException {
        return MAPPER.readValue(json, type);
    }

    static {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.registerModule(new ImmutableModule());
        MAPPER = mapper;
    }
}
