package org.babyfish.jimmer.client.meta.impl;

import org.babyfish.jimmer.client.IllegalDocMetaException;
import org.babyfish.jimmer.client.meta.EnumBasedError;
import org.babyfish.jimmer.client.meta.Type;
import org.babyfish.jimmer.error.ErrorFamily;
import org.babyfish.jimmer.error.ErrorField;
import org.babyfish.jimmer.error.ErrorFields;
import org.babyfish.jimmer.impl.util.StaticCache;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
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

    public List<EnumBasedError> getErrors(Method operationMethod) {
        List<EnumBasedError> errors = new ArrayList<>();
        for (Annotation annotation : operationMethod.getAnnotations()) {
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
                    errors.add(parseError((Enum<?>) o));
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

    private EnumBasedError parseError(Enum<?> error) {
        Field constantField;
        Map<String, EnumBasedError.Field> fieldMap = new LinkedHashMap<>();
        try {
            constantField = error.getClass().getField(error.name());
        } catch (NoSuchFieldException ex) {
            throw new AssertionError(
                    "Cannot get field of \"" +
                            error.name() +
                            "\" from \"" +
                            error.getClass() +
                            "\""
            );
        }
        ErrorFields fields = constantField.getAnnotation(ErrorFields.class);
        if (fields != null) {
            for (ErrorField field : fields.value()) {
                if (fieldMap.put(field.name(), parseErrorField(error, field)) != null) {
                    throw new IllegalArgumentException(
                            "Duplicated field name \"" +
                                    field.name() +
                                    "\" is declared on \"" +
                                    error.getClass().getName() +
                                    "." +
                                    error.name() +
                                    "\""
                    );
                }
            }
        } else {
            ErrorField field = constantField.getAnnotation(ErrorField.class);
            if (field != null) {
                fieldMap.put(field.name(), parseErrorField(error, field));
            }
        }
        return new EnumBasedError(error, fieldMap);
    }

    private EnumBasedError.Field parseErrorField(Enum<?> error, ErrorField field) {
        Type type;
        try {
            type = ctx.parseErrorFieldType(field.type());
        } catch (IllegalArgumentException ex) {
            throw new IllegalDocMetaException(
                    "Cannot parse the field \"" +
                            field.name() +
                            "\" of \"" +
                            error.getClass().getName() +
                            "." +
                            error.name() +
                            "\". " +
                            ex.getMessage()
            );
        }
        return new EnumBasedError.Field(field.name(), type);
    }
}
