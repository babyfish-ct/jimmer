package org.babyfish.jimmer.apt.immutable.generator;

import com.squareup.javapoet.*;
import org.babyfish.jimmer.Draft;
import org.babyfish.jimmer.apt.GeneratorException;
import org.babyfish.jimmer.apt.immutable.meta.ImmutableProp;
import org.babyfish.jimmer.apt.immutable.meta.ImmutableType;
import org.babyfish.jimmer.lang.OldChain;
import org.jetbrains.annotations.Nullable;

import javax.annotation.processing.Filer;
import javax.lang.model.element.Modifier;
import java.io.IOException;

public class DraftGenerator {

    private final ImmutableType type;

    private final Filer filer;

    private TypeSpec.Builder typeBuilder;

    public DraftGenerator(
            ImmutableType type,
            Filer filer
    ) {
        this.type = type;
        this.filer = filer;
    }

    public void generate() {
        typeBuilder = TypeSpec
                .interfaceBuilder(type.getName() + "Draft")
                .addSuperinterface(type.getClassName())
                .addAnnotation(
                        AnnotationSpec
                                .builder(Constants.GENERATED_BY_CLASS_NAME)
                                .addMember("type", "$T.class", type.getClassName())
                                .build()
                );
        if (type.getSuperTypes().isEmpty()) {
            typeBuilder.addSuperinterface(Draft.class);
        } else {
            for (ImmutableType superType : type.getSuperTypes()) {
                typeBuilder.addSuperinterface(superType.getDraftClassName());
            }
        }
        addMembers(type);
        try {
            JavaFile
                    .builder(
                            type.getPackageName(),
                            typeBuilder.build()
                    )
                    .indent("    ")
                    .build()
                    .writeTo(filer);
        } catch (IOException ex) {
            throw new GeneratorException(
                    String.format(
                            "Cannot generate draft interface for '%s'",
                            type.getName()
                    ),
                    ex
            );
        }
    }

    private void addMembers(
            ImmutableType type
    ) {
        if (type.getModifiers().contains(Modifier.PUBLIC)) {
            typeBuilder.modifiers.add(Modifier.PUBLIC);
        }
        add$();
        for (ImmutableProp prop : type.getProps().values()) {
            addGetter(prop, false);
            addGetter(prop, true);
            addSetter(prop);
            addAssociatedIdGetter(prop);
            addAssociatedIdSetter(prop);
            addUtilMethod(prop, false);
            addUtilMethod(prop, true);
        }
        new ProducerGenerator(type).generate(typeBuilder);
        if (!type.isMappedSuperClass()) {
            new BuilderGenerator(type).generate(typeBuilder);
        }
    }

    private void add$() {
        FieldSpec.Builder builder = FieldSpec.builder(
                ClassName.get(
                        type.getPackageName(),
                        type.getName() + "Draft.Producer"
                ),
                "$"
        );
        builder.modifiers.add(Modifier.PUBLIC);
        builder.modifiers.add(Modifier.STATIC);
        builder.modifiers.add(Modifier.FINAL);
        builder.initializer("Producer.INSTANCE");
        typeBuilder.addField(builder.build());
    }

    private void addGetter(
            ImmutableProp prop,
            boolean autoCreate
    ) {
        if (prop.getManyToManyViewBaseProp() != null || prop.isFormula()) {
            return;
        }
        if (!autoCreate && (!prop.isAssociation(false) || prop.isList())) {
            return;
        }
        if (autoCreate && !prop.isAssociation(false) && !prop.isList()) {
            return;
        }
        MethodSpec.Builder builder = MethodSpec.methodBuilder(prop.getGetterName());
        builder.modifiers.add(Modifier.PUBLIC);
        builder.modifiers.add(Modifier.ABSTRACT);
        if (autoCreate) {
            builder.addParameter(boolean.class, "autoCreate");
        } else if (prop.isNullable()) {
            builder.addAnnotation(Nullable.class);
        }
        builder.returns(prop.getDraftTypeName(autoCreate));
        typeBuilder.addMethod(builder.build());
    }

    private void addAssociatedIdGetter(ImmutableProp prop) {
        new AssociatedIdGenerator(typeBuilder, false).getter(prop);
    }

    private void addAssociatedIdSetter(ImmutableProp prop) {
        new AssociatedIdGenerator(typeBuilder, false).setter(prop);
    }

    private void addSetter(ImmutableProp prop) {
        if (prop.isJavaFormula() || prop.getManyToManyViewBaseProp() != null) {
            return;
        }
        MethodSpec.Builder builder = MethodSpec
                .methodBuilder(prop.getSetterName())
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .addAnnotation(OldChain.class)
                .addParameter(TypeName.get(prop.getReturnType()), prop.getName())
                .returns(type.getDraftClassName());
        typeBuilder.addMethod(builder.build());
    }

    private void addUtilMethod(ImmutableProp prop, boolean withBase) {
        if (prop.getManyToManyViewBaseProp() != null || !prop.isAssociation(false) || prop.isJavaFormula()) {
            return;
        }
        MethodSpec.Builder builder = MethodSpec
                .methodBuilder(
                        prop.isList() ?
                                prop.getAdderByName():
                                prop.getApplierName()
                )
                .addAnnotation(OldChain.class)
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .returns(type.getDraftClassName());
        if (withBase) {
            builder.addParameter(prop.getElementTypeName(), "base");
        }

        ParameterizedTypeName consumerTypeName = ParameterizedTypeName.get(
                Constants.DRAFT_CONSUMER_CLASS_NAME,
                prop.getDraftElementTypeName()
        );
        builder.addParameter(consumerTypeName, "block");
        typeBuilder.addMethod(builder.build());
    }
}
