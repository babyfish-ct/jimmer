package org.babyfish.jimmer.apt.generator;

import com.squareup.javapoet.*;
import org.babyfish.jimmer.UnloadedException;
import org.babyfish.jimmer.apt.meta.ImmutableProp;
import org.babyfish.jimmer.apt.meta.ImmutableType;
import org.babyfish.jimmer.runtime.NonSharedList;
import org.babyfish.jimmer.sql.Id;

import javax.lang.model.element.Modifier;
import javax.lang.model.type.PrimitiveType;
import java.util.Objects;

public class ImplGenerator {

    private final ImmutableType type;

    private final ClassName unloadedExceptionClassName;

    private TypeSpec.Builder typeBuilder;

    public ImplGenerator(ImmutableType type) {
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
        addIsLoaded(int.class);
        addIsLoaded(String.class);
        addHashCode(false);
        addHashCode(true);
        addParameterizedHashCode();
        addEquals(false);
        addEquals(true);
        addParameterizedEquals();
        parentBuilder.addType(typeBuilder.build());
    }

    private void addFields() {
        for (ImmutableProp prop : type.getProps().values()) {
            FieldSpec.Builder valueBuilder = FieldSpec.builder(
                    prop.isList() ?
                            ParameterizedTypeName.get(
                                    ClassName.get(NonSharedList.class),
                                    prop.getElementTypeName()
                            ) :
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
                builder.addStatement("$L = from.__isLoaded($L)", prop.getLoadedStateName(), prop.getId());
                builder.beginControlFlow("if ($L)", prop.getLoadedStateName());
            } else {
                builder.beginControlFlow("if (from.__isLoaded($L))", prop.getId());
            }
            if (prop.isList()) {
                builder.addStatement(
                        "$L = $T.of(null, from.$L())",
                        prop.getName(),
                        NonSharedList.class,
                        prop.getGetterName()
                );
            } else {
                builder.addStatement("$L = from.$L()", prop.getName(), prop.getGetterName());
            }
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

    private void addIsLoaded(Class<?> argType) {
        MethodSpec.Builder builder = MethodSpec
                .methodBuilder("__isLoaded")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .addParameter(argType, "prop")
                .returns(boolean.class);
        builder.beginControlFlow("switch (prop)");
        for (ImmutableProp prop : type.getProps().values()) {
            Object arg = argType == int.class ? prop.getId() : '"' + prop.getName() + '"';
            if (prop.isLoadedStateRequired()) {
                builder.addStatement("case $L: return $L", arg, prop.getLoadedStateName());
            } else {
                builder.addStatement("case $L: return $L != null", arg, prop.getName());
            }
        }
        builder.addStatement(
                "default: throw new IllegalArgumentException($S + prop + $S)",
                "Illegal property " +
                        (argType == int.class ? "id" : "name") +
                        ": \"",
                        "\""
        );
        builder.endControlFlow();
        typeBuilder.addMethod(builder.build());
    }

    private void addHashCode(boolean shallow) {
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
                if (!shallow) {
                    if (prop.getAnnotation(Id.class) != null) {
                        builder.addComment("If entity-id is loaded, return directly");
                        builder.addStatement("return hash");
                    }
                }
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
                if (prop.getAnnotation(Id.class) != null) {
                    builder.addComment("If entity-id is loaded, return directly");
                    builder.addStatement("return hash");
                }
                builder.endControlFlow();
            }
        }
        builder.addStatement("return hash");
        typeBuilder.addMethod(builder.build());
    }

    private void addEquals(boolean shallow) {
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
                            "if (__$LLoaded != other.__isLoaded($L))",
                            prop.getName(),
                            prop.getId()
                    )
                    .addStatement("return false")
                    .endControlFlow();
            if (shallow || prop.getReturnType() instanceof PrimitiveType) {
                if (!shallow && prop.getAnnotation(Id.class) != null) {
                    builder
                            .beginControlFlow("if (__$LLoaded)", prop.getName())
                            .addComment("If entity-id is loaded, return directly")
                            .addStatement("return $L == other.$L()", prop.getName(), prop.getGetterName())
                            .endControlFlow();
                } else {
                    builder
                            .beginControlFlow(
                                    "if (__$LLoaded && $L != other.$L())",
                                    prop.getName(),
                                    prop.getName(),
                                    prop.getGetterName()
                            )
                            .addStatement("return false")
                            .endControlFlow();
                }
            } else if (prop.getAnnotation(Id.class) != null) {
                builder
                        .beginControlFlow(
                                "if (__$LLoaded)",
                                prop.getName()
                        )
                        .addComment("If entity-id is loaded, return directly")
                        .addStatement(
                                "return $T.equals($L, other.$L())",
                                Objects.class,
                                prop.getName(),
                                prop.getGetterName()
                        )
                        .endControlFlow();
            } else {
                builder
                        .beginControlFlow(
                                "if (__$LLoaded && !$T.equals($L, other.$L()))",
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
}
