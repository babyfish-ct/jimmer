package org.babyfish.jimmer.apt.generator;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.squareup.javapoet.*;
import org.babyfish.jimmer.apt.meta.ImmutableProp;
import org.babyfish.jimmer.apt.meta.ImmutableType;
import org.babyfish.jimmer.jackson.ImmutableModuleRequiredException;
import org.babyfish.jimmer.meta.PropId;
import org.babyfish.jimmer.runtime.ImmutableSpi;

import javax.lang.model.element.Modifier;

import static org.babyfish.jimmer.apt.generator.Constants.*;

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
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .addSuperinterface(type.getClassName())
                .addSuperinterface(spiClassName);
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

    private void addStaticFields() {
        for (ImmutableProp prop : type.getProps().values()) {
            if (prop.getDeeperPropIdName() != null) {
                FieldSpec.Builder builder = FieldSpec
                        .builder(
                                PROP_ID_CLASS_NAME,
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
                                    MANY_TO_MANY_VIEW_LIST_CLASS_NAME,
                                    prop.getDeeperPropIdName(),
                                    manyToManyViewBaseProp.getGetterName()
                            )
                            .build()
            );
        }
        if (!prop.isBeanStyle()) {
            String name = prop.getGetterName();
            boolean isBoolean = prop.getTypeName().equals(TypeName.BOOLEAN);
            typeBuilder.addMethod(
                    MethodSpec
                            .methodBuilder(
                                    (isBoolean ? "is" : "get") +
                                            Character.toUpperCase(name.charAt(0)) +
                                            name.substring(1)
                            )
                            .addAnnotation(JsonIgnore.class)
                            .addModifiers(Modifier.PUBLIC, Modifier.DEFAULT)
                            .returns(prop.getTypeName())
                            .addStatement("return $L()", name)
                            .build()
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
