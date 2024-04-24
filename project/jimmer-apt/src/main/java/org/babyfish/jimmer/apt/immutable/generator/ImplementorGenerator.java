package org.babyfish.jimmer.apt.immutable.generator;

import com.squareup.javapoet.*;
import org.babyfish.jimmer.apt.immutable.meta.ImmutableProp;
import org.babyfish.jimmer.apt.immutable.meta.ImmutableType;
import org.babyfish.jimmer.impl.util.StringUtil;
import org.babyfish.jimmer.jackson.ImmutableModuleRequiredException;
import org.babyfish.jimmer.meta.PropId;
import org.babyfish.jimmer.runtime.ImmutableSpi;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Modifier;

import static org.babyfish.jimmer.apt.util.GeneratedAnnotation.generatedAnnotation;

public class ImplementorGenerator {

    private final ImmutableType type;

    private final ClassName spiClassName;

    private TypeSpec.Builder typeBuilder;

    ImplementorGenerator(ImmutableType type) {
        this.type = type;
        spiClassName = ClassName.get(ImmutableSpi.class);
    }

    public void generate(TypeSpec.Builder parentBuilder) {
        typeBuilder = TypeSpec
                .interfaceBuilder("Implementor")
                .addAnnotation(generatedAnnotation(type))
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .addSuperinterface(type.getClassName())
                .addSuperinterface(spiClassName);

        addPropertyOrderAnnotation();
        addStaticFields();
        addGet(PropId.class);
        addGet(String.class);
        for (ImmutableProp prop : type.getProps().values()) {
            addGetterIfNecessary(prop);
        }
        addType();
        addDummyProp();
        parentBuilder.addType(typeBuilder.build());
    }

    private void addPropertyOrderAnnotation() {
        CodeBlock.Builder builder = CodeBlock.builder();
        builder.add("{$S", "dummyPropForJacksonError__");
        for (ImmutableProp prop : type.getPropsOrderById()) {
            builder.add(", $S", prop.getName());
        }
        builder.add("}");
        typeBuilder.addAnnotation(
                AnnotationSpec
                        .builder(Constants.JSON_PROPERTY_ORDER_CLASS_NAME)
                        .addMember("value", builder.build())
                        .build()
        );
    }

    private void addStaticFields() {
        for (ImmutableProp prop : type.getProps().values()) {
            if (prop.getDeeperPropIdName() != null) {
                FieldSpec.Builder builder = FieldSpec
                        .builder(
                                Constants.PROP_ID_CLASS_NAME,
                                prop.getDeeperPropIdName()
                        )
                        .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                        .initializer(
                                "$T.TYPE.getProp($S).getManyToManyViewBaseDeeperProp().getId()",
                                type.getProducerClassName(),
                                prop.getName()
                        );
                typeBuilder.addField(builder.build());
            }
        }
    }

    private void addGet(Class<?> argType) {
        MethodSpec.Builder builder = MethodSpec
                .methodBuilder("__get")
                .addModifiers(Modifier.PUBLIC, Modifier.DEFAULT)
                .addAnnotation(Override.class)
                .addParameter(argType, "prop")
                .returns(Object.class);
        CaseAppender appender = new CaseAppender(builder, type, argType);
        if (argType == PropId.class) {
            builder.addStatement("int __propIndex = prop.asIndex()");
            builder.beginControlFlow("switch (__propIndex)");
            appender.addIllegalCase();
            builder.addStatement("return __get(prop.asName())");
        } else {
            builder.beginControlFlow("switch (prop)");
        }
        for (ImmutableProp prop : type.getPropsOrderById()) {
            appender.addCase(prop);
            if (prop.getBoxType() != null) {
                builder.addStatement("return ($T)$L()",
                        prop.getBoxType(),
                        prop.getGetterName()
                );
            } else {
                builder.addStatement("return $L()",
                        prop.getGetterName()
                );
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

    private void addType() {
        MethodSpec.Builder builder = MethodSpec
                .methodBuilder("__type")
                .addModifiers(Modifier.PUBLIC, Modifier.DEFAULT)
                .addAnnotation(Override.class)
                .returns(Constants.RUNTIME_TYPE_CLASS_NAME)
                .addStatement("return TYPE");
        typeBuilder.addMethod(builder.build());
    }

    private void addGetterIfNecessary(ImmutableProp prop) {
        ImmutableProp manyToManyViewBaseProp = prop.getManyToManyViewBaseProp();
        if (manyToManyViewBaseProp != null) {
            typeBuilder.addMethod(
                    MethodSpec
                            .methodBuilder(prop.getGetterName())
                            .addAnnotation(Override.class)
                            .addModifiers(Modifier.PUBLIC, Modifier.DEFAULT)
                            .returns(prop.getTypeName())
                            .addStatement(
                                    "return new $T<>(\n$>$L, $L()$<\n)",
                                    Constants.MANY_TO_MANY_VIEW_LIST_CLASS_NAME,
                                    prop.getDeeperPropIdName(),
                                    manyToManyViewBaseProp.getGetterName()
                            )
                            .build()
            );
        }
        if (!prop.isBeanStyle()) {
            String name = prop.getGetterName();
            boolean isBoolean = prop.getTypeName().equals(TypeName.BOOLEAN);
            MethodSpec.Builder builder = MethodSpec
                    .methodBuilder(
                            StringUtil.identifier(isBoolean ? "is" : "get", name)
                    )
                    .addModifiers(Modifier.PUBLIC, Modifier.DEFAULT)
                    .returns(prop.getTypeName())
                    .addStatement("return $L()", name);
            Annotations.copyNonJimmerAnnotations(builder, prop.getAnnotations());
            typeBuilder.addMethod(
                    builder.build()
            );
        }
    }

    private void addDummyProp() {
        MethodSpec.Builder builder = MethodSpec
                .methodBuilder("getDummyPropForJacksonError__")
                .returns(TypeName.INT)
                .addModifiers(Modifier.PUBLIC, Modifier.DEFAULT)
                .addStatement("throw new $T()", ImmutableModuleRequiredException.class);
        typeBuilder.addMethod(builder.build());
    }
}
