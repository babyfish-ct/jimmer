package org.babyfish.jimmer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.babyfish.jimmer.jackson.ImmutableModule;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.TargetLevel;
import org.babyfish.jimmer.meta.TypedProp;
import org.babyfish.jimmer.runtime.DraftSpi;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.runtime.Internal;
import org.babyfish.jimmer.sql.meta.Column;

public class ImmutableObjects {

    private static final ObjectMapper MAPPER;

    private ImmutableObjects() {}

    /**
     * Jimmer object is dynamic, none properties are mandatory.
     *
     * This method can ask whether a property of the object is specified.
     *
     * @param immutable Object instance
     * @param prop Property id
     * @return Whether the property of the object is specified.
     * @exception IllegalArgumentException The first argument is immutable object created by jimmer
     */
    public static boolean isLoaded(Object immutable, int prop) {
        if (immutable instanceof ImmutableSpi) {
            return ((ImmutableSpi) immutable).__isLoaded(prop);
        }
        throw new IllegalArgumentException("The first argument is immutable object created by jimmer");
    }

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
     * Jimmer object is dynamic, none properties are mandatory.
     *
     * This method can ask whether a property of the object is specified.
     *
     * @param immutable Object instance
     * @param prop Property
     * @return Whether the property of the object is specified.
     * @exception IllegalArgumentException The first argument is immutable object created by jimmer
     */
    public static boolean isLoaded(Object immutable, ImmutableProp prop) {
        return isLoaded(immutable, prop.getId());
    }

    /**
     * Jimmer object is dynamic, none properties are mandatory.
     *
     * This method can ask whether a property of the object is specified.
     *
     * @param immutable Object instance
     * @param prop Property
     * @return Whether the property of the object is specified.
     * @exception IllegalArgumentException The first argument is immutable object created by jimmer
     */
    public static boolean isLoaded(Object immutable, TypedProp<?, ?> prop) {
        return isLoaded(immutable, prop.unwrap().getId());
    }

    /**
     * Get the property value of immutable object,
     * if the property is not loaded, exception will be thrown.
     *
     * @param immutable Object instance
     * @param prop Property id
     * @return Whether the property of the object is specified.
     * @exception IllegalArgumentException There are two possibilities
     *      <ul>
     *          <li>The first argument is immutable object created by jimmer</li>
     *          <li>The second argument is not a valid property name of immutable object</li>
     *      </ul>
     * @exception UnloadedException The property is not loaded
     */
    public static Object get(Object immutable, int prop) {
        if (immutable instanceof ImmutableSpi) {
            return ((ImmutableSpi) immutable).__get(prop);
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
     * Get the property value of immutable object,
     * if the property is not loaded, exception will be thrown.
     *
     * @param immutable Object instance
     * @param prop Property
     * @return Whether the property of the object is specified.
     * @exception IllegalArgumentException There are two possibilities
     *      <ul>
     *          <li>The first argument is immutable object created by jimmer</li>
     *          <li>The second argument is not a valid property name of immutable object</li>
     *      </ul>
     * @exception UnloadedException The property is not loaded
     */
    public static Object get(Object immutable, ImmutableProp prop) {
        return get(immutable, prop.getId());
    }

    /**
     * Get the property value of immutable object,
     * if the property is not loaded, exception will be thrown.
     *
     * @param <T> The entity type
     * @param <X> The property type
     *
     * @param immutable Object instance
     * @param prop Property
     * @return Whether the property of the object is specified.
     * @exception IllegalArgumentException There are two possibilities
     *      <ul>
     *          <li>The first argument is immutable object created by jimmer</li>
     *          <li>The second argument is not a valid property name of immutable object</li>
     *      </ul>
     * @exception UnloadedException The property is not loaded
     */
    @SuppressWarnings("unchecked")
    public static <T, X> X get(T immutable, TypedProp<T, X> prop) {
        return (X)get(immutable, prop.unwrap().getId());
    }

    public static boolean isIdOnly(Object immutable) {
        if (immutable == null) {
            return false;
        }
        if (immutable instanceof ImmutableSpi) {
            ImmutableSpi spi = (ImmutableSpi) immutable;
            ImmutableType type = spi.__type();
            ImmutableProp idProp = type.getIdProp();
            if (idProp == null) {
                throw new IllegalArgumentException("The object type \"" + type + "\" does not have id property");
            }
            for (ImmutableProp prop : type.getProps().values()) {
                if (prop.isId()) {
                    if (!spi.__isLoaded(prop.getId())) {
                        throw new IllegalArgumentException("The id of " + spi + " is unloaded");
                    }
                } else if (spi.__isLoaded(prop.getId())) {
                    return false;
                }
            }
            return true;
        }
        throw new IllegalArgumentException("The first argument is immutable object created by jimmer");
    }

    public static <T> T makeIdOnly(Class<T> type, Object id) {
        return makeIdOnly(ImmutableType.get(type), id);
    }

    @SuppressWarnings("unchecked")
    public static <T> T makeIdOnly(ImmutableType type, Object id) {
        ImmutableProp idProp = type.getIdProp();
        if (idProp == null) {
            throw new IllegalArgumentException("No id property in \"" + type + "\"");
        }
        if (id == null) {
            return null;
        }
        return (T) Internal.produce(type, null, draft -> {
            DraftSpi targetDraft = (DraftSpi) draft;
            targetDraft.__set(idProp.getId(), id);
        });
    }

    public static boolean isLonely(Object immutable) {
        if (immutable instanceof ImmutableSpi) {
            ImmutableSpi spi = (ImmutableSpi) immutable;
            ImmutableType type = spi.__type();
            for (ImmutableProp prop : type.getProps().values()) {
                if (prop.isAssociation(TargetLevel.ENTITY) && spi.__isLoaded(prop.getId())) {
                    if (prop.getStorage() instanceof Column) {
                        ImmutableSpi target = (ImmutableSpi) spi.__get(prop.getId());
                        if (target != null && !isIdOnly(target)) {
                            return false;
                        }
                    } else {
                        return false;
                    }
                }
            }
        }
        throw new IllegalArgumentException("The first argument is immutable object created by jimmer");
    }

    @SuppressWarnings("unchecked")
    public static <T> T toLonely(T immutable) {
        if (immutable == null) {
            return null;
        }
        ImmutableSpi spi = (ImmutableSpi) immutable;
        ImmutableType type = spi.__type();
        return (T)Internal.produce(type, immutable, draft -> {
            for (ImmutableProp prop : type.getProps().values()) {
                int propId = prop.getId();
                if (prop.isAssociation(TargetLevel.ENTITY) && spi.__isLoaded(propId)) {
                    if (prop.getStorage() instanceof Column) {
                        ImmutableSpi target = (ImmutableSpi) spi.__get(propId);
                        if (target != null) {
                            ImmutableType targetType = prop.getTargetType();
                            int targetIdPropId = targetType.getIdProp().getId();
                            if (!target.__isLoaded(targetIdPropId)) {
                                ((DraftSpi) draft).__unload(propId);
                            } else if (!isIdOnly(target)) {
                                Object targetId = target.__get(targetIdPropId);
                                ((DraftSpi) draft).__set(propId, makeIdOnly(targetType, targetId));
                            }
                        }
                    } else {
                        ((DraftSpi) draft).__unload(propId);
                    }
                }
            }
        });
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
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        MAPPER = mapper;
    }
}
