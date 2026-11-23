package org.babyfish.jimmer.jackson.v3;

import org.babyfish.jimmer.Draft;
import org.babyfish.jimmer.JimmerVersion;
import org.babyfish.jimmer.impl.util.StringUtil;
import org.babyfish.jimmer.json.converter.Converter;
import org.babyfish.jimmer.json.converter.ConverterMetadata;
import org.babyfish.jimmer.meta.impl.ImmutableProps;
import org.babyfish.jimmer.json.JsonIgnore;
import org.babyfish.jimmer.json.DtoJsonBinding;
import org.babyfish.jimmer.json.EnumJson;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import tools.jackson.databind.jsontype.NamedType;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import tools.jackson.core.Version;
import tools.jackson.databind.AnnotationIntrospector;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.PropertyName;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.annotation.JsonPOJOBuilder;
import tools.jackson.databind.annotation.JsonNaming;
import tools.jackson.databind.cfg.MapperConfig;
import tools.jackson.databind.introspect.Annotated;
import tools.jackson.databind.introspect.AnnotatedClass;
import tools.jackson.databind.introspect.AnnotatedMember;
import tools.jackson.databind.type.TypeFactory;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

class ImmutableAnnotationIntrospectorV3 extends AnnotationIntrospector {

    @Override
    public Version version() {
        return new Version(
                JimmerVersion.major,
                JimmerVersion.minor,
                JimmerVersion.patch,
                null,
                "org.babyfish.jimmer",
                "jimmer-core"
        );
    }

    @Override
    public JsonTypeInfo.Value findPolymorphicTypeInfo(MapperConfig<?> config, Annotated annotated) {
        DtoJsonBinding root = polymorphicRoot(annotated);
        if (root == null || annotated.hasAnnotation(JsonTypeInfo.class)) {
            return null;
        }
        boolean visible = isDiscriminatorProjected(root);
        return JsonTypeInfo.Value.construct(JsonTypeInfo.Id.NAME,
                visible ? JsonTypeInfo.As.EXISTING_PROPERTY : JsonTypeInfo.As.PROPERTY,
                root.getDiscriminator(), root.getDefaultSubtype(), visible, null);
    }

    @Override
    public List<NamedType> findSubtypes(MapperConfig<?> config, Annotated annotated) {
        DtoJsonBinding root = polymorphicRoot(annotated);
        if (root == null || annotated.hasAnnotation(JsonSubTypes.class)) {
            return null;
        }
        List<NamedType> subtypes = new ArrayList<>();
        for (Class<?> subtype : root.getSubtypes().values()) {
            subtypes.add(new NamedType(subtype));
        }
        return subtypes;
    }

    @Override
    public String findTypeName(MapperConfig<?> config, AnnotatedClass ac) {
        DtoJsonBinding root = polymorphicRoot(ac);
        if (root == null || ac.hasAnnotation(JsonTypeName.class) || ac.hasAnnotation(JsonSubTypes.class)) {
            return null;
        }
        return root.getDiscriminatorValue(ac.getRawType());
    }

    private static DtoJsonBinding polymorphicRoot(Annotated annotated) {
        if (!(annotated instanceof AnnotatedClass)) {
            return null;
        }
        DtoJsonBinding binding = DtoJsonBinding.find(annotated.getRawType());
        if (binding != null && binding.getPolymorphicRoot() != null) {
            binding = DtoJsonBinding.find(binding.getPolymorphicRoot());
        }
        return binding != null && binding.getDiscriminator() != null ? binding : null;
    }

    private static boolean isDiscriminatorProjected(DtoJsonBinding root) {
        for (DtoJsonBinding.Property prop : root.getProperties()) {
            if (prop.getName().equals(root.getDiscriminator())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Object findNamingStrategy(MapperConfig<?> config, AnnotatedClass ac) {
        Class<?> builder = ac.getRawType();
        Class<?> dto = builder.getDeclaringClass();
        if (dto != null && builder.getSimpleName().equals("Builder") &&
                DtoJsonBinding.find(dto) != null && !ac.hasAnnotation(JsonNaming.class)) {
            JsonNaming naming = dto.getAnnotation(JsonNaming.class);
            return naming != null ? naming.value() : null;
        }
        return null;
    }

    @Override
    public PropertyName findNameForSerialization(MapperConfig<?> config, Annotated annotated) {
        AnnotatedElement element = annotated.getAnnotated();
        if (element instanceof Method) {
            Method method = (Method) element;
            Class<?> declaringType = method.getDeclaringClass();
            if (method.getParameterCount() == 0 &&
                    (declaringType.isInterface() || ImmutableSpi.class.isAssignableFrom(declaringType))) {
                ImmutableType type = ImmutableType.tryGet(declaringType);
                ImmutableProp prop = type != null ? ImmutableProps.tryGet(type, method) : null;
                // Expose both the interface accessor and the generated JavaBean bridge. The concrete
                // raw accessor carries an ignore marker to keep it distinct from its bridge.
                if (prop != null && (declaringType.isInterface() || !method.getName().equals(prop.getName()))) {
                    return PropertyName.USE_DEFAULT;
                }
            }
        }
        return null;
    }

    @Override
    public Object findSerializer(MapperConfig<?> config, Annotated annotated) {
        EnumJson mapping = annotated instanceof AnnotatedClass ? EnumJson.find(annotated.getRawType()) : null;
        return mapping != null ? new EnumScalarV3.Serializer(mapping) : null;
    }

    @Override
    public Object findDeserializer(MapperConfig<?> config, Annotated annotated) {
        EnumJson mapping = annotated instanceof AnnotatedClass ? EnumJson.find(annotated.getRawType()) : null;
        return mapping != null ? new EnumScalarV3.Deserializer(annotated.getRawType(), mapping) : null;
    }

    @Override
    public String[] findSerializationPropertyOrder(MapperConfig<?> config, AnnotatedClass ac) {
        if (ac.hasAnnotation(JsonPropertyOrder.class)) {
            return null;
        }
        ImmutableType type = ImmutableType.tryGet(ac.getRawType());
        return type != null ? type.getProps().values().stream()
                .sorted(Comparator.comparingInt(prop -> prop.getId().asIndex()))
                .map(ImmutableProp::getName)
                .toArray(String[]::new) : null;
    }

    @Override
    public JsonFormat.Value findFormat(MapperConfig<?> config, Annotated annotated) {
        org.babyfish.jimmer.json.JsonFormat format = annotated.getAnnotation(org.babyfish.jimmer.json.JsonFormat.class);
        if (format == null) {
            return null;
        }
        JsonFormat.Value value = JsonFormat.Value.empty().withPattern(format.pattern());
        if (format.shape() == org.babyfish.jimmer.json.JsonFormat.Shape.STRING) {
            value = value.withShape(JsonFormat.Shape.STRING);
        }
        return value;
    }

    @Override
    public boolean hasIgnoreMarker(MapperConfig<?> config, AnnotatedMember member) {
        JsonIgnore ignore = member.getAnnotation(JsonIgnore.class);
        return ignore != null && ignore.value();
    }

    @Override
    public Class<?> findPOJOBuilder(final MapperConfig<?> config, final AnnotatedClass ac) {
        if (DtoJsonBinding.find(ac.getAnnotated()) != null) {
            for (Class<?> nested : ac.getAnnotated().getDeclaredClasses()) {
                if (nested.getSimpleName().equals("Builder")) {
                    return nested;
                }
            }
        }
        ImmutableType type = ImmutableType.tryGet(ac.getAnnotated());
        if (type == null) {
            return super.findPOJOBuilder(config, ac);
        }
        Class<?> draftClass;
        try {
            draftClass = Class.forName(type.getJavaClass().getName() + "Draft", true, type.getJavaClass().getClassLoader());
        } catch (ClassNotFoundException ex) {
            throw new IllegalStateException(
                    "Cannot not load the draft type \"" +
                            type.getJavaClass().getName() +
                            "Draft\""
            );
        }
        Class<?> builderClass = null;
        for (Class<?> nestedClass : draftClass.getDeclaredClasses()) {
            if (nestedClass.getSimpleName().equals("Builder")) {
                builderClass = nestedClass;
                break;
            }
        }
        if (builderClass == null) {
            throw new AssertionError(
                    "There is no nested type \"Builder\" in \"" +
                            draftClass.getName() +
                            "\""
            );
        }
        return builderClass;
    }

    @Override
    public JsonPOJOBuilder.Value findPOJOBuilderConfig(final MapperConfig<?> config, final AnnotatedClass ac) {
        Class<?> declaringType = ac.getAnnotated().getDeclaringClass();
        if (declaringType != null && (Draft.class.isAssignableFrom(declaringType) ||
                (ac.getAnnotated().getSimpleName().equals("Builder") && DtoJsonBinding.find(declaringType) != null))) {
            return new JsonPOJOBuilder.Value("build", "");
        }
        return super.findPOJOBuilderConfig(config, ac);
    }

    @Override
    public Object findSerializationConverter(final MapperConfig<?> config, final Annotated a) {
        AnnotatedElement element = a.getAnnotated();
        if (element instanceof Method) {
            Method method = (Method) element;
            ImmutableType type = ImmutableType.tryGet(method.getDeclaringClass());
            if (type != null) {
                ImmutableProp prop = ImmutableProps.tryGet(type, method);
                ConverterMetadata metadata = prop != null ? prop.getConverterMetadata() : null;
                if (metadata != null) {
                    return toOutput(metadata);
                }
            }
        }
        return super.findSerializationConverter(config, a);
    }

    @Override
    public Object findDeserializationConverter(final MapperConfig<?> config, final Annotated a) {
        AnnotatedElement element = a.getAnnotated();
        if (element instanceof Method) {
            Method method = (Method) element;
            if (method.getDeclaringClass().getSimpleName().equals("Builder")) {
                Class<?> parentClass = method.getDeclaringClass().getDeclaringClass();
                if (parentClass != null && Draft.class.isAssignableFrom(parentClass)) {
                    ImmutableType type = ImmutableType.get(parentClass);
                    String propName = StringUtil.propName(method.getName(), method.getReturnType() == boolean.class);
                    if (propName == null) {
                        propName = method.getName();
                    }
                    ImmutableProp prop = type.getProp(propName);
                    ConverterMetadata metadata = prop.getConverterMetadata();
                    if (metadata != null) {
                        return toInput(metadata);
                    }
                }
            }
        }
        return super.findDeserializationConverter(config, a);
    }

    private static tools.jackson.databind.util.Converter<?, ?> toOutput(ConverterMetadata metadata) {
        JavaType sourceJacksonType = JacksonUtilsV3.getJacksonType(metadata.getSourceType());
        JavaType targetJacksonType = JacksonUtilsV3.getJacksonType(metadata.getTargetType());
        Converter<Object, Object> converter = metadata.getConverter();

        return new tools.jackson.databind.util.Converter<Object, Object>() {

            @Override
            public Object convert(final SerializationContext ctxt, final Object value) {
                return converter.output(value);
            }

            @Override
            public Object convert(final DeserializationContext ctxt, final Object value) {
                throw new UnsupportedOperationException();
            }

            @Override
            public JavaType getInputType(TypeFactory typeFactory) {
                return sourceJacksonType;
            }

            @Override
            public JavaType getOutputType(TypeFactory typeFactory) {
                return targetJacksonType;
            }
        };
    }

    private static tools.jackson.databind.util.Converter<?, ?> toInput(ConverterMetadata metadata) {
        JavaType sourceJacksonType = JacksonUtilsV3.getJacksonType(metadata.getSourceType());
        JavaType targetJacksonType = JacksonUtilsV3.getJacksonType(metadata.getTargetType());
        Converter<Object, Object> converter = metadata.getConverter();

        return new tools.jackson.databind.util.Converter<Object, Object>() {

            @Override
            public Object convert(final SerializationContext ctxt, final Object value) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Object convert(final DeserializationContext ctxt, final Object value) {
                return converter.input(value);
            }

            @Override
            public JavaType getInputType(TypeFactory typeFactory) {
                return targetJacksonType;
            }

            @Override
            public JavaType getOutputType(TypeFactory typeFactory) {
                return sourceJacksonType;
            }
        };
    }
}
