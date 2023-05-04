package org.babyfish.jimmer.apt.generator;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.squareup.javapoet.*;
import org.babyfish.jimmer.ImmutableObjects;
import org.babyfish.jimmer.apt.meta.ImmutableProp;
import org.babyfish.jimmer.apt.meta.ImmutableType;
import org.babyfish.jimmer.jackson.ImmutableModuleRequiredException;
import org.babyfish.jimmer.runtime.ImmutableSpi;

import javax.lang.model.element.Modifier;

import static org.babyfish.jimmer.apt.generator.Constants.MANY_TO_MANY_VIEW_LIST_CLASS_NAME;

public class ImplementorGenerator {

    private ImmutableType type;

    private ClassName spiClassName;

    private TypeSpec.Builder typeBuilder;

    ImplementorGenerator(ImmutableType type) {
        this.type = type;
        spiClassName = ClassName.get(ImmutableSpi.class);
    }

    public void generate(TypeSpec.Builder parentBuilder) {
        typeBuilder = TypeSpec.classBuilder("Implementor");
        typeBuilder.modifiers.add(Modifier.PUBLIC);
        typeBuilder.modifiers.add(Modifier.STATIC);
        typeBuilder.modifiers.add(Modifier.ABSTRACT);
        typeBuilder.addSuperinterface(type.getClassName());
        typeBuilder.addSuperinterface(spiClassName);
        addGet(int.class);
        addGet(String.class);
        for (ImmutableProp prop : type.getProps().values()) {
            addGetterIfNecessary(prop);
        }
        addType();
        addToString();
        addDummyProp();
        parentBuilder.addType(typeBuilder.build());
    }

    private void addGet(Class<?> argType) {
        MethodSpec.Builder builder = MethodSpec
                .methodBuilder("__get")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .addParameter(argType, "prop")
                .returns(Object.class);
        builder.beginControlFlow("switch (prop)");
        CaseAppender appender = new CaseAppender(builder, type, argType);
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
                .addModifiers(Modifier.PUBLIC)
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
                            .addModifiers(Modifier.PUBLIC)
                            .returns(prop.getTypeName())
                            .addStatement(
                                    "return new $T<>($L, $L())",
                                    MANY_TO_MANY_VIEW_LIST_CLASS_NAME,
                                    prop.getManyToManyViewBaseDeeperProp().getId(),
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
                            .addModifiers(Modifier.PUBLIC)
                            .returns(prop.getTypeName())
                            .addStatement("return $L()", name)
                            .build()
            );
        }
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

    private void addDummyProp() {
        MethodSpec.Builder builder = MethodSpec
                .methodBuilder("getDummyPropForJacksonError__")
                .returns(TypeName.INT)
                .addModifiers(Modifier.PUBLIC)
                .addStatement("throw new $T()", ImmutableModuleRequiredException.class);
        typeBuilder.addMethod(builder.build());
    }
}
