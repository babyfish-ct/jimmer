package org.babyfish.jimmer.apt.generator;

import com.squareup.javapoet.*;
import org.babyfish.jimmer.ImmutableObjects;
import org.babyfish.jimmer.UnloadedException;
import org.babyfish.jimmer.apt.meta.ImmutableProp;
import org.babyfish.jimmer.apt.meta.ImmutableType;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.runtime.NonSharedList;
import org.babyfish.jimmer.sql.Id;
import org.jetbrains.annotations.Nullable;

import javax.lang.model.element.Modifier;
import javax.lang.model.type.PrimitiveType;
import java.util.ArrayList;
import java.util.Objects;

import static org.babyfish.jimmer.apt.generator.Constants.*;

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
                .addSuperinterface(type.getImplementorClassName())
                .addSuperinterface(CLONEABLE_CLASS_NAME);
        addFields();
        addConstructor();
        for (ImmutableProp prop : type.getProps().values()) {
            addGetter(prop);
        }
        addClone();
        addIsLoaded(int.class);
        addIsLoaded(String.class);
        addIsVisible(int.class);
        addIsVisible(String.class);
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
        typeBuilder.addField(
                FieldSpec
                        .builder(VISIBILITY_CLASS_NAME, "__visibility")
                        .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
                        .initializer("$T.of($L)", VISIBILITY_CLASS_NAME, type.getProps().size())
                        .build()
        );
        for (ImmutableProp prop : type.getProps().values()) {
            if (prop.isValueRequired()) {
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
            }
            if (prop.isLoadedStateRequired()) {
                FieldSpec.Builder stateBuilder = FieldSpec.builder(
                        boolean.class,
                        prop.getLoadedStateName()
                ).initializer("false");
                typeBuilder.addField(stateBuilder.build());
            }
        }
    }

    private void addConstructor() {
        if (type.getProps().values().stream().allMatch(ImmutableProp::isValueRequired)) {
            return;
        }
        MethodSpec.Builder builder = MethodSpec.constructorBuilder();
        for (ImmutableProp prop : type.getProps().values()) {
            if (!prop.isValueRequired()) {
                builder.addStatement("__visibility.show($L, false)", prop.getSlotName());
            }
        }
        typeBuilder.addMethod(builder.build());
    }

    private void addGetter(ImmutableProp prop) {
        if (prop.isJavaFormula() || prop.getManyToManyViewBaseProp() != null) {
            return;
        }

        MethodSpec.Builder builder = MethodSpec
                .methodBuilder(prop.getGetterName())
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .returns(TypeName.get(prop.getReturnType()));
        if (prop.isBeanStyle()) {
            builder.addAnnotation(JSON_IGNORE_CLASS_NAME);
        }
        if (prop.isNullable()) {
            builder.addAnnotation(Nullable.class);
        }

        ImmutableProp idViewBaseProp = prop.getIdViewBaseProp();
        if (idViewBaseProp != null) {
            if (idViewBaseProp.isList()) {
                builder.addStatement(
                        "$T<$T> __ids = new $T($L().size())",
                        LIST_CLASS_NAME,
                        idViewBaseProp.getTargetType().getIdProp().getTypeName().box(),
                        ArrayList.class,
                        idViewBaseProp.getGetterName()
                );
                builder.beginControlFlow(
                        "for ($T __target : $L())",
                        idViewBaseProp.getElementTypeName(),
                        idViewBaseProp.getGetterName()
                );
                builder.addStatement(
                        "__ids.add(__target.$L())",
                        idViewBaseProp.getTargetType().getIdProp().getGetterName()
                );
                builder.endControlFlow();
                builder.addStatement("return __ids");
            } else {
                builder.addStatement("$T __target = $L()", idViewBaseProp.getElementTypeName(), idViewBaseProp.getGetterName());
                builder.addStatement(
                        prop.isNullable() ?
                        "return __target != null ? __target.$L() : null" :
                        "return __target.$L()",
                        idViewBaseProp.getTargetType().getIdProp().getGetterName()
                );
            }
        } else {
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
                    .endControlFlow();
            builder.addStatement("return $L", prop.getName());
        }
        typeBuilder.addMethod(builder.build());
    }

    private void addClone() {
        MethodSpec.Builder builder = MethodSpec
                .methodBuilder("clone")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .returns(type.getImplClassName());
        builder
                .beginControlFlow("try")
                .addStatement("return ($T)super.clone()", type.getImplClassName())
                .nextControlFlow("catch($T ex)", Constants.CLONE_NOT_SUPPORTED_EXCEPTION_CLASS_NAME)
                .addStatement("throw new AssertionError(ex)")
                .endControlFlow();
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
        CaseAppender appender = new CaseAppender(builder, type, argType);
        for (ImmutableProp prop : type.getPropsOrderById()) {
            appender.addCase(prop);
            ImmutableProp idViewBaseProp = prop.getIdViewBaseProp();
            ImmutableProp manyToManyViewBaseProp = prop.getManyToManyViewBaseProp();
            if (idViewBaseProp != null) {
                if (idViewBaseProp.isList()) {
                    builder.addStatement(
                            "return __isLoaded($L) && $L().stream().allMatch(__each -> (($T)__each).__isLoaded($T.$L))",
                            idViewBaseProp.getSlotName(),
                            idViewBaseProp.getGetterName(),
                            ImmutableSpi.class,
                            idViewBaseProp.getTargetType().getProducerClassName(),
                            idViewBaseProp.getTargetType().getIdProp().getSlotName()
                    );
                } else {
                    builder.addStatement(
                            "return __isLoaded($L) && ($L() == null || (($T)$L()).__isLoaded($T.$L))",
                            idViewBaseProp.getSlotName(),
                            idViewBaseProp.getGetterName(),
                            ImmutableSpi.class,
                            idViewBaseProp.getGetterName(),
                            idViewBaseProp.getTargetType().getProducerClassName(),
                            idViewBaseProp.getTargetType().getIdProp().getSlotName()
                    );
                }
            } else if (manyToManyViewBaseProp != null) {
                builder.addStatement(
                        "return __isLoaded($L) && $L().stream().allMatch(__each -> (($T)__each).__isLoaded($T.$L))",
                        manyToManyViewBaseProp.getSlotName(),
                        manyToManyViewBaseProp.getGetterName(),
                        ImmutableSpi.class,
                        prop.getManyToManyViewBaseDeeperProp().getDeclaringType().getProducerClassName(),
                        prop.getManyToManyViewBaseDeeperProp().getSlotName()
                );
            } else if (prop.isJavaFormula()) {
                boolean first = true;
                builder.addCode("return $>");
                for (ImmutableProp dependency : prop.getDependencies()) {
                    if (first) {
                        first = false;
                    } else {
                        builder.addCode(" && \n");
                    }
                    builder.addCode("__isLoaded($L)", dependency.getSlotName());
                }
                builder.addStatement("$<");
            } else if (prop.isLoadedStateRequired()) {
                builder.addStatement("return $L", prop.getLoadedStateName());
            } else {
                builder.addStatement("return $L != null", prop.getName());
            }
        }
        builder.addStatement(
                "default: throw new IllegalArgumentException($S + prop + $S)",
                "Illegal property " +
                        (argType == int.class ? "id" : "name") +
                        " for \"" + type + "\": \"",
                        "\""
        );
        builder.endControlFlow();
        typeBuilder.addMethod(builder.build());
    }

    private void addIsVisible(Class<?> argType) {
        MethodSpec.Builder builder = MethodSpec
                .methodBuilder("__isVisible")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .addParameter(argType, "prop")
                .returns(boolean.class);
        builder.beginControlFlow("switch (prop)");
        CaseAppender appender = new CaseAppender(builder, type, argType);
        for (ImmutableProp prop : type.getPropsOrderById()) {
            appender.addCase(prop);
            builder.addStatement("return __visibility.visible($L)", prop.getSlotName());
        }
        builder.addStatement("default: return true");
        builder.endControlFlow();
        typeBuilder.addMethod(builder.build());
    }

    private void addHashCode(boolean shallow) {
        MethodSpec.Builder builder = MethodSpec
                .methodBuilder(shallow ? "__shallowHashCode" : "hashCode")
                .addModifiers(shallow ? Modifier.PRIVATE : Modifier.PUBLIC)
                .returns(int.class)
                .addStatement("int hash = __visibility.hashCode()");
        if (!shallow) {
            builder.addAnnotation(Override.class);
        }
        for (ImmutableProp prop : type.getProps().values()) {
            if (!prop.isValueRequired()) {
                continue;
            }
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
                .beginControlFlow("if (obj == null || !(obj instanceof $T))", type.getImplementorClassName())
                .addStatement("return false")
                .endControlFlow()
                .addStatement("$T __other = ($T)obj", type.getImplementorClassName(), type.getImplementorClassName());
        for (ImmutableProp prop : type.getProps().values()) {
            builder
                    .beginControlFlow(
                            "if (__visibility.visible($L) != __other.__isVisible($L))",
                            prop.getSlotName(),
                            prop.getSlotName()
                    )
                    .addStatement("return false")
                    .endControlFlow();
            if (!prop.isValueRequired()) {
                continue;
            }
            if (prop.isLoadedStateRequired()) {
                builder.addStatement("boolean __$L = $L", prop.getLoadedStateName(), prop.getLoadedStateName());
            } else {
                builder.addStatement("boolean __$L = $L != null", prop.getLoadedStateName(true), prop.getName());
            }
            builder
                    .beginControlFlow(
                            "if (__$L != __other.__isLoaded($L))",
                            prop.getLoadedStateName(true),
                            prop.getSlotName()
                    )
                    .addStatement("return false")
                    .endControlFlow();
            if (shallow || prop.getReturnType() instanceof PrimitiveType) {
                if (!shallow && prop.getAnnotation(Id.class) != null) {
                    builder
                            .beginControlFlow("if (__$L)", prop.getLoadedStateName(true))
                            .addComment("If entity-id is loaded, return directly")
                            .addStatement("return $L == __other.$L()", prop.getName(), prop.getGetterName())
                            .endControlFlow();
                } else {
                    builder
                            .beginControlFlow(
                                    "if (__$L && $L != __other.$L())",
                                    prop.getLoadedStateName(true),
                                    prop.getName(),
                                    prop.getGetterName()
                            )
                            .addStatement("return false")
                            .endControlFlow();
                }
            } else if (prop.getAnnotation(Id.class) != null) {
                builder
                        .beginControlFlow(
                                "if (__$L)",
                                prop.getLoadedStateName(true)
                        )
                        .addComment("If entity-id is loaded, return directly")
                        .addStatement(
                                "return $T.equals($L, __other.$L())",
                                Objects.class,
                                prop.getName(),
                                prop.getGetterName()
                        )
                        .endControlFlow();
            } else {
                builder
                        .beginControlFlow(
                                "if (__$L && !$T.equals($L, __other.$L()))",
                                prop.getLoadedStateName(true),
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
