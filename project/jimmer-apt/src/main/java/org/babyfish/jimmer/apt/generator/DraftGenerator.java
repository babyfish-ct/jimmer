package org.babyfish.jimmer.apt.generator;

import com.squareup.javapoet.*;
import org.babyfish.jimmer.Draft;
import org.babyfish.jimmer.apt.GeneratorException;
import org.babyfish.jimmer.apt.meta.ImmutableProp;
import org.babyfish.jimmer.apt.meta.ImmutableType;
import org.babyfish.jimmer.lang.OldChain;

import javax.annotation.processing.Filer;
import javax.lang.model.element.Modifier;
import java.io.IOException;

import static org.babyfish.jimmer.apt.generator.Constants.DRAFT_CONSUMER_CLASS_NAME;

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
                .addSuperinterface(type.getClassName());
        if (type.getSuperType() != null) {
            typeBuilder.addSuperinterface(type.getSuperType().getDraftClassName());
        } else {
            typeBuilder.addSuperinterface(Draft.class);
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
            addUtilMethod(prop, false);
            addUtilMethod(prop, true);
        }
        new ProducerGenerator(type).generate(typeBuilder);
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
        }
        builder.returns(prop.getDraftTypeName(autoCreate));
        typeBuilder.addMethod(builder.build());
    }

    private void addSetter(
            ImmutableProp prop
    ) {
        MethodSpec.Builder builder = MethodSpec
                .methodBuilder(prop.getSetterName())
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .addAnnotation(OldChain.class)
                .addParameter(TypeName.get(prop.getReturnType()), prop.getName())
                .returns(type.getDraftClassName());
        typeBuilder.addMethod(builder.build());
    }

    private void addUtilMethod(ImmutableProp prop, boolean withBase) {
        if (!prop.isAssociation(false)) {
            return;
        }
        MethodSpec.Builder builder = MethodSpec
                .methodBuilder(
                        prop.isList() ?
                                prop.getAdderByName() :
                                prop.getSetterName()
                )
                .addAnnotation(OldChain.class)
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .returns(type.getDraftClassName());
        if (withBase) {
            builder.addParameter(prop.getElementTypeName(), "base");
        }

        ParameterizedTypeName consumerTypeName = ParameterizedTypeName.get(
                DRAFT_CONSUMER_CLASS_NAME,
                prop.getDraftElementTypeName()
        );
        builder.addParameter(consumerTypeName, "block");
        typeBuilder.addMethod(builder.build());
    }
}
