package org.babyfish.jimmer.apt.generator;

import com.squareup.javapoet.*;
import org.babyfish.jimmer.apt.meta.ImmutableProp;
import org.babyfish.jimmer.apt.meta.ImmutableType;
import org.babyfish.jimmer.meta.ImmutablePropCategory;
import org.babyfish.jimmer.runtime.Internal;
import org.babyfish.jimmer.sql.Key;

import javax.lang.model.element.Modifier;

import static org.babyfish.jimmer.apt.generator.Constants.DRAFT_CONSUMER_CLASS_NAME;
import static org.babyfish.jimmer.apt.generator.Constants.RUNTIME_TYPE_CLASS_NAME;

public class ProducerGenerator {

    private final ImmutableType type;

    private TypeSpec.Builder typeBuilder;

    ProducerGenerator(ImmutableType type) {
        this.type = type;
    }

    public void generate(TypeSpec.Builder parentBuilder) {
        typeBuilder = TypeSpec.classBuilder("Producer");
        typeBuilder.modifiers.add(Modifier.PUBLIC);
        typeBuilder.modifiers.add(Modifier.STATIC);
        addInstance();
        addType();
        addConstructor();
        addProduce(false);
        addProduce(true);
        new ImplementorGenerator(type).generate(typeBuilder);
        new ImplGenerator(type).generate(typeBuilder);
        new DraftImplGenerator(type).generate(typeBuilder);
        parentBuilder.addType(typeBuilder.build());
    }

    private void addProduce(Boolean base) {
        TypeName baseType = type.getClassName();
        TypeName draftType = type.getDraftClassName();
        MethodSpec.Builder builder = MethodSpec.methodBuilder("produce");
        builder.modifiers.add(Modifier.PUBLIC);
        if (base) {
            builder.addParameter(baseType, "base");
        }
        builder.addParameter(
                ParameterizedTypeName.get(
                        DRAFT_CONSUMER_CLASS_NAME,
                        draftType
                ),
                "block"
        );
        builder.returns(baseType);
        if (base) {
            builder.addCode(
                    "return ($T)$T.produce(TYPE, base, block);",
                    baseType,
                    Internal.class
            );
        } else {
            builder.addCode("return produce(null, block);");
        }
        typeBuilder.addMethod(builder.build());
    }

    private void addInstance() {
        FieldSpec.Builder builder = FieldSpec.builder(
                type.getProducerClassName(),
                "INSTANCE",
                Modifier.STATIC,
                Modifier.FINAL
        );
        builder.initializer("new $T()", type.getProducerClassName());
        typeBuilder.addField(builder.build());
    }

    private void addType() {
        CodeBlock.Builder builder = CodeBlock
                .builder()
                .add("$T\n", RUNTIME_TYPE_CLASS_NAME)
                .indent()
                .add(".newBuilder(\n")
                .indent()
                .add("$T.class,\n", type.getClassName());
        if (type.getSuperType() != null) {
            builder.add(
                    "$T.Producer.TYPE,\n",
                    type.getSuperType().getDraftClassName()
            );
        } else {
            builder.add("null,\n");
        }

        builder
                .add(
                        "(ctx, base) -> new $T(ctx, ($T)base)\n",
                        type.getDraftImplClassName(),
                        type.getClassName()
                )
                .unindent()
                .add(")\n");
        for (ImmutableProp prop : type.getDeclaredProps().values()) {
            ImmutablePropCategory category;
            if (prop.isList()) {
                category = prop.isAssociation(false) ?
                        ImmutablePropCategory.REFERENCE_LIST :
                        ImmutablePropCategory.SCALAR_LIST;
            } else if (prop.isAssociation(false)) {
                category = ImmutablePropCategory.REFERENCE;
            } else {
                category = ImmutablePropCategory.SCALAR;
            }
            if (prop == type.getIdProp()) {
                builder.add(
                        ".id($L, $S, $T.class)\n",
                        prop.getId(),
                        prop.getName(),
                        prop.getElementTypeName()
                );
            } else if (prop == type.getVersionProp()) {
                builder.add(
                        ".version($L, $S)\n",
                        prop.getId(),
                        prop.getName()
                );
            } else if (prop.getAnnotation(Key.class) != null && !prop.isAssociation(false)) {
                builder.add(
                        ".key($L, $S, $T.class)\n",
                        prop.getId(),
                        prop.getName(),
                        prop.getElementTypeName()
                );
            } else if (prop.getAnnotation(Key.class) != null && prop.isAssociation(false)) {
                builder.add(
                        ".keyReference($L, $S, $T.class, $L)\n",
                        prop.getId(),
                        prop.getName(),
                        prop.getElementTypeName(),
                        prop.isNullable() ? "true" : "false"
                );
            } else if (prop.getAssociationAnnotation() != null) {
                builder.add(
                        ".add($L, $S, $T.class, $T.class, $L)\n",
                        prop.getId(),
                        prop.getName(),
                        prop.getAssociationAnnotation().annotationType(),
                        prop.getElementTypeName(),
                        prop.isNullable()
                );
            } else {
                builder.add(
                        ".add($L, $S, $T.$L, $T.class, $L)\n",
                        prop.getId(),
                        prop.getName(),
                        ImmutablePropCategory.class,
                        category.name(),
                        prop.getElementTypeName(),
                        prop.isNullable()
                );
            }
        }
        builder.add(".build()")
                .unindent();
        typeBuilder.addField(
                FieldSpec
                        .builder(
                                RUNTIME_TYPE_CLASS_NAME,
                                "TYPE",
                                Modifier.PUBLIC,
                                Modifier.STATIC,
                                Modifier.FINAL
                        )
                        .initializer(builder.build())
                        .build()
        );
    }

    private void addConstructor() {
        MethodSpec.Builder builder = MethodSpec.constructorBuilder();
        builder.modifiers.add(Modifier.PRIVATE);
        typeBuilder.addMethod(builder.build());
    }
}
