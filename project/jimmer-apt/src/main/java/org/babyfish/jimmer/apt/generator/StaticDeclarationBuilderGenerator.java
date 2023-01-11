package org.babyfish.jimmer.apt.generator;

import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeSpec;
import org.babyfish.jimmer.apt.meta.StaticProp;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.lang.model.element.Modifier;
import java.util.List;

public class StaticDeclarationBuilderGenerator {

    private final StaticDeclarationGenerator parent;

    private final List<StaticProp> props;

    private TypeSpec.Builder typeBuilder;

    public StaticDeclarationBuilderGenerator(
            StaticDeclarationGenerator parent
    ) {
        this.parent = parent;
        this.props = parent.getProps();
    }

    public void generate() {
        typeBuilder = TypeSpec
                .classBuilder("Builder")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC);
        addMembers();
        parent.getTypeBuilder().addType(typeBuilder.build());
    }

    private void addMembers() {
        for (StaticProp prop : props) {
            addField(prop);
        }
        addConstructor();
        for (StaticProp prop : props) {
            addSetter(prop);
        }
        addBuild();
    }

    private void addField(StaticProp prop) {
        FieldSpec.Builder builder = FieldSpec
                .builder(
                        parent.getPropTypeName(prop),
                        prop.getName()
                )
                .addModifiers(Modifier.PRIVATE);
        typeBuilder.addField(builder.build());
    }

    private void addConstructor() {
        MethodSpec.Builder builder = MethodSpec
                .constructorBuilder()
                .addParameter(
                        ParameterSpec
                                .builder(
                                        parent.getClassName(),
                                        "base"
                                )
                                .build()
                );
        builder.beginControlFlow("if (base != null)");
        for (StaticProp prop : props) {
            builder.addStatement("this.$L = base.$L()", prop.getName(), prop.getGetterName());
        }
        builder.endControlFlow();
        typeBuilder.addMethod(builder.build());
    }

    private void addSetter(StaticProp prop) {

        MethodSpec.Builder builder = MethodSpec
                .methodBuilder(prop.getSetterName())
                .addModifiers(Modifier.PUBLIC)
                .returns(parent.getClassName("Builder"))
                .addAnnotation(NotNull.class)
                .addParameter(
                        ParameterSpec
                                .builder(parent.getPropTypeName(prop), prop.getName())
                                .addAnnotation(prop.isNullable() ? Nullable.class : NotNull.class)
                                .build()
                )
                .addStatement("this.$L = $L", prop.getName(), prop.getName())
                .addStatement("return this");
        typeBuilder.addMethod(builder.build());
    }

    private void addBuild() {
        MethodSpec.Builder builder = MethodSpec
                .methodBuilder("build")
                .returns(parent.getClassName())
                .addAnnotation(NotNull.class);
        for (StaticProp prop : props) {
            if (!prop.isNullable() && !prop.getImmutableProp().getTypeName().isPrimitive()) {
                builder
                        .beginControlFlow("if ($L == null)", prop.getName())
                        .addStatement(
                                "throw new IllegalArgumentException($S)",
                                "Property \"" + prop.getName() + "\" has not been set"
                        )
                        .endControlFlow();
            }
        }
        builder.addCode("return new $T(\n$>", parent.getClassName());
        boolean addComma = false;
        for (StaticProp prop : props) {
            if (addComma) {
                builder.addCode(",\n");
            } else {
                addComma = true;
            }
            builder.addCode(prop.getName());
        }
        builder.addCode("$<\n);\n");
        typeBuilder.addMethod(builder.build());
    }
}
