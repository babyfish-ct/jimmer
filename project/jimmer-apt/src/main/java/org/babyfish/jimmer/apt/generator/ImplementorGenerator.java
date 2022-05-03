package org.babyfish.jimmer.apt.generator;

import com.squareup.javapoet.*;
import org.babyfish.jimmer.apt.TypeUtils;
import org.babyfish.jimmer.apt.meta.ImmutableProp;
import org.babyfish.jimmer.apt.meta.ImmutableType;
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
        addGet();
        addType();
        parentBuilder.addType(typeBuilder.build());
    }

    private void addGet() {
        TypeVariableName typeVariableName = TypeVariableName.get("T");
        MethodSpec.Builder builder = MethodSpec
                .methodBuilder("__get")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .addTypeVariable(typeVariableName)
                .addAnnotation(
                        AnnotationSpec
                                .builder(SuppressWarnings.class)
                                .addMember("value", "$S", "unchecked")
                                .build()
                )
                .addParameter(String.class, "prop")
                .returns(typeVariableName);
        builder.beginControlFlow("switch (prop)");
        for (ImmutableProp prop : type.getProps().values()) {
            if (prop.getBoxType() != null) {
                builder.addStatement("case $S: return (T)($T)$L()",
                        prop.getName(),
                        prop.getBoxType(),
                        prop.getGetterName()
                );
            } else {
                builder.addStatement("case $S: return (T)$L()",
                        prop.getName(),
                        prop.getGetterName()
                );
            }
        }
        builder.addStatement(
                "default: throw new IllegalArgumentException($S)",
                "Illegal property name: \" + prop + \""
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
}
