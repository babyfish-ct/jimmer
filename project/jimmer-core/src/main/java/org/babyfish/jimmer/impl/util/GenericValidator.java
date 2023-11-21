package org.babyfish.jimmer.impl.util;

import org.apache.commons.lang3.reflect.TypeUtils;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ModelException;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Map;
import java.util.TreeMap;

public class GenericValidator {

    private final ImmutableProp prop;

    private final Class<? extends Annotation> annotationType;

    private final Class<?> configImplType;

    private final Class<?> configItfType;

    private final TypeVariable<?>[] parameters;

    private final Map<Integer, Expected> expectedMap = new TreeMap<>();

    public GenericValidator(ImmutableProp prop, Class<? extends Annotation> annotationType, Class<?> configImplType, Class<?> configItfType) {
        if (configImplType.isInterface() || !configItfType.isAssignableFrom(configImplType)) {
            ex("it is not a class implements \"" + configItfType.getName() + "\"");
        }
        if (configImplType.getTypeParameters().length != 0) {
            ex("it has type parameters");
        }
        TypeVariable<?>[] parameters = configItfType.getTypeParameters();
        this.prop = prop;
        this.annotationType = annotationType;
        this.configImplType = configImplType;
        this.configItfType = configItfType;
        this.parameters = parameters;
    }

    public GenericValidator expect(int typeParameterIndex, Type type) {
        return expect(typeParameterIndex, type, false);
    }

    public GenericValidator expect(int typeParameterIndex, Type type, boolean allowSubType) {
        if (typeParameterIndex < 0 || typeParameterIndex >= parameters.length) {
            throw new IllegalArgumentException(
                    "The argument \"genericParameterIndex\" must between 0 and " +
                            (parameters.length - 1)
            );
        }
        if (type instanceof Class<?>) {
            Class<?> clazz = (Class<?>) type;
            type = Classes.boxTypeOf(clazz);
        }
        expectedMap.put(
                typeParameterIndex,
                new Expected(typeParameterIndex, type, allowSubType)
        );
        return this;
    }

    public void validate() {
        Map<TypeVariable<?>, Type> argumentMap = TypeUtils.getTypeArguments(configImplType, configItfType);
        for (Expected expected : expectedMap.values()) {
            Type actualType = argumentMap.get(parameters[expected.typeParameterIndex]);
            if (actualType == null) {
                ex(
                        "\"that type does not specify type arguments for \"" +
                                configItfType.getName() +
                                "\""
                );
            }
            if (expected.allowSubType) {
                if (!TypeUtils.isAssignable(actualType, expected.type)) {
                    ex(
                            "that type specifies the type arguments[" +
                                    expected.typeParameterIndex +
                                    "] of \"" +
                                    configItfType.getName() +
                                    "\" as \"" +
                                    actualType +
                                    "\" which is cannot be assigned to the type \"" +
                                    expected.type +
                                    "\""
                    );
                }
            } else if (!expected.type.equals(actualType)) {
                ex(
                        "that type specifies the type arguments[" +
                                expected.typeParameterIndex +
                                "] of \"" +
                                configItfType.getName() +
                                "\" as \"" +
                                actualType +
                                "\" which is not the expected type \"" +
                                expected.type +
                                "\""
                );
            }
        }
    }

    private void ex(String message) {
        throw new ModelException(
                "Illegal property \"" +
                        prop +
                        "\", it does not accept the type \"" +
                        configImplType.getName() +
                        "\" configured by the annotation \"@" +
                        annotationType.getName() +
                        "\", " +
                        message
        );
    }

    private static class Expected {

        final int typeParameterIndex;

        final Type type;

        final boolean allowSubType;

        private Expected(int typeParameterIndex, Type type, boolean allowSubType) {
            this.typeParameterIndex = typeParameterIndex;
            this.type = type;
            this.allowSubType = allowSubType;
        }
    }
}
