package org.babyfish.jimmer.json;

import org.babyfish.jimmer.json.converter.ConverterMetadata;
import org.babyfish.jimmer.lang.Generics;
import org.babyfish.jimmer.meta.*;
import org.babyfish.jimmer.runtime.DraftSpi;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.runtime.Internal;

import java.beans.ConstructorProperties;
import java.lang.reflect.*;
import java.util.*;

/** The Jimmer model rules shared by JSON adapters. Native scalar and bean handling is delegated. */
public final class ModelJson {

    @FunctionalInterface
    public interface Conversion {
        Object apply(Type type, Object value);
    }

    private ModelJson() {}

    public static Class<?> rawType(Type type) {
        if (type instanceof ParameterizedType) {
            return (Class<?>) ((ParameterizedType) type).getRawType();
        }
        if (type instanceof WildcardType) {
            return rawType(((WildcardType) type).getUpperBounds()[0]);
        }
        if (type instanceof GenericArrayType) {
            return Array.newInstance(rawType(((GenericArrayType) type).getGenericComponentType()), 0).getClass();
        }
        return type instanceof Class<?> ? (Class<?>) type : Object.class;
    }

    public static boolean supports(Type type) {
        Class<?> raw = rawType(type);
        if (raw.isArray()) {
            return supports(componentType(type));
        }
        if (type instanceof ParameterizedType && (Iterable.class.isAssignableFrom(raw) || Map.class.isAssignableFrom(raw))) {
            Type[] args = ((ParameterizedType) type).getActualTypeArguments();
            return supports(args[args.length - 1]);
        }
        return EnumJson.find(raw) != null || DtoJsonBinding.find(raw) != null ||
                ImmutableType.tryGet(raw) != null || CONSTRUCTORS.get(raw).isPresent();
    }

    public static Object encode(Type type, Object value, Conversion fallback) {
        if (value == null) {
            return null;
        }
        if (value instanceof Enum<?>) {
            Enum<?> constant = (Enum<?>) value;
            EnumJson mapping = EnumJson.find(constant.getDeclaringClass());
            if (mapping != null) {
                return mapping.encode(constant);
            }
        }
        Class<?> raw = rawType(type);
        DtoJsonBinding dto = DtoJsonBinding.find(raw);
        if (dto == null) {
            dto = DtoJsonBinding.find(value.getClass());
        }
        if (dto != null) {
            return writeDto(dto, value, fallback);
        }
        if (value instanceof ImmutableSpi) {
            return writeImmutable((ImmutableSpi) value, fallback);
        }
        if (value instanceof Iterable<?>) {
            Type element = argument(type, Iterable.class, 0);
            List<Object> result = new ArrayList<>();
            for (Object item : (Iterable<?>) value) {
                result.add(encode(element, item, fallback));
            }
            return result;
        }
        if (value.getClass().isArray() && supports(componentType(type))) {
            List<Object> result = new ArrayList<>();
            for (int i = 0; i < Array.getLength(value); i++) {
                result.add(encode(componentType(type), Array.get(value, i), fallback));
            }
            return result;
        }
        if (value instanceof Map<?, ?> && supports(argument(type, Map.class, 1))) {
            Map<String, Object> result = new LinkedHashMap<>();
            Type element = argument(type, Map.class, 1);
            for (Map.Entry<?, ?> e : ((Map<?, ?>) value).entrySet()) {
                result.put(e.getKey().toString(), encode(element, e.getValue(), fallback));
            }
            return result;
        }
        ConstructorBinding constructor = CONSTRUCTORS.get(raw).orElse(null);
        if (constructor != null) {
            Map<String, Object> result = new LinkedHashMap<>();
            for (int i = 0; i < constructor.names.length; i++) {
                result.put(constructor.names[i], encode(constructor.type(type, i), constructor.get(value, i), fallback));
            }
            return result;
        }
        return fallback.apply(raw == Object.class ? value.getClass() : type, value);
    }

    public static Object decode(Type type, Object value, Conversion fallback) {
        if (value == null) {
            return null;
        }
        Class<?> raw = rawType(type);
        EnumJson enumJson = EnumJson.find(raw);
        if (enumJson != null) {
            return enumJson.decode(value);
        }
        DtoJsonBinding dto = DtoJsonBinding.find(raw);
        if (dto != null) {
            return readDto(dto, type, object(value, type), fallback);
        }
        ImmutableType model = ImmutableType.tryGet(raw);
        if (model != null) {
            return readImmutable(model, object(value, type), fallback);
        }
        if (raw.isArray() && supports(componentType(type))) {
            List<?> items = (List<?>) value;
            Object result = Array.newInstance(raw.getComponentType(), items.size());
            for (int i = 0; i < items.size(); i++) {
                Array.set(result, i, decode(componentType(type), items.get(i), fallback));
            }
            return result;
        }
        if (Iterable.class.isAssignableFrom(raw) && supports(type)) {
            Collection<Object> result = collection(raw);
            Type element = argument(type, Iterable.class, 0);
            for (Object item : (List<?>) value) {
                result.add(decode(element, item, fallback));
            }
            return result;
        }
        if (Map.class.isAssignableFrom(raw) && supports(type)) {
            Map<Object, Object> result = container(raw,
                    SortedMap.class.isAssignableFrom(raw) ? new TreeMap<>() : new LinkedHashMap<>());
            for (Map.Entry<?, ?> e : object(value, type).entrySet()) {
                result.put(decode(argument(type, Map.class, 0), e.getKey(), fallback),
                        decode(argument(type, Map.class, 1), e.getValue(), fallback));
            }
            return result;
        }
        ConstructorBinding constructor = CONSTRUCTORS.get(raw).orElse(null);
        if (constructor != null) {
            Map<?, ?> fields = object(value, type);
            Object[] args = new Object[constructor.names.length];
            for (int i = 0; i < args.length; i++) {
                args[i] = decode(constructor.type(type, i), fields.get(constructor.names[i]), fallback);
            }
            try {
                return constructor.ctor.newInstance(args);
            } catch (ReflectiveOperationException ex) {
                throw new IllegalArgumentException("Cannot construct " + type, ex);
            }
        }
        return fallback.apply(type, value);
    }

    private static Object writeDto(DtoJsonBinding dto, Object value, Conversion fallback) {
        Map<String, Object> fields = new LinkedHashMap<>();
        DtoJsonBinding root = dto.getPolymorphicRoot() != null ? DtoJsonBinding.find(dto.getPolymorphicRoot()) : dto;
        if (root != null && root.getDiscriminator() != null) {
            String tag = root.getDiscriminatorValue(value.getClass());
            if (tag == null) {
                if (value.getClass() != root.getDefaultSubtype()) {
                    throw new IllegalArgumentException("Unknown DTO subtype " + value.getClass().getName());
                }
                String className = value.getClass().getName();
                tag = className.substring(className.lastIndexOf('.') + 1);
            }
            // A projected discriminator below can supply the original value of a default branch.
            fields.put(root.getDiscriminator(), tag);
            dto = DtoJsonBinding.find(value.getClass());
        }
        for (DtoJsonBinding.Property prop : dto.getProperties()) {
            if (prop.writeToJson() && prop.isLoaded(value)) {
                fields.put(prop.getName(), JsonFormats.encode(prop.getFormat(), prop.getType(), prop.get(value), fallback));
            }
        }
        return fields;
    }

    private static Object writeImmutable(ImmutableSpi spi, Conversion fallback) {
        Map<String, Object> fields = new LinkedHashMap<>();
        for (ImmutableProp prop : ORDERED_PROPS.get(spi.__type().getJavaClass())) {
            if (ignored(prop) || !spi.__isVisible(prop.getId()) || !spi.__isLoaded(prop.getId())) {
                continue;
            }
            Object item = spi.__get(prop.getId());
            ConverterMetadata converter = prop.getConverterMetadata();
            if (item != null && converter != null) {
                item = converter.getConverter().output(item);
            }
            fields.put(prop.getName(), JsonFormats.encode(prop.getAnnotation(JsonFormat.class),
                    converter != null ? converter.getTargetType() : prop.getGenericType(), item, fallback));
        }
        return fields;
    }

    private static Object readDto(DtoJsonBinding dto, Type type, Map<?, ?> fields, Conversion fallback) {
        if (dto.getDiscriminator() != null) {
            Object tag = fields.get(dto.getDiscriminator());
            Class<?> subtype = tag instanceof String ? dto.getSubtype((String) tag) : null;
            if (subtype == null) {
                subtype = dto.getDefaultSubtype();
            }
            if (subtype == null) {
                throw new IllegalArgumentException("Invalid or missing discriminator " + dto.getDiscriminator() + " for " + type);
            }
            dto = DtoJsonBinding.find(subtype);
        }
        Map<String, Object> args = new LinkedHashMap<>();
        for (DtoJsonBinding.Property prop : dto.getProperties()) {
            if (prop.readFromJson() && fields.containsKey(prop.getName())) {
                try {
                    args.put(prop.getName(), JsonFormats.decode(
                            prop.getFormat(), prop.getType(), fields.get(prop.getName()), fallback
                    ));
                } catch (RuntimeException ex) {
                    throw new IllegalArgumentException("Cannot read property " + type + "." + prop.getName(), ex);
                }
            }
        }
        return dto.create(args);
    }

    private static Object readImmutable(ImmutableType model, Map<?, ?> fields, Conversion fallback) {
        ImmutableType actual = actualType(model, fields);
        return Internal.produce(actual, null, draft -> {
            DraftSpi spi = (DraftSpi) draft;
            for (Map.Entry<?, ?> e : fields.entrySet()) {
                ImmutableProp prop = actual.getProps().get(e.getKey());
                if (prop == null || ignored(prop) || (!prop.isMutable() && !prop.isDiscriminator())) {
                    continue;
                }
                if (e.getValue() == null && !prop.isNullable()) {
                    continue;
                }
                ConverterMetadata converter = prop.getConverterMetadata();
                Object item = JsonFormats.decode(prop.getAnnotation(JsonFormat.class),
                        converter != null ? converter.getTargetType() : prop.getGenericType(), e.getValue(), fallback);
                spi.__set(prop.getId(), item != null && converter != null ? converter.getConverter().input(item) : item);
                spi.__show(prop.getId(), true);
            }
            for (ImmutableProp prop : actual.getProps().values()) {
                if (!fields.containsKey(prop.getName()) && spi.__isLoaded(prop.getId())) {
                    spi.__show(prop.getId(), false);
                }
            }
        });
    }

    private static boolean ignored(ImmutableProp prop) {
        JsonIgnore ignore = prop.getAnnotation(JsonIgnore.class);
        return ignore != null && ignore.value();
    }

    private static Map<?, ?> object(Object value, Type type) {
        if (!(value instanceof Map<?, ?>)) {
            throw new IllegalArgumentException("Expected a JSON object for " + type);
        }
        return (Map<?, ?>) value;
    }

    private static ImmutableType actualType(ImmutableType type, Map<?, ?> fields) {
        InheritanceInfo info = type.getInheritanceInfo();
        if (info == null) {
            return type;
        }
        String key = info.getDiscriminatorProp().getName();
        if (!fields.containsKey(key) && type.isInstantiable()) {
            return type;
        }
        Object tag = fields.get(key);
        ImmutableType actual = tag != null ? info.getDiscriminatorTypeMap(type).get(info.discriminatorValue(tag.toString())) : null;
        if (actual == null) {
            throw new IllegalArgumentException("Invalid or missing discriminator " + key + " for " + type);
        }
        return actual;
    }

    private static Collection<Object> collection(Class<?> raw) {
        Collection<Object> result;
        if (SortedSet.class.isAssignableFrom(raw)) {
            result = new TreeSet<>();
        } else if (Set.class.isAssignableFrom(raw)) {
            result = new LinkedHashSet<>();
        } else if (Queue.class.isAssignableFrom(raw)) {
            result = new LinkedList<>();
        } else {
            result = new ArrayList<>();
        }
        return container(raw, result);
    }

    @SuppressWarnings("unchecked")
    private static <T> T container(Class<?> raw, T defaultValue) {
        if (raw.isInstance(defaultValue)) {
            return defaultValue;
        }
        try {
            return (T) raw.getConstructor().newInstance();
        } catch (ReflectiveOperationException ex) {
            throw new IllegalArgumentException("Cannot construct JSON container of type " + raw.getName(), ex);
        }
    }

    private static Type argument(Type type, Class<?> parent, int index) {
        if (!(type instanceof ParameterizedType)) {
            return Object.class;
        }
        return Generics.getTypeArguments(type, parent)[index];
    }

    private static Type componentType(Type type) {
        if (type instanceof GenericArrayType) {
            return ((GenericArrayType) type).getGenericComponentType();
        }
        Class<?> raw = rawType(type);
        return raw.isArray() ? raw.getComponentType() : Object.class;
    }

    private static final ClassValue<List<ImmutableProp>> ORDERED_PROPS = new ClassValue<List<ImmutableProp>>() {
        @Override
        protected List<ImmutableProp> computeValue(Class<?> type) {
            List<ImmutableProp> props = new ArrayList<>(ImmutableType.get(type).getProps().values());
            props.sort(Comparator.comparingInt(prop -> prop.getId().asIndex()));
            return props;
        }
    };

    private static final ClassValue<Optional<ConstructorBinding>> CONSTRUCTORS = new ClassValue<Optional<ConstructorBinding>>() {
        @Override
        protected Optional<ConstructorBinding> computeValue(Class<?> type) {
            if (type.getName().startsWith("org.babyfish.jimmer.")) {
                for (Constructor<?> ctor : type.getConstructors()) {
                    ConstructorProperties annotation = ctor.getAnnotation(ConstructorProperties.class);
                    if (annotation != null) {
                        return Optional.of(new ConstructorBinding(ctor, annotation.value()));
                    }
                }
            }
            return Optional.empty();
        }
    };

    private static class ConstructorBinding {

        final Constructor<?> ctor;
        final String[] names;
        final Method[] getters;

        ConstructorBinding(Constructor<?> ctor, String[] names) {
            this.ctor = ctor;
            if (!Modifier.isPublic(ctor.getDeclaringClass().getModifiers())) {
                ctor.setAccessible(true);
            }
            this.names = names;
            getters = new Method[names.length];
            for (int i = 0; i < names.length; i++) {
                String suffix = Character.toUpperCase(names[i].charAt(0)) + names[i].substring(1);
                try {
                    try {
                        getters[i] = ctor.getDeclaringClass().getMethod("get" + suffix);
                    } catch (NoSuchMethodException ex) {
                        getters[i] = ctor.getDeclaringClass().getMethod("is" + suffix);
                    }
                    if (!Modifier.isPublic(getters[i].getDeclaringClass().getModifiers())) {
                        getters[i].setAccessible(true);
                    }
                } catch (NoSuchMethodException ex) {
                    throw new IllegalArgumentException("Missing getter for " + names[i] + " of " + ctor, ex);
                }
            }
        }

        Type type(Type owner, int i) {
            return Generics.resolve(ctor.getGenericParameterTypes()[i], owner, ctor.getDeclaringClass());
        }

        Object get(Object value, int i) {
            try {
                return getters[i].invoke(value);
            } catch (ReflectiveOperationException ex) {
                throw new IllegalArgumentException("Cannot read " + getters[i], ex);
            }
        }
    }
}
