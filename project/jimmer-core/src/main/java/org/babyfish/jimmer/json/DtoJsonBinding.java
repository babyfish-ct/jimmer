package org.babyfish.jimmer.json;

import org.babyfish.jimmer.Dto;
import org.babyfish.jimmer.Specification;
import org.babyfish.jimmer.impl.org.objectweb.asm.AnnotationVisitor;
import org.babyfish.jimmer.impl.org.objectweb.asm.ClassReader;
import org.babyfish.jimmer.impl.org.objectweb.asm.ClassVisitor;
import org.babyfish.jimmer.impl.org.objectweb.asm.Opcodes;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

/** The JSON projection and construction rules generated with a Jimmer DTO. */
public abstract class DtoJsonBinding {

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface Generated {
        Class<? extends DtoJsonBinding> value();
    }

    private static final ClassValue<Optional<DtoJsonBinding>> BINDINGS =
            new ClassValue<Optional<DtoJsonBinding>>() {
                @Override
                protected Optional<DtoJsonBinding> computeValue(Class<?> type) {
                    Generated generated = type.getDeclaredAnnotation(Generated.class);
                    if (generated == null) {
                        if ((Dto.class.isAssignableFrom(type) || Specification.class.isAssignableFrom(type)) &&
                                hasLegacyGeneratorMarker(type)) {
                            throw new IllegalStateException(
                                    "Generated DTO " + type.getName() + " has no JSON binding. " +
                                    "Rebuild its model module with the current Jimmer APT or KSP processor."
                            );
                        }
                        return Optional.empty();
                    }
                    try {
                        return Optional.of(generated.value().getConstructor().newInstance());
                    } catch (ReflectiveOperationException ex) {
                        throw new IllegalStateException("Cannot load JSON binding for " + type.getName(), ex);
                    }
                }
            };

    // Older generators used CLASS retention. A class-file check distinguishes their
    // output from hand-written View and Specification types, even for nested branches.
    private static boolean hasLegacyGeneratorMarker(Class<?> type) {
        String resource = "/" + type.getName().replace('.', '/') + ".class";
        try (InputStream stream = type.getResourceAsStream(resource)) {
            if (stream == null) {
                return false;
            }
            class Marker extends ClassVisitor {
                boolean found;

                Marker() { super(Opcodes.ASM9); }

                @Override
                public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
                    if (descriptor.equals("Lorg/babyfish/jimmer/internal/GeneratedBy;")) {
                        found = true;
                    }
                    return null;
                }
            }
            Marker marker = new Marker();
            new ClassReader(stream).accept(marker, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            return marker.found;
        } catch (IOException | IllegalArgumentException ex) {
            // A class loader may not expose readable class files for application classes.
            return false;
        }
    }

    public static DtoJsonBinding find(Class<?> type) {
        return BINDINGS.get(type).orElse(null);
    }

    private final Class<?> type;
    private final List<Property> properties = new ArrayList<>();
    private final List<Property> propertyView = Collections.unmodifiableList(properties);
    private final Map<String, Class<?>> subtypes = new LinkedHashMap<>();
    private final Map<Class<?>, String> discriminatorValues = new LinkedHashMap<>();
    private final String discriminator;
    private final Class<?> defaultSubtype;
    private final Class<?> polymorphicRoot;

    protected DtoJsonBinding(Class<?> type, String discriminator, Class<?> defaultSubtype, Class<?> polymorphicRoot) {
        this.polymorphicRoot = polymorphicRoot;
        this.type = type;
        this.discriminator = discriminator;
        this.defaultSubtype = defaultSubtype;
    }

    protected final void property(
            String name, String getter, boolean readFromJson, boolean writeToJson,
            Function<Object, Object> read, Predicate<Object> loaded
    ) {
        try {
            Method method = type.getMethod(getter);
            properties.add(new Property(name, getter, method.getGenericReturnType(), method.getAnnotation(JsonFormat.class),
                    readFromJson, writeToJson, read, loaded));
        } catch (NoSuchMethodException ex) {
            throw new IllegalStateException("Invalid generated getter " + type.getName() + "." + getter, ex);
        }
    }

    protected final void subtype(String discriminatorValue, Class<?> subtype) {
        subtypes.put(discriminatorValue, subtype);
        discriminatorValues.put(subtype, discriminatorValue);
    }

    public final List<Property> getProperties() {
        return propertyView;
    }

    public final String getDiscriminator() {
        return discriminator;
    }

    public final Map<String, Class<?>> getSubtypes() {
        return Collections.unmodifiableMap(subtypes);
    }

    public final Class<?> getSubtype(String value) {
        return subtypes.get(value);
    }

    public final Class<?> getPolymorphicRoot() {
        return polymorphicRoot;
    }

    public final Class<?> getDefaultSubtype() {
        return defaultSubtype;
    }

    public final String getDiscriminatorValue(Class<?> subtype) {
        return discriminatorValues.get(subtype);
    }

    /** Values have already been decoded according to each property's declared type. */
    public Object create(Map<String, Object> values) {
        throw new IllegalArgumentException("Cannot construct abstract DTO " + type.getName());
    }

    public static final class Property {
        private final String name;
        private final String getterName;
        private final Type type;
        private final JsonFormat format;
        private final boolean readFromJson;
        private final boolean writeToJson;
        private final Function<Object, Object> read;
        private final Predicate<Object> loaded;

        private Property(String name, String getterName, Type type, JsonFormat format, boolean readFromJson, boolean writeToJson,
                         Function<Object, Object> read, Predicate<Object> loaded) {
            this.name = name;
            this.getterName = getterName;
            this.format = format;
            this.type = type;
            this.readFromJson = readFromJson;
            this.writeToJson = writeToJson;
            this.read = read;
            this.loaded = loaded;
        }

        public String getName() { return name; }
        public String getGetterName() { return getterName; }
        public Type getType() { return type; }
        public JsonFormat getFormat() { return format; }
        public boolean readFromJson() { return readFromJson; }
        public boolean writeToJson() { return writeToJson; }
        public boolean isLoaded(Object dto) { return loaded == null || loaded.test(dto); }
        public Object get(Object dto) { return read.apply(dto); }
    }
}
