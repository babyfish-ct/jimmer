package org.babyfish.jimmer.apt.generator;

import com.squareup.javapoet.*;
import org.babyfish.jimmer.ImmutableObjects;
import org.babyfish.jimmer.UnloadedException;
import org.babyfish.jimmer.apt.TypeUtils;
import org.babyfish.jimmer.apt.meta.ImmutableProp;
import org.babyfish.jimmer.apt.meta.ImmutableType;

import javax.lang.model.element.Modifier;
import javax.lang.model.type.PrimitiveType;
import java.util.Objects;

public class ImplGenerator {

    private TypeUtils typeUtils;

    private ImmutableType type;

    private TypeSpec.Builder typeBuilder;

    private ClassName unloadedExceptionClassName;

    public ImplGenerator(TypeUtils typeUtils, ImmutableType type) {
        this.typeUtils = typeUtils;
        this.type = type;
        unloadedExceptionClassName = ClassName.get(UnloadedException.class);
    }

    public void generate(TypeSpec.Builder parentBuilder) {
        typeBuilder = TypeSpec.classBuilder("Impl")
                .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
                .superclass(type.getImplementorClassName());
        addFields();
        addConstructor();
        for (ImmutableProp prop : type.getProps().values()) {
            addGetter(prop);
        }
        addIsLoaded();
        addHashCode(false);
        addHashCode(true);
        addParameterizedHashCode();
        addEquals(false);
        addEquals(true);
        addParameterizedEquals();
        addToString();
        parentBuilder.addType(typeBuilder.build());
    }

    private void addFields() {
        for (ImmutableProp prop : type.getProps().values()) {
            FieldSpec.Builder valueBuilder = FieldSpec.builder(
                    TypeName.get(prop.getReturnType()),
                    prop.getName()
            );
            typeBuilder.addField(valueBuilder.build());
            if (prop.isLoadedStateRequired()) {
                FieldSpec.Builder stateBuilder = FieldSpec.builder(
                        boolean.class,
                        prop.getLoadedStateName()
                );
                typeBuilder.addField(stateBuilder.build());
            }
        }
    }

    private void addConstructor() {
        MethodSpec.Builder builder = MethodSpec.constructorBuilder();
        builder.addParameter(type.getClassName(), "base");
        builder.beginControlFlow("if (base != null)");
        builder.addStatement("Implementor from = (Implementor)base");
        for (ImmutableProp prop : type.getProps().values()) {
            if (prop.isLoadedStateRequired()) {
                builder.addStatement("$L = from.__isLoaded($S)", prop.getLoadedStateName(), prop.getName());
                builder.beginControlFlow("if ($L)", prop.getLoadedStateName());
            } else {
                builder.beginControlFlow("if (from.__isLoaded($S))", prop.getName());
            }
            builder.addStatement("$L = from.$L()", prop.getName(), prop.getGetterName());
            builder.endControlFlow();
        }
        builder.endControlFlow();
        typeBuilder.addMethod(builder.build());
    }

    private void addGetter(ImmutableProp prop) {
        MethodSpec.Builder builder = MethodSpec
                .methodBuilder(prop.getGetterName())
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .returns(TypeName.get(prop.getReturnType()));
        if (prop.isLoadedStateRequired()) {
            builder.beginControlFlow("if (!$L)", prop.getLoadedStateName());
        } else {
            builder.beginControlFlow("if ($L == null)", prop.getName());
        }
        builder.addStatement(
                        "throw new $T($T.class, $S)",
                        unloadedExceptionClassName,
                        type.getClassName(),
                        prop.getName()
                )
                .endControlFlow()
                .addStatement("return $L", prop.getName());
        typeBuilder.addMethod(builder.build());
    }

    private void addIsLoaded() {
        MethodSpec.Builder builder = MethodSpec
                .methodBuilder("__isLoaded")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .addParameter(String.class, "prop")
                .returns(boolean.class);
        builder.beginControlFlow("switch (prop)");
        for (ImmutableProp prop : type.getProps().values()) {
            if (prop.isLoadedStateRequired()) {
                builder.addStatement("case $S: return $L", prop.getName(), prop.getLoadedStateName());
            } else {
                builder.addStatement("case $S: return $L != null", prop.getName(), prop.getName());
            }
        }
        builder.addStatement(
                "default: throw new IllegalArgumentException($S + prop + $S)",
                "Illegal property name: \"",
                "\""
        );
        builder.endControlFlow();
        typeBuilder.addMethod(builder.build());
    }

    private void addHashCode(Boolean shallow) {
        MethodSpec.Builder builder = MethodSpec
                .methodBuilder(shallow ? "__shallowHashCode" : "hashCode")
                .addModifiers(shallow ? Modifier.PRIVATE : Modifier.PUBLIC)
                .returns(int.class)
                .addStatement("int hash = 1");
        if (!shallow) {
            builder.addAnnotation(Override.class);
        }
        for (ImmutableProp prop : type.getProps().values()) {
            Class<?> boxType = prop.getBoxType();
            if (boxType != null) {
                builder.beginControlFlow("if ($L)", prop.getLoadedStateName());
                builder.addStatement("hash = 31 * hash + $T.hashCode($L)", boxType, prop.getName());
                builder.endControlFlow();
            } else if (shallow) {
                if (prop.isLoadedStateRequired()) {
                    builder.beginControlFlow("if ($L)", prop.getLoadedStateName());
                } else {
                    builder.beginControlFlow("if ($L != null)", prop.getName());
                }
                builder.addStatement("hash = 31 * hash + $T.identityHashCode($L)", System.class, prop.getName());
                builder.endControlFlow();
            } else {
                if (prop.isLoadedStateRequired()) {
                    builder.beginControlFlow(
                            "if ($L && $L != null)",
                            prop.getLoadedStateName(),
                            prop.getName()
                    );
                } else {
                    builder.beginControlFlow(
                            "if ($L != null)",
                            prop.getName()
                    );
                }
                builder.addStatement("hash = 31 * hash + $L.hashCode()", prop.getName());
                builder.endControlFlow();
            }
        }
        builder.addStatement("return hash");
        typeBuilder.addMethod(builder.build());
    }

    private void addEquals(Boolean shallow) {
        MethodSpec.Builder builder = MethodSpec
                .methodBuilder(shallow ? "__shallowEquals" : "equals")
                .addModifiers(shallow ? Modifier.PRIVATE : Modifier.PUBLIC)
                .addParameter(Object.class, "obj")
                .returns(boolean.class);
        if (!shallow) {
            builder.addAnnotation(Override.class);
        }
        builder
                .beginControlFlow("if (obj == null || getClass() != obj.getClass())")
                .addStatement("return false")
                .endControlFlow()
                .addStatement("Implementor other = (Implementor)obj");
        for (ImmutableProp prop : type.getProps().values()) {
            if (prop.isLoadedStateRequired()) {
                builder.addStatement("boolean __$LLoaded = $L", prop.getName(), prop.getLoadedStateName());
            } else {
                builder.addStatement("boolean __$LLoaded = $L != null", prop.getName(), prop.getName());
            }
            builder
                    .beginControlFlow(
                            "if (__$LLoaded != other.__isLoaded($S))",
                            prop.getName(),
                            prop.getName()
                    )
                    .addStatement("return false")
                    .endControlFlow();
            if (shallow || prop.getReturnType() instanceof PrimitiveType) {
                builder
                        .beginControlFlow(
                                "if (__$LLoaded && $L != other.$L())",
                                prop.getName(),
                                prop.getName(),
                                prop.getGetterName()
                        )
                        .addStatement("return false")
                        .endControlFlow();
            } else {
                builder
                        .beginControlFlow(
                                "if (__$LLoaded && $T.equals($L, other.$L()))",
                                prop.getName(),
                                Objects.class,
                                prop.getName(),
                                prop.getGetterName()
                        )
                        .addStatement("return false")
                        .endControlFlow();
            }
        }
        builder.addStatement("return true");
        typeBuilder.addMethod(builder.build());
    }

    private void addParameterizedHashCode() {
        MethodSpec.Builder builder = MethodSpec
                .methodBuilder("__hashCode")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .addParameter(boolean.class, "shallow")
                .returns(int.class)
                .addCode("return shallow ? __shallowHashCode() : hashCode();");
        typeBuilder.addMethod(builder.build());
    }

    private void addParameterizedEquals() {
        MethodSpec.Builder builder = MethodSpec
                .methodBuilder("__equals")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .addParameter(Object.class, "obj")
                .addParameter(boolean.class, "shallow")
                .returns(boolean.class)
                .addCode("return shallow ? __shallowEquals(obj) : equals(obj);");
        typeBuilder.addMethod(builder.build());
    }

    private void addToString() {
        MethodSpec.Builder builder = MethodSpec
                .methodBuilder("toString")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .returns(String.class)
                .addStatement("return $T.toString(this)", ImmutableObjects.class);
        typeBuilder.addMethod(builder.build());
    }
}
