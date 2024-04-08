package org.babyfish.jimmer.jackson;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.introspect.AnnotatedClass;
import com.fasterxml.jackson.databind.type.TypeFactory;
import org.babyfish.jimmer.Draft;
import org.babyfish.jimmer.JimmerVersion;
import org.babyfish.jimmer.impl.util.StringUtil;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;

import javax.lang.model.element.Element;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.function.Function;

class ImmutableAnnotationIntrospector extends AnnotationIntrospector {

    @Override
    public Version version() {
        return new Version(JimmerVersion.major, JimmerVersion.minor, JimmerVersion.patch, null);
    }

    @Override
    public Class<?> findPOJOBuilder(AnnotatedClass ac) {
        ImmutableType type = ImmutableType.tryGet(ac.getAnnotated());
        if (type == null) {
            return super.findPOJOBuilder(ac);
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
    public JsonPOJOBuilder.Value findPOJOBuilderConfig(AnnotatedClass ac) {
        Class<?> declaringType = ac.getAnnotated().getDeclaringClass();
        if (declaringType != null && Draft.class.isAssignableFrom(declaringType)) {
            return new JsonPOJOBuilder.Value("build", "");
        }
        return super.findPOJOBuilderConfig(ac);
    }

    @Override
    public Object findSerializationConverter(Annotated a) {
        AnnotatedElement element = a.getAnnotated();
        if (element instanceof Method) {
            Method method = (Method) element;
            ImmutableType type = ImmutableType.tryGet(method.getDeclaringClass());
            if (type != null) {
                String propName = StringUtil.propName(method.getName(), method.getReturnType() == boolean.class);
                if (propName == null) {
                    propName = method.getName();
                }
                ImmutableProp prop = type.getProp(propName);
                ConverterMetadata metadata = prop.getConverterMetadata();
                if (metadata != null) {
                    return toOutput(metadata);
                }
            }
        }
        return super.findSerializationConverter(a);
    }

    @Override
    public Object findDeserializationConverter(Annotated a) {
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
        return super.findDeserializationConverter(a);
    }

    private static com.fasterxml.jackson.databind.util.Converter<?, ?> toOutput(ConverterMetadata metadata) {
        return new com.fasterxml.jackson.databind.util.Converter<Object, Object>() {
            @Override
            public Object convert(Object value) {
                return metadata.getConverter().output(value);
            }

            @Override
            public JavaType getInputType(TypeFactory typeFactory) {
                return metadata.getSourceJacksonType();
            }

            @Override
            public JavaType getOutputType(TypeFactory typeFactory) {
                return metadata.getTargetJacksonType();
            }
        };
    }

    private static com.fasterxml.jackson.databind.util.Converter<?, ?> toInput(ConverterMetadata metadata) {
        return new com.fasterxml.jackson.databind.util.Converter<Object, Object>() {
            @Override
            public Object convert(Object value) {
                return metadata.getConverter().input(value);
            }

            @Override
            public JavaType getInputType(TypeFactory typeFactory) {
                return metadata.getTargetJacksonType();
            }

            @Override
            public JavaType getOutputType(TypeFactory typeFactory) {
                return metadata.getSourceJacksonType();
            }
        };
    }
}
