package org.babyfish.jimmer.apt.generator;

import com.squareup.javapoet.*;
import org.babyfish.jimmer.apt.meta.ImmutableProp;
import org.babyfish.jimmer.apt.meta.ImmutableType;
import org.babyfish.jimmer.meta.ImmutablePropCategory;
import org.babyfish.jimmer.runtime.Internal;
import org.babyfish.jimmer.sql.Key;
import org.babyfish.jimmer.sql.ManyToOne;
import org.babyfish.jimmer.sql.OneToOne;

import javax.lang.model.element.Modifier;

import java.util.Arrays;

import static org.babyfish.jimmer.apt.generator.Constants.*;

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
        if (!type.isMappedSuperClass()) {
            addSlots();
        }
        addType();
        addConstructor();
        if (!type.isMappedSuperClass()) {
            addProduce(false);
            addProduce(true);
            new ImplementorGenerator(type).generate(typeBuilder);
            new ImplGenerator(type).generate(typeBuilder);
            new DraftImplGenerator(type).generate(typeBuilder);
        }
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

    private void addSlots() {
        for (ImmutableProp prop : type.getProps().values()) {
            FieldSpec.Builder builder = FieldSpec.builder(
                    TypeName.INT,
                    prop.getSlotName(),
                    Modifier.PUBLIC,
                    Modifier.STATIC,
                    Modifier.FINAL
            );
            if (prop.getDeclaringType() == type || prop.getDeclaringType().isMappedSuperClass()) {
                builder.initializer(Integer.toString(prop.getId()));
            } else {
                builder.initializer("$T.$L", prop.getDeclaringType().getProducerClassName(), prop.getSlotName());
            }
            typeBuilder.addField(builder.build());
        }
    }

    private void addType() {
        CodeBlock.Builder builder = CodeBlock
                .builder()
                .add("$T\n", RUNTIME_TYPE_CLASS_NAME)
                .indent()
                .add(".newBuilder(\n")
                .indent()
                .add("$T.class,\n", type.getClassName());
        switch (type.getSuperTypes().size()) {
            case 0:
                builder.add("$T.emptyList(),\n", COLLECTIONS_CLASS_NAME);
                break;
            case 1:
                builder.add(
                        "$T.singleton($T.Producer.TYPE),\n",
                        COLLECTIONS_CLASS_NAME,
                        type.getSuperTypes().iterator().next().getDraftClassName()
                );
                break;
            default:
                builder.add("$T.asList(\n$>", Arrays.class);
                boolean addComma = false;
                for (ImmutableType superType : type.getSuperTypes()) {
                    if (addComma) {
                        builder.add(",\n");
                    } else {
                        addComma = true;
                    }
                    builder.add("$T.Producer.TYPE", superType.getDraftClassName());
                }
                builder.add("\n$<)\n,");
                break;
        }

        if (type.isMappedSuperClass()) {
            builder.add("null\n");
        } else {
            builder.add(
                    "(ctx, base) -> new $T(ctx, ($T)base)\n",
                    type.getDraftImplClassName(),
                    type.getClassName()
            );
        }
        builder.unindent().add(")\n");
        if (!type.isMappedSuperClass()) {
            for (ImmutableProp prop : type.getRedefinedProps().values()) {
                builder.add(".redefine($S, $L)\n", prop.getName(), prop.getSlotName());
            }
        }
        for (ImmutableProp prop : type.getDeclaredProps().values()) {
            if (type.getPrimarySuperType() != null && type.getPrimarySuperType().getProps().containsKey(prop.getName())) {
                continue;
            }
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
            String slotName = type.isMappedSuperClass() ? "-1" : prop.getSlotName();
            if (prop == type.getIdProp()) {
                builder.add(
                        ".id($L, $S, $T.class)\n",
                        slotName,
                        prop.getName(),
                        prop.getRawElementTypeName()
                );
            } else if (prop == type.getVersionProp()) {
                builder.add(
                        ".version($L, $S)\n",
                        slotName,
                        prop.getName()
                );
            } else if (prop == type.getLogicalDeletedProp()) {
                builder.add(
                        ".logicalDeleted($L, $S, $T.class, $L)\n",
                        slotName,
                        prop.getName(),
                        prop.getRawElementTypeName(),
                        prop.isNullable()
                );
            } else if (prop.getAnnotation(Key.class) != null && !prop.isAssociation(false)) {
                builder.add(
                        ".key($L, $S, $T.class, $L)\n",
                        slotName,
                        prop.getName(),
                        prop.getRawElementTypeName(),
                        prop.isNullable()
                );
            } else if (prop.getAnnotation(Key.class) != null && prop.isAssociation(false)) {
                builder.add(
                        ".keyReference($L, $S, $T.class, $T.class, $L)\n",
                        slotName,
                        prop.getName(),
                        prop.getAnnotation(OneToOne.class) != null ? OneToOne.class : ManyToOne.class,
                        prop.getRawElementTypeName(),
                        prop.isNullable() ? "true" : "false"
                );
            } else if (prop.getAssociationAnnotation() != null) {
                builder.add(
                        ".add($L, $S, $T.class, $T.class, $L)\n",
                        slotName,
                        prop.getName(),
                        prop.getAssociationAnnotation().annotationType(),
                        prop.getRawElementTypeName(),
                        prop.isNullable()
                );
            } else {
                builder.add(
                        ".add($L, $S, $T.$L, $T.class, $L)\n",
                        slotName,
                        prop.getName(),
                        ImmutablePropCategory.class,
                        category.name(),
                        prop.getRawElementTypeName(),
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
