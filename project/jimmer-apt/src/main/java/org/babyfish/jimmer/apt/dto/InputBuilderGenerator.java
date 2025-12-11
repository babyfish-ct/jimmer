package org.babyfish.jimmer.apt.dto;

import com.squareup.javapoet.*;
import org.babyfish.jimmer.apt.immutable.generator.Constants;
import org.babyfish.jimmer.apt.immutable.meta.ImmutableProp;
import org.babyfish.jimmer.apt.immutable.meta.ImmutableType;
import org.babyfish.jimmer.dto.compiler.*;
import org.babyfish.jimmer.impl.util.StringUtil;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import java.util.*;

public class InputBuilderGenerator {

    private static final String[] JACKSON_ANNO_PREFIXIES = new String[] {
            "com.fasterxml.jackson.databind.annotation.",
            "com.fasterxml.jackson.annotation."
    };

    private final DtoGenerator parentGenerator;

    private final DtoType<ImmutableType, ImmutableProp> dtoType;

    private TypeSpec.Builder typeBuilder;

    public InputBuilderGenerator(DtoGenerator parentGenerator) {
        this.parentGenerator = parentGenerator;
        this.dtoType = parentGenerator.dtoType;
    }

    public void generate() {
        typeBuilder = TypeSpec
                .classBuilder("Builder")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC);
        try {
            addAnnotations();
            addMembers();
            parentGenerator.getTypeBuilder().addType(typeBuilder.build());
        } finally {
            typeBuilder = null;
        }
    }

    private void addAnnotations() {
        typeBuilder.addAnnotation(
                AnnotationSpec
                        .builder(parentGenerator.ctx.getJacksonTypes().jsonPojoBuilder)
                        .addMember("withPrefix", "$S", "")
                        .build()
        );

        for (Anno annotation : dtoType.getAnnotations()) {
            if (annotation.getQualifiedName().equals(parentGenerator.ctx.getJacksonTypes().jsonNaming.canonicalName())) {
                if (!annotation.getValueMap().containsKey("value")) {
                    continue;
                }
                typeBuilder.addAnnotation(
                        AnnotationSpec
                                .builder(parentGenerator.ctx.getJacksonTypes().jsonNaming)
                                .addMember(
                                        "value", "$T.class",
                                        ClassName.bestGuess(((Anno.TypeRefValue) annotation.getValueMap().get("value")).typeRef.getTypeName()))
                                .build()
                );
            }
        }
    }

    private void addMembers() {
        for (DtoProp<ImmutableType, ImmutableProp> prop : dtoType.getDtoProps()) {
            addField(prop);
            addStateField(prop);
        }
        for (UserProp prop : dtoType.getUserProps()) {
            addField(prop);
        }
        for (DtoProp<ImmutableType, ImmutableProp> prop : dtoType.getDtoProps()) {
            addSetter(prop);
        }
        for (UserProp prop : dtoType.getUserProps()) {
            addSetter(prop);
        }
        addBuild();
    }

    private void addField(AbstractProp prop) {
        TypeName typeName = parentGenerator.getPropTypeName(prop);
        if (isFieldNullable(prop)) {
            typeName = typeName.box();
        }
        FieldSpec.Builder builder = FieldSpec
                .builder(typeName, prop.getName())
                .addModifiers(Modifier.PRIVATE);
        typeBuilder.addField(builder.build());
    }

    private void addStateField(AbstractProp prop) {
        String stateFieldName = parentGenerator.stateFieldName(prop, true);
        if (stateFieldName == null) {
            return;
        }
        FieldSpec.Builder builder = FieldSpec
                .builder(TypeName.BOOLEAN, stateFieldName)
                .addModifiers(Modifier.PRIVATE);
        typeBuilder.addField(builder.build());
    }

    private void addSetter(AbstractProp prop) {
        String stateFieldName = parentGenerator.stateFieldName(prop, true);
        MethodSpec.Builder builder = MethodSpec
                .methodBuilder(prop.getName())
                .addModifiers(Modifier.PUBLIC)
                .addParameter(parentGenerator.getPropTypeName(prop), prop.getName())
                .returns(parentGenerator.getDtoClassName("Builder"));
        addJacksonAnnotations(prop, builder);
        if (prop.isNullable()) {
            builder.addStatement(
                    "this.$L = $L",
                    prop.getName(),
                    prop.getName()
            );
            if (stateFieldName != null) {
                builder.addStatement("this.$L = true", stateFieldName);
            }
        } else {
            builder.addStatement(
                    "this.$L = $T.requireNonNull($L, $S)",
                    prop.getName(),
                    Constants.OBJECTS_CLASS_NAME,
                    prop.getName(),
                    "The property \"" +
                            prop.getName() +
                            "\" cannot be null"
            );
        }
        builder.addStatement("return this");
        typeBuilder.addMethod(builder.build());
    }

    @SuppressWarnings("unchecked")
    private void addJacksonAnnotations(
            AbstractProp prop,
            MethodSpec.Builder builder
    ) {
        Set<String> typeNames = new HashSet<>();
        for (Anno anno : prop.getAnnotations()) {
            if (isJacksonQualifiedName(anno.getQualifiedName()) && typeNames.add(anno.getQualifiedName())) {
                builder.addAnnotation(DtoGenerator.annotationOf(anno));
            }
        }
        if (prop instanceof DtoProp<?, ?>) {
            DtoProp<?, ImmutableProp> dtoProp = (DtoProp<?, ImmutableProp>) prop;
            for (AnnotationMirror mirror : dtoProp.toTailProp().getBaseProp().getAnnotations()) {
                String qualifiedName = ((TypeElement) mirror.getAnnotationType()
                        .asElement())
                        .getQualifiedName()
                        .toString();
                if (isJacksonQualifiedName(qualifiedName) && typeNames.add(qualifiedName)) {
                    builder.addAnnotation(AnnotationSpec.get(mirror));
                }
            }
        }
    }

    private void addBuild() {
        ClassName dtoClassName = parentGenerator.getDtoClassName(null);
        MethodSpec.Builder builder = MethodSpec
                .methodBuilder("build")
                .addModifiers(Modifier.PUBLIC)
                .returns(dtoClassName);
        builder.addStatement("$T _input = new $T()", dtoClassName, dtoClassName);
        for (DtoProp<ImmutableType, ImmutableProp> prop : dtoType.getDtoProps()) {
            if (!prop.isNullable()) {
                builder.beginControlFlow("if ($L == null)", prop.getName());
                builder.addStatement(
                        "throw $T.$L($T.class, $S)",
                        Constants.INPUT_CLASS_NAME,
                        "unknownNonNullProperty",
                        dtoClassName,
                        prop.getName()
                );
                builder.endControlFlow();
                builder.addStatement(
                        "_input.$L($L)",
                        setterName(prop),
                        prop.getName()
                );
            } else {
                String stateFieldName = parentGenerator.stateFieldName(prop, true);
                switch (prop.getInputModifier()) {
                    case FIXED:
                        builder.beginControlFlow("if (!$L)", stateFieldName);
                        builder.addStatement(
                                "throw $T.$L($T.class, $S)",
                                Constants.INPUT_CLASS_NAME,
                                "unknownNullableProperty",
                                dtoClassName,
                                prop.getName()
                        );
                        builder.endControlFlow();
                    case STATIC:
                        builder.addStatement(
                                "_input.$L($L)",
                                setterName(prop),
                                prop.getName()
                        );
                        break;
                    case DYNAMIC:
                        builder.beginControlFlow("if ($L)", stateFieldName);
                        builder.addStatement(
                                "_input.$L($L)",
                                setterName(prop),
                                prop.getName()
                        );
                        builder.endControlFlow();
                        break;
                    case FUZZY:
                        builder.beginControlFlow("if ($L != null)", prop.getName());
                        builder.addStatement(
                                "_input.$L($L)",
                                setterName(prop),
                                prop.getName()
                        );
                        builder.endControlFlow();
                        break;
                }
            }
        }
        for (UserProp prop : dtoType.getUserProps()) {
            builder.addStatement(
                    "_input.$L($L)",
                    setterName(prop),
                    prop.getName()
            );
        }
        builder.addStatement("return _input");
        typeBuilder.addMethod(builder.build());
    }

    private static boolean isFieldNullable(AbstractProp prop) {
        if (prop instanceof DtoProp<?, ?>) {
            String funcName = ((DtoProp<?, ?>) prop).getFuncName();
            return !"null".equals(funcName) && !"notNull".equals(funcName);
        }
        return true;
    }

    private static boolean isJacksonQualifiedName(String qualifiedName) {
        for (String prefix : JACKSON_ANNO_PREFIXIES) {
            if (qualifiedName.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    private static String setterName(DtoProp<?, ImmutableProp> prop) {
        String name = prop.getName();
        if (name.length() > 2 &&
                name.startsWith("is") &&
                Character.isUpperCase(name.charAt(2)) &&
                prop.toTailProp().getBaseProp().getTypeName().equals(TypeName.BOOLEAN)) {
            return StringUtil.identifier("set", prop.getName().substring(2));
        }
        return StringUtil.identifier("set", prop.getName());
    }

    private static String setterName(UserProp prop) {
        String name = prop.getName();
        if (name.length() > 2 &&
                name.startsWith("is") &&
                Character.isUpperCase(name.charAt(2)) &&
                TypeRef.TN_BOOLEAN.equals(prop.getTypeRef().getTypeName())) {
            return StringUtil.identifier("set", prop.getName().substring(2));
        }
        return StringUtil.identifier("set", prop.getName());
    }
}
