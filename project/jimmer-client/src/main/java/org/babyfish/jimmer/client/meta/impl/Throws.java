package org.babyfish.jimmer.client.meta.impl;

import org.babyfish.jimmer.client.ThrowsAll;
import org.babyfish.jimmer.client.meta.EnumBasedError;
import org.babyfish.jimmer.error.ErrorFamily;
import org.babyfish.jimmer.impl.util.StaticCache;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

public class Throws {

    // AnnotationType -> value method
    private static StaticCache<Class<?>, Method> ANNOTATION_VALUE_METHOD_CACHE = new StaticCache<>(
            Throws::parseAnnotationValueMethod, true
    );

    private final Context ctx;

    public Throws(Context ctx) {
        this.ctx = ctx;
    }

    public Collection<EnumBasedError> getErrors(Method operationMethod) {
        Collection<EnumBasedError> errors = new LinkedHashSet<>();
        ThrowsAll throwsAll = operationMethod.getAnnotation(ThrowsAll.class);
        if (throwsAll != null) {
            for (Enum<?> constant : throwsAll.value().getEnumConstants()) {
                errors.add(ctx.getError(constant));
            }
        }
        for (Annotation annotation : operationMethod.getAnnotations()) {
            if (annotation.annotationType() == ThrowsAll.class) {
                continue;
            }
            Method valueMethod = ANNOTATION_VALUE_METHOD_CACHE.get(annotation.annotationType());
            if (valueMethod != null) {
                Object[] arr;
                try {
                    arr = (Object[]) valueMethod.invoke(annotation);
                } catch (IllegalAccessException ex) {
                    throw new AssertionError(
                            "Internal bug, can not get error code from \"" +
                                    valueMethod +
                                    "\"",
                            ex
                    );
                } catch (InvocationTargetException ex) {
                    throw new AssertionError(
                            "Internal bug, can not get error code from \"" +
                                    valueMethod +
                                    "\"",
                            ex.getTargetException()
                    );
                }
                for (Object o : arr) {
                    errors.add(ctx.getError((Enum<?>) o));
                }
            }
        }
        return errors;
    }

    private static Method parseAnnotationValueMethod(Class<?> annotationType) {
        Method valueMethod;
        try {
            valueMethod = annotationType.getMethod("value");
        } catch (NoSuchMethodException ex) {
            return null;
        }
        if (!valueMethod.getReturnType().isArray()) {
            return null;
        }
        Class<?> errorType = valueMethod.getReturnType().getComponentType();
        if (!errorType.isEnum() || !errorType.isAnnotationPresent(ErrorFamily.class)) {
            return null;
        }
        return valueMethod;
    }
}
