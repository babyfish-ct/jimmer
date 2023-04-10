package org.babyfish.jimmer.apt.generator;

import com.squareup.javapoet.*;
import org.babyfish.jimmer.ImmutableObjects;
import org.babyfish.jimmer.apt.meta.ImmutableProp;
import org.babyfish.jimmer.apt.meta.ImmutableType;

import javax.lang.model.element.Modifier;
import java.util.ArrayList;
import java.util.Collections;

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
        if (prop.isJavaFormula() || prop.getIdViewBaseProp() != null) {
            return;
        }
        if (isMapStructLoadedStateRequired(prop)) {
            typeBuilder.addField(
                    FieldSpec.builder(
                            TypeName.BOOLEAN,
                            prop.getLoadedStateName()
                    ).addModifiers(Modifier.PRIVATE).build()
            );
        }
        typeBuilder.addField(
                FieldSpec.builder(
                        prop.getTypeName().box(),
                        prop.getName()
                ).addModifiers(Modifier.PRIVATE).build()
        );
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
                .returns(type.getMapStructClassName());
        if (prop.getIdViewBaseProp() != null) {
            ImmutableProp baseProp = prop.getIdViewBaseProp();
            if (isMapStructLoadedStateRequired(baseProp)) {
                builder.addStatement("this.$L = true", baseProp.getLoadedStateName());
            }
            builder.beginControlFlow("if ($L == null)", prop.getName());
            if (prop.isList()) {
                builder.addStatement(
                        "this.$L = $T.emptyList()",
                        baseProp.getName(),
                        Constants.COLLECTIONS_CLASS_NAME
                );
            } else {
                builder.addStatement("this.$L = null", baseProp.getName());
            }
            builder.nextControlFlow("else");
            if (prop.isList()) {
                builder.addStatement(
                        "$T<$T> __targets = new $T($L.size())",
                        Constants.LIST_CLASS_NAME,
                        baseProp.getElementTypeName(),
                        ArrayList.class,
                        prop.getName()
                );
                builder.beginControlFlow(
                        "for ($T __targetId : $L)",
                        prop.getElementTypeName(),
                        prop.getName()
                );
                builder.addStatement(
                        "__targets.add($T.makeIdOnly($T.class, __targetId))",
                        ImmutableObjects.class,
                        baseProp.getTargetType().getClassName()
                );
                builder.endControlFlow();
                builder.addStatement("this.$L = __targets", baseProp.getName());
            } else {
                builder.addStatement(
                        "this.$L = $T.makeIdOnly($T.class, $L)",
                        baseProp.getName(),
                        ImmutableObjects.class,
                        baseProp.getTargetType().getClassName(),
                        prop.getName()
                );
            }
            builder.endControlFlow();
        } else {
            if (isMapStructLoadedStateRequired(prop)) {
                builder.addStatement("this.$L = true", prop.getLoadedStateName());
            }
            if (prop.isList()) {
                builder.addStatement(
                        "this.$L = $L != null ? $L : $T.emptyList()",
                        prop.getName(),
                        prop.getName(),
                        prop.getName(),
                        Constants.COLLECTIONS_CLASS_NAME
                );
            } else {
                builder.addStatement("this.$L = $L", prop.getName(), prop.getName());
            }
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
            if (prop.isValueRequired()) {
                if (isMapStructLoadedStateRequired(prop)) {
                    builder.beginControlFlow("if ($L)", prop.getLoadedStateName());
                    builder.addStatement("draft.$L($L)", prop.getSetterName(), prop.getName());
                    builder.endControlFlow();
                } else {
                    builder.beginControlFlow("if ($L != null)", prop.getName());
                    builder.addStatement("draft.$L($L)", prop.getSetterName(), prop.getName());
                    builder.endControlFlow();
                }
            }
        }
        builder.addCode("$<});\n");
        typeBuilder.addMethod(builder.build());
    }
    
    private static boolean isMapStructLoadedStateRequired(ImmutableProp prop) {
        return prop.isNullable();
    }
}
