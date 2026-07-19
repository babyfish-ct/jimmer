package org.babyfish.jimmer.jackson.v3;

import org.babyfish.jimmer.Draft;
import org.babyfish.jimmer.JimmerVersion;
import org.babyfish.jimmer.impl.util.StringUtil;
import org.babyfish.jimmer.jackson.Converter;
import org.babyfish.jimmer.jackson.ConverterMetadata;
import org.babyfish.jimmer.jackson.ImmutableProps;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import tools.jackson.core.Version;
import tools.jackson.databind.AnnotationIntrospector;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.annotation.JsonPOJOBuilder;
import tools.jackson.databind.cfg.MapperConfig;
import tools.jackson.databind.introspect.Annotated;
import tools.jackson.databind.introspect.AnnotatedClass;
import tools.jackson.databind.type.TypeFactory;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;

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
    public Class<?> findPOJOBuilder(final MapperConfig<?> config, final AnnotatedClass ac) {
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
        if (declaringType != null && Draft.class.isAssignableFrom(declaringType)) {
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
                ImmutableProp prop = ImmutableProps.get(type, method);
                ConverterMetadata metadata = prop.getConverterMetadata();
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
