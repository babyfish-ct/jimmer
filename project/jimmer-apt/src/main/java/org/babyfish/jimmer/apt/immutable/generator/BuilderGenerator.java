package org.babyfish.jimmer.apt.immutable.generator;

import com.squareup.javapoet.*;
import org.babyfish.jimmer.apt.immutable.meta.ImmutableProp;
import org.babyfish.jimmer.apt.immutable.meta.ImmutableType;
import org.babyfish.jimmer.runtime.Internal;

import javax.lang.model.element.Modifier;

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

    private void addConstructor() {
        MethodSpec.Builder builder = MethodSpec
                .constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addStatement("__draft = new $T(null, null)", type.getDraftImplClassName());
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
        MethodSpec.Builder builder = MethodSpec
                .methodBuilder(prop.getName())
                .addModifiers(Modifier.PUBLIC)
                .addParameter(
                        prop.getTypeName().box(),
                        prop.getName()
                )
                .returns(type.getBuilderClassName());
        Annotations.copyNonJimmerAnnotations(builder, prop.getAnnotations());
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
