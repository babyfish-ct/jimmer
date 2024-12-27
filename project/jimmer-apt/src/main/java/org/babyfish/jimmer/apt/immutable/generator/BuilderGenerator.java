package org.babyfish.jimmer.apt.immutable.generator;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.squareup.javapoet.*;
import org.babyfish.jimmer.apt.immutable.meta.ImmutableProp;
import org.babyfish.jimmer.apt.immutable.meta.ImmutableType;
import org.babyfish.jimmer.impl.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;

import java.util.ArrayList;
import java.util.Collection;

import static org.babyfish.jimmer.apt.util.GeneratedAnnotation.generatedAnnotation;

public class BuilderGenerator {

    private final ImmutableType type;

    private TypeSpec.Builder typeBuilder;

    public BuilderGenerator(ImmutableType type) {
        this.type = type;
    }

    public void generate(TypeSpec.Builder parentBuilder) {
        typeBuilder = TypeSpec
                .classBuilder("Builder")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addAnnotation(generatedAnnotation(type));
        addMembers();
        parentBuilder.addType(typeBuilder.build());
    }

    private void addMembers() {
        addField();
        addDefaultConstructor();
        addConstructor();
        for (ImmutableProp prop : type.getProps().values()) {
            addSetter(prop);
        }
        addBuild();
    }

    private void addField() {
        typeBuilder.addField(
                FieldSpec
                        .builder(
                                type.getDraftImplClassName(),
                                "__draft"
                        )
                        .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
                        .build()
        );
    }

    private void addDefaultConstructor() {
        MethodSpec.Builder builder = MethodSpec
                .constructorBuilder()
                .addModifiers(Modifier.PUBLIC);
        builder.addStatement("this(null)", type.getDraftImplClassName());
        typeBuilder.addMethod(builder.build());
    }

    private void addConstructor() {
        MethodSpec.Builder builder = MethodSpec
                .constructorBuilder()
                .addModifiers(Modifier.PUBLIC);
        builder.addParameter(
                ParameterSpec
                        .builder(type.getClassName(), "base")
                        .addAnnotation(Nullable.class)
                        .build()
        );
        builder.addStatement("__draft = new $T(null, base)", type.getDraftImplClassName());
        for (ImmutableProp prop : type.getProps().values()) {
            if (isVisibilityControllable(prop)) {
                builder.addStatement(
                        "__draft.__show($T.byIndex($T.$L), false)",
                        Constants.PROP_ID_CLASS_NAME,
                        type.getProducerClassName(),
                        prop.getSlotName()
                );
            }
        }
        typeBuilder.addMethod(builder.build());
    }

    private void addSetter(ImmutableProp prop) {
        if (prop.isJavaFormula() || prop.getManyToManyViewBaseProp() != null) {
            return;
        }
        ParameterSpec.Builder parameterBuilder = ParameterSpec.builder(
                prop.getTypeName().box(),
                prop.getName()
        );
        if (prop.isNullable()) {
            parameterBuilder.addAnnotation(Nullable.class);
        } else {
            parameterBuilder.addAnnotation(NotNull.class);
        }
        MethodSpec.Builder builder = MethodSpec
                .methodBuilder(prop.getName())
                .addModifiers(Modifier.PUBLIC)
                .addParameter(parameterBuilder.build())
                .returns(type.getBuilderClassName());
        Collection<AnnotationMirror> annotations = new ArrayList<>(prop.getAnnotations().size());
        for (AnnotationMirror annotation : prop.getAnnotations()) {
            TypeElement typeElement = (TypeElement) annotation.getAnnotationType().asElement();
            if (!typeElement.getSimpleName().toString().equals("Nullable")) {
                annotations.add(annotation);
            }
        }
        Annotations.copyNonJimmerAnnotations(builder, annotations);
        if (prop.isNullable()) {
            builder.addStatement("__draft.$L($L)", prop.getSetterName(), prop.getName());
            if (isVisibilityControllable(prop)) {
                builder.addStatement(
                        "__draft.__show($T.byIndex($T.$L), true)",
                        Constants.PROP_ID_CLASS_NAME,
                        type.getProducerClassName(),
                        prop.getSlotName()
                );
            }
        } else {
            builder.beginControlFlow("if ($L != null)", prop.getName());
            builder.addStatement("__draft.$L($L)", prop.getSetterName(), prop.getName());
            if (isVisibilityControllable(prop)) {
                builder.addStatement(
                        "__draft.__show($T.byIndex($T.$L), true)",
                        Constants.PROP_ID_CLASS_NAME,
                        type.getProducerClassName(),
                        prop.getSlotName()
                );
            }
            builder.endControlFlow();
        }
        builder.addStatement("return this");
        typeBuilder.addMethod(builder.build());
    }

    private void addBuild() {
        MethodSpec.Builder builder = MethodSpec
                .methodBuilder("build")
                .addModifiers(Modifier.PUBLIC)
                .returns(type.getClassName());
        builder.addStatement(
                "return ($T)__draft.__modified()",
                type.getClassName()
        );
        typeBuilder.addMethod(builder.build());
    }

    private static boolean isVisibilityControllable(ImmutableProp prop) {
        return prop.isBaseProp() ||
                !prop.getDependencies().isEmpty() ||
                prop.getIdViewBaseProp() != null ||
                prop.getManyToManyViewBaseProp() != null;
    }
}
