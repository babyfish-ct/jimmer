package org.babyfish.jimmer.apt.generator;

import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeSpec;
import org.babyfish.jimmer.apt.meta.ImmutableProp;
import org.babyfish.jimmer.apt.meta.ImmutableType;
import org.babyfish.jimmer.dto.compiler.DtoProp;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.lang.model.element.Modifier;
import java.util.List;

public class DtoBuilderGenerator {

    private final DtoGenerator parent;

    private final List<DtoProp<ImmutableType, ImmutableProp>> props;

    private TypeSpec.Builder typeBuilder;

    public DtoBuilderGenerator(DtoGenerator parent) {
        this.parent = parent;
        this.props = parent.getDtoType().getProps();
    }

    public void generate() {
        typeBuilder = TypeSpec
                .classBuilder("Builder")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC);
        addMembers();
        parent.getTypeBuilder().addType(typeBuilder.build());
    }

    private void addMembers() {
        for (DtoProp<ImmutableType, ImmutableProp> prop : props) {
            addField(prop);
        }
        addConstructor();
        for (DtoProp<ImmutableType, ImmutableProp> prop : props) {
            addSetter(prop);
        }
        addBuild();
    }

    private void addField(DtoProp<ImmutableType, ImmutableProp> prop) {
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
        for (DtoProp<ImmutableType, ImmutableProp> prop : props) {
            builder.addStatement("this.$L = base.$L()", prop.getName(), DtoGenerator.dtoGetterName(prop));
        }
        builder.endControlFlow();
        typeBuilder.addMethod(builder.build());
    }

    private void addSetter(DtoProp<ImmutableType, ImmutableProp> prop) {

        MethodSpec.Builder builder = MethodSpec
                .methodBuilder(prop.getBaseProp().getSetterName())
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
                .addModifiers(Modifier.PUBLIC)
                .returns(parent.getClassName())
                .addAnnotation(NotNull.class);
        for (DtoProp<ImmutableType, ImmutableProp> prop : props) {
            if (!prop.isNullable() && !prop.isIdOnly() && !prop.getBaseProp().getTypeName().isPrimitive()) {
                builder
                        .beginControlFlow("if ($L == null)", prop.getName())
                        .addStatement(
                                "throw new IllegalArgumentException($S)",
                                "Property \"" + prop.getName() + "\" has not been set"
                        )
                        .endControlFlow();
            } else if (!prop.isNullable() && prop.isIdOnly() && !prop.getBaseProp().getTargetType().getIdProp().getTypeName().isPrimitive()) {
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
        for (DtoProp<ImmutableType, ImmutableProp> prop : props) {
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
