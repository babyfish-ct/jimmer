package org.babyfish.jimmer.jackson;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.type.ArrayType;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.databind.type.MapType;
import com.fasterxml.jackson.databind.type.SimpleType;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ModelException;
import org.babyfish.jimmer.meta.TargetLevel;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;

public class JacksonUtils {

    public static JavaType getJacksonType(ImmutableProp prop) {
        ConverterMetadata metadata = prop.getConverterMetadata();
        if (metadata != null) {
            return metadata.getTargetJacksonType();
        }
        if (prop.isReferenceList(TargetLevel.OBJECT) || prop.isScalarList()) {
            return CollectionType.construct(
                    List.class,
                    null,
                    null,
                    null,
                    SimpleType.constructUnsafe(prop.getElementClass())
            );
        }
        try {
            return getJacksonType(prop.getGenericType());
        } catch (RuntimeException ex) {
            throw new ModelException(
                    "Illegal property \"" +
                            "prop" +
                            "\", cannot create jackson property: " +
                            ex.getMessage(),
                    ex
            );
        }
    }

    public static JavaType getJacksonType(Type type) {
        if (type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) type;
            Type rawType = parameterizedType.getRawType();
            if (rawType == Collection.class || rawType == List.class || rawType == Set.class) {
                return CollectionType.construct(
                        (Class<?>) rawType,
                        null,
                        null,
                        null,
                        getJacksonType(parameterizedType.getActualTypeArguments()[0])
                );
            }
            if (rawType == Map.class) {
                return MapType.construct(
                        (Class<?>) rawType,
                        null,
                        null,
                        null,
                        getJacksonType(parameterizedType.getActualTypeArguments()[0]),
                        getJacksonType(parameterizedType.getActualTypeArguments()[1])
                );
            }
            throw new IllegalArgumentException("Parameterized type must be collection, list, set or map");
        }
        if (type instanceof TypeVariable<?>) {
            return getJacksonType(((TypeVariable<?>)type).getBounds()[0]);
        }
        if (type instanceof WildcardType) {
            return getJacksonType(((WildcardType)type).getUpperBounds()[0]);
        }
        if (type instanceof GenericArrayType) {
            GenericArrayType arrType = (GenericArrayType) type;
            return ArrayType.construct(
                    getJacksonType(arrType.getGenericComponentType()),
                    null
            );
        }
        Class<?> clazz = (Class<?>) type;
        if (clazz.isArray()) {
            return ArrayType.construct(
                    getJacksonType(clazz.getComponentType()),
                    null
            );
        }
        return SimpleType.constructUnsafe(clazz);
    }

    public static <A extends Annotation> A getAnnotation(ImmutableProp prop, Class<A> annotationType) {
        AnnotationSearchContext<A> ctx = new AnnotationSearchContext<>(prop, annotationType);
        for (Annotation annotation : prop.getAnnotations()) {
            collectAnnotationType(annotation, ctx);
        }
        return ctx.getAnnotation();
    }

    @SuppressWarnings("unchecked")
    private static <A extends Annotation> void collectAnnotationType(
            Annotation annotation,
            AnnotationSearchContext<A> ctx
    ) {
        Class<? extends Annotation> curAnnotationType = annotation.annotationType();
        if (curAnnotationType == ctx.annotationType) {
            ctx.set((A)annotation);
            return;
        }
        if (!ctx.push(curAnnotationType)) {
            return;
        }
        for (Annotation deeperAnnotation : curAnnotationType.getAnnotations()) {
            collectAnnotationType(deeperAnnotation, ctx);
        }
        ctx.pop();
    }

    private static class AnnotationSearchContext<A extends Annotation> {

        private final ImmutableProp prop;

        final Class<A> annotationType;

        private final Deque<Class<? extends Annotation>> pathStack = new ArrayDeque<>();

        private ArrayList<Class<? extends Annotation>> path;

        private A annotation;

        private AnnotationSearchContext(ImmutableProp prop, Class<A> annotationType) {
            this.prop = prop;
            this.annotationType = annotationType;
        }

        public boolean push(Class<? extends Annotation> annotationType) {
            if (pathStack.contains(annotationType)) {
                return false;
            }
            pathStack.push(annotationType);
            return true;
        }

        public void pop() {
            pathStack.pop();
        }

        public A getAnnotation() {
            return annotation;
        }

        public void set(A annotation) {
            if (this.annotation != null && !this.annotation.equals(annotation)) {
                throw new ModelException(
                        "Illegal property \"" +
                                prop +
                                "\", conflict annotation \"@" +
                                annotation.annotationType().getName() +
                                "\", one " +
                                declaredPath(path) +
                                " and the other one " +
                                declaredPath(pathStack)
                );
            }
            if (this.annotation == null) {
                path = new ArrayList<>(pathStack);
                this.annotation = annotation;
            }
        }

        private static String declaredPath(Collection<Class<? extends Annotation>> path) {
            if (path.isEmpty()) {
                return "is declared directly";
            }
            return "is declared as nest annotation: " + path;
        }
    }
}
