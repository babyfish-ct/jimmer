package org.babyfish.jimmer.apt.immutable.generator;

import com.squareup.javapoet.*;
import org.babyfish.jimmer.ImmutableObjects;
import org.babyfish.jimmer.UnloadedException;
import org.babyfish.jimmer.apt.immutable.meta.FormulaDependency;
import org.babyfish.jimmer.apt.immutable.meta.ImmutableProp;
import org.babyfish.jimmer.apt.immutable.meta.ImmutableType;
import org.babyfish.jimmer.meta.PropId;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.runtime.NonSharedList;
import org.babyfish.jimmer.sql.Id;
import org.jetbrains.annotations.Nullable;

import javax.lang.model.element.Modifier;
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
                .addSuperinterface(type.getImplementorClassName())
                .addSuperinterface(Constants.CLONEABLE_CLASS_NAME)
                .addSuperinterface(Constants.SERIALIZABLE_CLASS_NAME);
        addFields();
        addConstructor();
        for (ImmutableProp prop : type.getProps().values()) {
            addGetter(prop);
        }
        addClone();
        addIsLoaded(PropId.class);
        addIsLoaded(String.class);
        addIsVisible(PropId.class);
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
                        .builder(Constants.VISIBILITY_CLASS_NAME, "__visibility")
                        .addModifiers(Modifier.PRIVATE)
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
                        prop.getValueName()
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
                builder.addStatement("__visibility = $T.of($L)", Constants.VISIBILITY_CLASS_NAME, type.getProps().size());
                break;
            }
        }
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
            builder.addAnnotation(Constants.JSON_IGNORE_CLASS_NAME);
        }
        if (prop.isNullable()) {
            builder.addAnnotation(Nullable.class);
        }

        ImmutableProp idViewBaseProp = prop.getIdViewBaseProp();
        if (idViewBaseProp != null) {
            if (idViewBaseProp.isList()) {
                builder.addStatement(
                        "return new $T<>($T.TYPE, $L())",
                        Constants.ID_VIEW_LIST_CLASS_NAME,
                        idViewBaseProp.getTargetType().getProducerClassName(),
                        idViewBaseProp.getGetterName()
                );
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
                builder.beginControlFlow("if ($L == null)", prop.getValueName());
            }
            builder.addStatement(
                            "throw new $T($T.class, $S)",
                            unloadedExceptionClassName,
                            type.getClassName(),
                            prop.getName()
                    )
                    .endControlFlow();
            builder.addStatement("return $L", prop.getValueName());
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
        CaseAppender appender = new CaseAppender(builder, type, argType);
        if (argType == PropId.class) {
            builder.addStatement("int __propIndex = prop.asIndex()");
            builder.beginControlFlow("switch (__propIndex)");
            appender.addIllegalCase();
            builder.addStatement("return __isLoaded(prop.asName())");
        } else {
            builder.beginControlFlow("switch (prop)");
        }
        for (ImmutableProp prop : type.getPropsOrderById()) {
            appender.addCase(prop);
            ImmutableProp idViewBaseProp = prop.getIdViewBaseProp();
            ImmutableProp manyToManyViewBaseProp = prop.getManyToManyViewBaseProp();
            if (idViewBaseProp != null) {
                if (idViewBaseProp.isList()) {
                    builder.addStatement(
                            "return __isLoaded($T.byIndex($L)) && $L().stream().allMatch(__each -> \n$>" +
                                    "(($T)__each).__isLoaded($T.byIndex($T.$L))" +
                                    "\n$<)",
                            Constants.PROP_ID_CLASS_NAME,
                            idViewBaseProp.getSlotName(),
                            idViewBaseProp.getGetterName(),
                            ImmutableSpi.class,
                            Constants.PROP_ID_CLASS_NAME,
                            idViewBaseProp.getTargetType().getProducerClassName(),
                            idViewBaseProp.getTargetType().getIdProp().getSlotName()
                    );
                } else {
                    builder.addStatement(
                            "return __isLoaded($T.byIndex($L)) && ($L() == null || \n\t(($T)$L()).__isLoaded($T.byIndex($T.$L)))",
                            Constants.PROP_ID_CLASS_NAME,
                            idViewBaseProp.getSlotName(),
                            idViewBaseProp.getGetterName(),
                            ImmutableSpi.class,
                            idViewBaseProp.getGetterName(),
                            Constants.PROP_ID_CLASS_NAME,
                            idViewBaseProp.getTargetType().getProducerClassName(),
                            idViewBaseProp.getTargetType().getIdProp().getSlotName()
                    );
                }
            } else if (manyToManyViewBaseProp != null) {
                builder.addStatement(
                        "return __isLoaded($T.byIndex($L)) && $L().stream().allMatch(__each -> \n$>" +
                                "(($T)__each).__isLoaded($L)" +
                                "$<\n)",
                        Constants.PROP_ID_CLASS_NAME,
                        manyToManyViewBaseProp.getSlotName(),
                        manyToManyViewBaseProp.getGetterName(),
                        ImmutableSpi.class,
                        prop.getDeeperPropIdName()
                );
            } else if (prop.isJavaFormula()) {
                boolean first = true;
                builder.addCode("return $>");
                for (FormulaDependency dependency : prop.getDependencies()) {
                    if (first) {
                        first = false;
                    } else {
                        builder.addCode(" && \n");
                    }
                    if (dependency.getProps().size() == 1) {
                        builder.addCode(
                                "__isLoaded($T.byIndex($L))",
                                Constants.PROP_ID_CLASS_NAME,
                                dependency.getProps().get(0).getSlotName()
                        );
                    } else {
                        builder.addCode("$T.isLoadedChain(this", Constants.IMMUTABLE_OBJECTS_CLASS_NAME);
                        for (ImmutableProp depProp : dependency.getProps()) {
                            builder.addCode(", ");
                            builder.addCode(
                                    "$T.byIndex($T.$L)",
                                    Constants.PROP_ID_CLASS_NAME,
                                    depProp.getDeclaringType().getProducerClassName(),
                                    depProp.getSlotName()
                            );
                        }
                        builder.addCode(")");
                    }
                }
                builder.addStatement("$<");
            } else if (prop.isLoadedStateRequired()) {
                builder.addStatement("return $L", prop.getLoadedStateName());
            } else {
                builder.addStatement("return $L != null", prop.getValueName());
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
        builder
                .beginControlFlow("if (__visibility == null)")
                .addStatement("return true")
                .endControlFlow();
        CaseAppender appender = new CaseAppender(builder, type, argType);
        if (argType == PropId.class) {
            builder.addStatement("int __propIndex = prop.asIndex()");
            builder.beginControlFlow("switch (__propIndex)");
            appender.addIllegalCase();
            builder.addStatement("return __isVisible(prop.asName())");
        } else {
            builder.beginControlFlow("switch (prop)");
        }
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
                .addStatement("int hash = __visibility != null ? __visibility.hashCode() : 0");
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
                builder.addStatement("hash = 31 * hash + $T.hashCode($L)", boxType, prop.getValueName());
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
                    builder.beginControlFlow("if ($L != null)", prop.getValueName());
                }
                builder.addStatement("hash = 31 * hash + $T.identityHashCode($L)", System.class, prop.getValueName());
                builder.endControlFlow();
            } else {
                if (prop.isLoadedStateRequired()) {
                    builder.beginControlFlow(
                            "if ($L && $L != null)",
                            prop.getLoadedStateName(),
                            prop.getValueName()
                    );
                } else {
                    builder.beginControlFlow(
                            "if ($L != null)",
                            prop.getValueName()
                    );
                }
                builder.addStatement("hash = 31 * hash + $L.hashCode()", prop.getValueName());
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
                            "if (__isVisible($T.byIndex($L)) != __other.__isVisible($T.byIndex($L)))",
                            Constants.PROP_ID_CLASS_NAME,
                            prop.getSlotName(),
                            Constants.PROP_ID_CLASS_NAME,
                            prop.getSlotName()
                    )
                    .addStatement("return false")
                    .endControlFlow();
            if (!prop.isValueRequired()) {
                continue;
            }
            if (prop.isLoadedStateRequired()) {
                builder.addStatement("boolean $L = this.$L", prop.getLoadedStateName(), prop.getLoadedStateName());
            } else {
                builder.addStatement("boolean $L = $L != null", prop.getLoadedStateName(true), prop.getValueName());
            }
            builder
                    .beginControlFlow(
                            "if ($L != __other.__isLoaded($T.byIndex($L)))",
                            prop.getLoadedStateName(true),
                            Constants.PROP_ID_CLASS_NAME,
                            prop.getSlotName()
                    )
                    .addStatement("return false")
                    .endControlFlow();
            if (shallow || prop.getReturnType().getKind().isPrimitive()) {
                if (!shallow && prop.getAnnotation(Id.class) != null) {
                    builder
                            .beginControlFlow("if ($L)", prop.getLoadedStateName(true))
                            .addComment("If entity-id is loaded, return directly")
                            .addStatement("return $L == __other.$L()", prop.getValueName(), prop.getGetterName())
                            .endControlFlow();
                } else {
                    builder
                            .beginControlFlow(
                                    "if ($L && $L != __other.$L())",
                                    prop.getLoadedStateName(true),
                                    prop.getValueName(),
                                    prop.getGetterName()
                            )
                            .addStatement("return false")
                            .endControlFlow();
                }
            } else if (prop.getAnnotation(Id.class) != null) {
                builder
                        .beginControlFlow(
                                "if ($L)",
                                prop.getLoadedStateName(true)
                        )
                        .addComment("If entity-id is loaded, return directly")
                        .addStatement(
                                "return $T.equals($L, __other.$L())",
                                Objects.class,
                                prop.getValueName(),
                                prop.getGetterName()
                        )
                        .endControlFlow();
            } else {
                builder
                        .beginControlFlow(
                                "if ($L && !$T.equals($L, __other.$L()))",
                                prop.getLoadedStateName(true),
                                Objects.class,
                                prop.getValueName(),
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
