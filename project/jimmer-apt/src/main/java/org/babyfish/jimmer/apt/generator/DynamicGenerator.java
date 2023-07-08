package org.babyfish.jimmer.apt.generator;

import com.squareup.javapoet.*;
import org.babyfish.jimmer.apt.GeneratorException;
import org.babyfish.jimmer.apt.meta.ImmutableProp;
import org.babyfish.jimmer.apt.meta.ImmutableType;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.processing.Filer;
import javax.lang.model.element.Modifier;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Objects;

public class DynamicGenerator {

    private final ImmutableType type;

    private final Filer filer;

    private TypeSpec.Builder typeBuilder;

    public DynamicGenerator(
            ImmutableType type,
            Filer filer
    ) {
        this.type = type;
        this.filer = filer;
    }

    public void generate() {
        try {
            JavaFile
                    .builder(
                            type.getPackageName(),
                            generateDynamic()
                    )
                    .indent("    ")
                    .build()
                    .writeTo(filer);
        } catch (IOException ex) {
            throw new GeneratorException(
                    String.format(
                            "Cannot generate dynamic class for '%s'",
                            type.getName()
                    ),
                    ex
            );
        }
    }

    private TypeSpec generateDynamic() {
        TypeSpec.Builder oldTypeBuilder = typeBuilder;
        try {
            typeBuilder = TypeSpec
                    .classBuilder(type.getDynamicClassName())
                    .addModifiers(Modifier.PUBLIC)
                    .addSuperinterface(
                            ParameterizedTypeName.get(
                                    Constants.DYNAMIC_CLASS_NAME,
                                    type.getClassName()
                            )
                    );
            addField();
            addConstruct();
            for (ImmutableProp prop : type.getProps().values()) {
                addProp(prop);
            }
            addUnwrap();
            addHashCode();
            addEquals();
            addToString();
            return typeBuilder.build();
        } finally {
            typeBuilder = oldTypeBuilder;
        }
    }

    private void addField() {
        typeBuilder.addField(
                FieldSpec
                        .builder(
                                type.getClassName(),
                                "raw"
                        )
                        .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
                        .build()
        );
    }

    private void addConstruct() {
        MethodSpec.Builder builder = MethodSpec
                .constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addParameter(
                        type.getClassName(),
                        "raw"
                )
                .addStatement(
                        "this.raw = $T.requireNonNull(raw, $S)",
                        Objects.class,
                        "The argument `raw` cannot be null"
                );
        typeBuilder.addMethod(builder.build());
    }

    private void addUnwrap() {
        MethodSpec.Builder builder = MethodSpec
                .methodBuilder("__unwrap")
                .addAnnotation(NotNull.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(type.getClassName())
                .addStatement("return raw");
        typeBuilder.addMethod(builder.build());
    }

    private void addHashCode() {
        MethodSpec.Builder builder = MethodSpec
                .methodBuilder("hashCode")
                .addModifiers(Modifier.PUBLIC)
                .returns(TypeName.INT)
                .addStatement("return raw.hashCode()");
        typeBuilder.addMethod(builder.build());
    }

    private void addEquals() {
        MethodSpec.Builder builder = MethodSpec
                .methodBuilder("equals")
                .addModifiers(Modifier.PUBLIC)
                .addParameter(TypeName.OBJECT, "o")
                .returns(TypeName.BOOLEAN)
                .beginControlFlow("if (this == o)")
                .addStatement("return true")
                .endControlFlow()
                .beginControlFlow("if (this.getClass() != o.getClass())")
                .addStatement("return false")
                .endControlFlow()
                .addStatement("return raw.equals((($T<?>)o).__unwrap())", Constants.DYNAMIC_CLASS_NAME);
        typeBuilder.addMethod(builder.build());
    }

    public void addToString(){
        MethodSpec.Builder builder = MethodSpec
                .methodBuilder("toString")
                .addModifiers(Modifier.PUBLIC)
                .returns(Constants.STRING_CLASS_NAME)
                .addStatement("return raw.toString()");
        typeBuilder.addMethod(builder.build());
    }

    private void addProp(ImmutableProp prop) {
        MethodSpec.Builder builder = MethodSpec
                .methodBuilder(prop.getBeanGetterName())
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Nullable.class)
                .returns(prop.getDynamicClassName());
        if (type.isMappedSuperClass()) {
            builder.beginControlFlow(
                    "if (!(($T)raw).__isLoaded($S))",
                    ImmutableSpi.class,
                    prop.getName()
            );
        } else {
            builder.beginControlFlow(
                    "if (!(($T)raw).__isLoaded($T.byIndex($T.$L)))",
                    ImmutableSpi.class,
                    Constants.PROP_ID_CLASS_NAME,
                    type.getProducerClassName(),
                    prop.getSlotName()
            );
        }
        builder
                .addStatement("return null")
                .endControlFlow();
        if (!prop.isAssociation(false)) {
            builder.addStatement("return raw.$L()", prop.getGetterName());
        } else {
            builder.addStatement(
                    "$T value = raw.$L()",
                    prop.getTypeName(),
                    prop.getGetterName()
            );
            if (prop.isList()) {
                builder.addStatement(
                        "$T newValue = new $T<>(value.size())",
                        prop.getDynamicClassName(),
                        ArrayList.class
                );
                builder.beginControlFlow("for ($T e : value)", prop.getElementTypeName())
                        .addStatement("newValue.add(new $T(e))", prop.getDynamicElementClassName())
                        .endControlFlow()
                        .addStatement("return newValue");
            } else {
                if (prop.isNullable()) {
                    builder.addStatement("return value != null ? new $T(value) : null", prop.getDynamicElementClassName());
                } else {
                    builder.addStatement("return new $T(value)", prop.getDynamicElementClassName());
                }
            }
        }
        typeBuilder.addMethod(builder.build());
    }
}
