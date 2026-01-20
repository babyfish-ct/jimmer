package org.babyfish.jimmer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.babyfish.jimmer.jackson.ImmutableModule;
import org.babyfish.jimmer.meta.*;
import org.babyfish.jimmer.runtime.DraftSpi;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.runtime.Internal;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class ImmutableObjects {

    private static final ObjectMapper OBJECT_MAPPER;

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
    public static boolean isLoaded(Object immutable, PropId prop) {
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

    @SuppressWarnings("unchecked")
    public static boolean isLoadedChain(Object immutable, PropId ... propIds) {
        ImmutableSpi spi = (ImmutableSpi) immutable;
        int parentCount = propIds.length - 1;
        if (parentCount == -1) {
            return true;
        }
        for (int i = 0; i < parentCount; i++) {
            if (spi == null) {
                return true;
            }
            PropId propId = propIds[i];
            if (!spi.__isLoaded(propId)) {
                return false;
            }
            Object value = spi.__get(propId);
            if (value instanceof ImmutableSpi) {
                spi = (ImmutableSpi) value;
            } else if (spi.__type().getProp(propId).isReferenceList(TargetLevel.ENTITY)) {
                List<Object> list = (List<Object>) value;
                PropId[] itemPropIds = Arrays.copyOfRange(propIds, i + 1, propIds.length);
                for (Object item : list) {
                    if (!isLoadedChain(item, itemPropIds)) {
                        return false;
                    }
                }
                return true;
            } else if (value != null) {
                throw new IllegalArgumentException(
                        "The properties[" +
                                i +
                                "] \"" +
                                spi.__type().getProp(propId) +
                                "\" is neither association nor embedded property"
                );
            }
        }
        return spi == null || spi.__isLoaded(propIds[parentCount]);
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
    public static Object get(Object immutable, PropId prop) {
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
                        return false;
                    }
                } else if (spi.__isLoaded(prop.getId())) {
                    return false;
                }
            }
            return true;
        }
        throw new IllegalArgumentException("The first argument is immutable object created by jimmer");
    }

    @Nullable
    public static <T> T makeIdOnly(Class<T> type, @Nullable Object id) {
        return makeIdOnly(ImmutableType.get(type), id);
    }

    @Nullable
    @SuppressWarnings("unchecked")
    public static <T> T makeIdOnly(ImmutableType type, @Nullable Object id) {
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
        if (!(immutable instanceof ImmutableSpi)) {
            throw new IllegalArgumentException("The first argument is not immutable object created by jimmer");
        }

        ImmutableSpi spi = (ImmutableSpi) immutable;
        ImmutableType type = spi.__type();
        for (ImmutableProp prop : type.getProps().values()) {
            if (!(prop.isAssociation(TargetLevel.ENTITY) && spi.__isLoaded(prop.getId()))) {
                continue;
            }
            if (!prop.isColumnDefinition()) {
                return false;
            }
            ImmutableSpi target = (ImmutableSpi) spi.__get(prop.getId());
            if (target != null && !isIdOnly(target)) {
                return false;
            }
        }
        return true;
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
                PropId propId = prop.getId();
                if (prop.isAssociation(TargetLevel.ENTITY) && spi.__isLoaded(propId)) {
                    if (prop.isColumnDefinition()) {
                        ImmutableSpi target = (ImmutableSpi) spi.__get(propId);
                        if (target != null) {
                            ImmutableType targetType = prop.getTargetType();
                            PropId targetIdPropId = targetType.getIdProp().getId();
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

    public static <T> Collection<T> toIdOnly(Iterable<T> immutables) {
        if (immutables instanceof Set<?>) {
            return toIdOnly((Set<T>) immutables);
        }
        List<T> idOnlyList = immutables instanceof Collection<?> ?
                new ArrayList<>(((Collection<?>)immutables).size()) :
                new ArrayList<>();
        for (T immutable : immutables) {
            idOnlyList.add(toIdOnly(immutable));
        }
        return idOnlyList;
    }

    public static <T> Set<T> toIdOnly(Set<T> immutables) {
        Set<T> idOnlySet = new LinkedHashSet<>((immutables.size() * 4 + 2) / 3);
        for (T immutable : immutables) {
            idOnlySet.add(toIdOnly(immutable));
        }
        return idOnlySet;
    }

    public static <T> List<T> toIdOnly(List<T> immutables) {
        List<T> idOnlyList = new ArrayList<>(immutables.size());
        for (T immutable : immutables) {
            idOnlyList.add(toIdOnly(immutable));
        }
        return idOnlyList;
    }

    public static <T> T toIdOnly(T immutable) {
        if (immutable == null) {
            return null;
        }
        ImmutableSpi spi = (ImmutableSpi) immutable;
        ImmutableType type = spi.__type();
        ImmutableProp idProp = type.getIdProp();
        if (idProp == null) {
            throw new IllegalArgumentException(
                    "Cannot convert \"" + type + "\" to id only object because it is not entity"
            );
        }
        if (!spi.__isLoaded(idProp.getId())) {
            throw new IllegalArgumentException(
                    "Cannot convert \"" + type + "\" to id only object because its id property \"" +
                            type.getIdProp() +
                            "\""
            );
        }
        return makeIdOnly(type, spi.__get(idProp.getId()));
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
            return OBJECT_MAPPER.writeValueAsString(immutable);
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
    public static <I> I fromString(Class<I> type, String json) throws JsonProcessingException {
        return OBJECT_MAPPER.readValue(json, type);
    }

    public static <I> I fromString(Class<I> type, String json, @Nullable ObjectMapper mapper) throws JsonProcessingException {
        return (mapper != null ? mapper : objectMapper()).readValue(json, type);
    }

    @SuppressWarnings("unchecked")
    @SafeVarargs
    public static <I> I merge(I ... parts) {
        List<ImmutableSpi> spis = new ArrayList<>(parts.length);
        for (I part : parts) {
            if (part == null) {
                continue;
            }
            if (!(part instanceof ImmutableSpi)) {
                throw new IllegalArgumentException("Each element of `parts` must be immutable object");
            }
            ImmutableSpi spi = (ImmutableSpi) part;
            if (!spis.isEmpty()) {
                if (spis.get(0).__type() != spi.__type()) {
                    throw new IllegalArgumentException(
                            "All element of `parts` must belong to same type, but both \"" +
                                    spis.get(0).__type() +
                                    "\" and \"" +
                                    spi.__type() +
                                    "\" are found"
                    );
                }
            }
            spis.add(spi);
        }
        if (spis.size() == 1) {
            return (I) spis.get(0);
        }
        return (I) Internal.produce(spis.get(0).__type(), null, draft -> {
            for (ImmutableSpi spi : spis) {
                merge((DraftSpi) draft, spi);
            }
        });
    }

    private static void merge(DraftSpi target, ImmutableSpi source) {
        for (ImmutableProp prop : source.__type().getProps().values()) {
            PropId propId = prop.getId();
            if (source.__isLoaded(propId)) {
                target.__set(propId, source.__get(propId));
            }
        }
    }

    @SuppressWarnings("unchecked")
    public static <I> I deepClone(I immutable) {
        if (immutable == null) {
            return null;
        }
        ImmutableType immutableType = ImmutableType.get(immutable.getClass());
        return (I) deepClone(immutableType, immutable);
    }

    @SuppressWarnings("unchecked")
    private static Object deepClone(ImmutableType type, Object immutable) {
        ImmutableSpi from = (ImmutableSpi) immutable;
        return Internal.produce(type, null, draft -> {
            DraftSpi to = (DraftSpi) draft;
            for (ImmutableProp prop : type.getProps().values()) {
                PropId propId = prop.getId();
                if (prop.isView() || !prop.isMutable() || !from.__isLoaded(propId)) {
                    continue;
                }
                ImmutableType targetType = prop.getTargetType();
                if (prop.isReferenceList(TargetLevel.OBJECT)) {
                    List<Object> targets = (List<Object>) from.__get(propId);
                    List<Object> clonedTargets = new ArrayList<>(targets.size());
                    for (Object target : targets) {
                        clonedTargets.add(deepClone(targetType, target));
                    }
                    to.__set(propId, clonedTargets);
                } else if (prop.isReference(TargetLevel.OBJECT)) {
                    Object target = from.__get(propId);
                    if (target != null) {
                        to.__set(propId, deepClone(targetType, target));
                    }
                } else {
                    to.__set(propId, from.__get(propId));
                }
            }
        });
    }

    public static boolean isLogicalDeleted(Object o) {
        if (!(o instanceof ImmutableSpi)) {
            return false;
        }
        ImmutableSpi spi = (ImmutableSpi) o;
        LogicalDeletedInfo info = spi.__type().getLogicalDeletedInfo();
        if (info == null) {
            return false;
        }
        PropId propId = info.getProp().getId();
        if (!spi.__isLoaded(propId)) {
            return false;
        }
        return info.isDeleted(spi.__get(propId));
    }

    public static <T> Set<T> emptyStringToNull(Set<T> s) {
        Set<T> newSet = new LinkedHashSet<>((s.size() * 4 + 2) / 3);
        for (T e : s) {
            newSet.add(emptyStringToNullImpl(e));
        }
        return newSet;
    }

    public static <T> List<T> emptyStringToNull(Collection<T> c) {
        List<T> newList = new ArrayList<>(c.size());
        for (T e : c) {
            newList.add(emptyStringToNullImpl(e));
        }
        return newList;
    }

    @SuppressWarnings("unchecked")
    public static <T> T emptyStringToNull(T o) {
        if (o instanceof Set<?>) {
            return (T) emptyStringToNull((Set<?>) o);
        }
        if (o instanceof Collection<?>) {
            return (T) emptyStringToNull((List<?>) o);
        }
        return emptyStringToNullImpl(o);
    }

    @SuppressWarnings("unchecked")
    private static <T> T emptyStringToNullImpl(T o) {
        if (!(o instanceof ImmutableSpi)) {
            return o;
        }
        ImmutableSpi spi = (ImmutableSpi) o;
        return (T) Internal.produce(spi.__type(), o, draft -> {
            DraftSpi draftSpi = (DraftSpi) draft;
            for (ImmutableProp prop : spi.__type().getProps().values()) {
                if (!prop.isNullable()) {
                    continue;
                }
                PropId propId = prop.getId();
                if (!spi.__isLoaded(propId)) {
                    continue;
                }
                if (prop.getReturnClass() == String.class) {
                    Object value = spi.__get(propId);
                    if ("".equals(value)) {
                        draftSpi.__set(propId, null);
                    }
                } else if (prop.isAssociation(TargetLevel.OBJECT)) {
                    Object value = spi.__get(propId);
                    if (value == null) {
                        continue;
                    }
                    if (value instanceof Collection<?>) {
                        draftSpi.__set(propId, emptyStringToNull((Collection<?>)value));
                    } else {
                        draftSpi.__set(propId, emptyStringToNullImpl(value));
                    }
                }
            }
        });
    }

    private static ObjectMapper objectMapper() {
        if (OBJECT_MAPPER == null) {
            throw new IllegalStateException("Jackson2 is required");
        }
        return OBJECT_MAPPER;
    }

    private static boolean classExists(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException ex) {
            return false;
        }
    }

    static {
        if (classExists("com.fasterxml.jackson.databind.ObjectMapper")) {
            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule());
            mapper.registerModule(new ImmutableModule());
            mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            OBJECT_MAPPER = mapper;
        } else {
            OBJECT_MAPPER = null;
        }
    }
}
