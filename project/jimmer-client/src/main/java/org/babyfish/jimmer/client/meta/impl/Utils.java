package org.babyfish.jimmer.client.meta.impl;

import org.babyfish.jimmer.client.meta.Type;

import javax.validation.constraints.Null;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;

class Utils {

    private static final String SPRING_NULLABLE = "org.springframework.lang.Nullable";

    private Utils() {}

    public static Type wrap(Context ctx, Type type, AnnotatedElement annotatedElement) {
        if (annotatedElement.isAnnotationPresent(Null.class)) {
            return NullableTypeImpl.of(type);
        }
        if (Arrays.stream(annotatedElement.getAnnotations()).anyMatch(it -> it.annotationType().getName().equals(SPRING_NULLABLE))) {
            return NullableTypeImpl.of(type);
        }
        if (annotatedElement instanceof Field) {
            Field field = (Field) annotatedElement;
            JetBrainsNullity nullity = ctx.getJetBrainsNullity(field.getDeclaringClass());
            if (nullity.isNull(field.getName())) {
                return NullableTypeImpl.of(type);
            }
        } else if (annotatedElement instanceof Method) {
            Method method = (Method) annotatedElement;
            JetBrainsNullity nullity = ctx.getJetBrainsNullity(method.getDeclaringClass());
            if (nullity.isNull(method.getName())) {
                return NullableTypeImpl.of(type);
            }
        } else if (annotatedElement instanceof Parameter) {
            Parameter parameter = (Parameter) annotatedElement;
            Method method = (Method) parameter.getDeclaringExecutable();
            Parameter[] parameters = method.getParameters();
            int index = 0;
            while (index < parameters.length) {
                if (parameters[index] == parameter) {
                    JetBrainsNullity nullity = ctx.getJetBrainsNullity(method.getDeclaringClass());
                    if (nullity.isNull(method.getName(), index)) {
                        return NullableTypeImpl.of(type);
                    }
                    break;
                }
                index++;
            }
        }
        return type;
    }
}
