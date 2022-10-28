package org.babyfish.jimmer.apt.generator;

import com.squareup.javapoet.*;
import org.babyfish.jimmer.ImmutableObjects;
import org.babyfish.jimmer.apt.TypeUtils;
import org.babyfish.jimmer.apt.meta.ImmutableProp;
import org.babyfish.jimmer.apt.meta.ImmutableType;
import org.babyfish.jimmer.jackson.ImmutableModule;
import org.babyfish.jimmer.jackson.ImmutableModuleRequiredException;
import org.babyfish.jimmer.runtime.ImmutableSpi;

import javax.lang.model.element.Modifier;

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
        typeBuilder.superinterfaces.add(type.getClassName());
        typeBuilder.superinterfaces.add(spiClassName);
        addGet(int.class);
        addGet(String.class);
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
        for (ImmutableProp prop : type.getPropsOrderById()) {
            if (prop.getBoxType() != null) {
                builder.addStatement("case $L: return ($T)$L()",
                        argType == int.class ? prop.getId() : '"' + prop.getName() + '"',
                        prop.getBoxType(),
                        prop.getGetterName()
                );
            } else {
                builder.addStatement("case $L: return $L()",
                        argType == int.class ? prop.getId() : '"' + prop.getName() + '"',
                        prop.getGetterName()
                );
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

    private void addType() {
        MethodSpec.Builder builder = MethodSpec
                .methodBuilder("__type")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .returns(Constants.RUNTIME_TYPE_CLASS_NAME)
                .addStatement("return TYPE");
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

    private void addDummyProp() {
        MethodSpec.Builder builder = MethodSpec
                .methodBuilder("getDummyPropForNoImmutableModuleError")
                .returns(TypeName.INT)
                .addModifiers(Modifier.PUBLIC)
                .addStatement("throw new $T()", ImmutableModuleRequiredException.class);
        typeBuilder.addMethod(builder.build());
    }
}
