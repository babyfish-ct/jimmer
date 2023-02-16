package org.babyfish.jimmer.apt.generator;

import com.squareup.javapoet.*;
import org.babyfish.jimmer.apt.meta.ImmutableProp;
import org.babyfish.jimmer.apt.meta.ImmutableType;

import javax.lang.model.element.Modifier;

public class MapStructGenerator {

    private final ImmutableType type;

    private TypeSpec.Builder typeBuilder;

    public MapStructGenerator(ImmutableType type) {
        this.type = type;
    }

    public void generate(TypeSpec.Builder parentBuilder) {
        typeBuilder = TypeSpec
                .classBuilder("MapStruct")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC);
        addMembers();
        parentBuilder.addType(typeBuilder.build());
    }

    private void addMembers() {
        for (ImmutableProp prop : type.getProps().values()) {
            addFields(prop);
        }
        for (ImmutableProp prop : type.getProps().values()) {
            addSetter(prop);
        }
        addBuild();
    }

    private void addFields(ImmutableProp prop) {
        if (prop.isJavaFormula()) {
            return;
        }
        if (prop.isLoadedStateRequired()) {
            typeBuilder.addField(
                    FieldSpec.builder(
                            TypeName.BOOLEAN,
                            prop.getLoadedStateName()
                    ).addModifiers(Modifier.PRIVATE).build()
            );
        }
        typeBuilder.addField(
                FieldSpec.builder(
                        prop.getTypeName(),
                        prop.getName()
                ).addModifiers(Modifier.PRIVATE).build()
        );
    }

    private void addSetter(ImmutableProp prop) {
        if (prop.isJavaFormula()) {
            return;
        }
        MethodSpec.Builder builder = MethodSpec
                .methodBuilder(prop.getName())
                .addModifiers(Modifier.PUBLIC)
                .addParameter(
                        prop.getTypeName(),
                        prop.getName()
                )
                .returns(type.getMapStructClassName());
        if (prop.isList()) {
            builder.addStatement(
                    "this.$L = $L != null ? $L : $T.emptyList()",
                    prop.getName(),
                    prop.getName(),
                    prop.getName(),
                    Constants.COLLECTIONS_CLASS_NAME
            );
        } else if (prop.isNullable() || prop.getTypeName().isPrimitive()) {
            if (prop.isLoadedStateRequired()) {
                builder.addStatement("this.$L = true", prop.getLoadedStateName());
            }
            builder.addStatement("this.$L = $L", prop.getName(), prop.getName());
        } else {
            builder.beginControlFlow("if ($L != null)", prop.getName());
            if (prop.isLoadedStateRequired()) {
                builder.addStatement("this.$L = true", prop.getLoadedStateName());
            }
            builder.addStatement("this.$L = $L", prop.getName(), prop.getName());
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
        builder.addCode("return $T.$L.produce(draft -> {$>\n", type.getDraftClassName(), "$");
        for (ImmutableProp prop : type.getProps().values()) {
            if (!prop.isJavaFormula()) {
                if (prop.isLoadedStateRequired()) {
                    builder.beginControlFlow("if ($L)", prop.getLoadedStateName());
                } else {
                    builder.beginControlFlow("if ($L != null)", prop.getName());
                }
                builder.addStatement("draft.$L($L)", prop.getSetterName(), prop.getName());
                builder.endControlFlow();
            }
        }
        builder.addCode("$<});\n");
        typeBuilder.addMethod(builder.build());
    }
}
