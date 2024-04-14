package org.babyfish.jimmer.apt.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.squareup.javapoet.*;
import org.babyfish.jimmer.apt.Context;
import org.babyfish.jimmer.apt.GeneratorException;
import org.babyfish.jimmer.apt.immutable.generator.Annotations;
import org.babyfish.jimmer.apt.immutable.meta.ImmutableProp;
import org.babyfish.jimmer.apt.immutable.meta.ImmutableType;
import org.babyfish.jimmer.apt.util.ConverterMetadata;
import org.babyfish.jimmer.client.ApiIgnore;
import org.babyfish.jimmer.client.meta.Doc;
import org.babyfish.jimmer.dto.compiler.*;
import org.babyfish.jimmer.impl.util.StringUtil;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.sql.Id;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.processing.Filer;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import java.io.IOException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import java.util.*;

public class DtoGenerator {

    private static final String[] EMPTY_STR_ARR = new String[0];

    private final Context ctx;

    final DtoType<ImmutableType, ImmutableProp> dtoType;

    private final Document document;

    private final Filer filer;

    private final DtoGenerator parent;

    private final DtoGenerator root;

    private final String innerClassName;

    private final Set<String> interfaceMethodNames;

    private TypeSpec.Builder typeBuilder;

    public DtoGenerator(
            Context ctx,
            DtoType<ImmutableType, ImmutableProp> dtoType,
            Filer filer
    ) {
        this(ctx, dtoType, filer, null, null);
    }

    private DtoGenerator(
            Context ctx,
            DtoType<ImmutableType, ImmutableProp> dtoType,
            Filer filer,
            DtoGenerator parent,
            String innerClassName
    ) {
        if ((filer == null) == (parent == null)) {
            throw new IllegalArgumentException("The nullity values of `filer` and `parent` cannot be same");
        }
        if ((parent == null) != (innerClassName == null)) {
            throw new IllegalArgumentException("The nullity values of `parent` and `innerClassName` must be same");
        }
        this.ctx = ctx;
        this.dtoType = dtoType;
        this.document = new Document(ctx, dtoType);
        this.filer = filer;
        this.parent = parent;
        this.root = parent != null ? parent.root : this;
        this.innerClassName = innerClassName;
        this.interfaceMethodNames = DtoInterfaces.abstractMethodNames(ctx, dtoType);
    }

    public void generate() {
        String simpleName = getSimpleName();
        typeBuilder = TypeSpec
                .classBuilder(simpleName)
                .addModifiers(Modifier.PUBLIC);
        if (isImpl() && dtoType.getBaseType().isEntity()) {
            typeBuilder.addSuperinterface(
                    dtoType.getModifiers().contains(DtoModifier.SPECIFICATION) ?
                            ParameterizedTypeName.get(
                                    org.babyfish.jimmer.apt.immutable.generator.Constants.JSPECIFICATION_CLASS_NAME,
                                    dtoType.getBaseType().getClassName(),
                                    dtoType.getBaseType().getTableClassName()
                            ) :
                            ParameterizedTypeName.get(
                                    dtoType.getModifiers().contains(DtoModifier.INPUT) ?
                                            org.babyfish.jimmer.apt.immutable.generator.Constants.INPUT_CLASS_NAME :
                                            org.babyfish.jimmer.apt.immutable.generator.Constants.VIEW_CLASS_NAME,
                                    dtoType.getBaseType().getClassName()
                            )
            );
        }
        for (TypeRef typeRef : dtoType.getSuperInterfaces()) {
            typeBuilder.addSuperinterface(getTypeName(typeRef));
        }
        if (parent == null) {
            typeBuilder.addAnnotation(
                    AnnotationSpec
                            .builder(org.babyfish.jimmer.apt.immutable.generator.Constants.GENERATED_BY_CLASS_NAME)
                            .addMember(
                                    "file",
                                    "$S",
                                    dtoType.getDtoFile().getPath()
                            )
                            .build()
            );
        }
        if (dtoType.getModifiers().contains(DtoModifier.INPUT)) {
            typeBuilder.addAnnotation(
                    AnnotationSpec
                            .builder(org.babyfish.jimmer.apt.immutable.generator.Constants.GENERATED_INPUT_CLASS_NAME)
                            .addMember(
                                    "type",
                                    "$T.$L",
                                    org.babyfish.jimmer.apt.immutable.generator.Constants.GENERATED_INPUT_TYPE_CLASS_NAME,
                                    dtoType
                                            .getModifiers()
                                            .stream()
                                            .filter(DtoModifier::isInputStrategy)
                                            .findFirst()
                                            .orElseThrow(() -> new AssertionError("Internal bug"))
                                            .name()
                                            .toUpperCase()
                            )
                            .build()
            );
            typeBuilder.addAnnotation(
                    AnnotationSpec
                            .builder(org.babyfish.jimmer.apt.immutable.generator.Constants.JSON_DESERIALIZE_CLASS_NAME)
                            .addMember(
                                    "builder",
                                    "$T.class",
                                    getDtoClassName("Builder")
                            )
                            .build()
            );
        }
        String doc = document.get();
        if (doc == null) {
            doc = ctx.getElements().getDocComment(dtoType.getBaseType().getTypeElement());
        }
        if (doc != null) {
            typeBuilder.addJavadoc(doc.replace("$", "$$"));
        }
        for (Anno anno : dtoType.getAnnotations()) {
            typeBuilder.addAnnotation(annotationOf(anno));
        }
        if (innerClassName != null) {
            typeBuilder.addModifiers(Modifier.STATIC);
            addMembers();
        } else {
            addMembers();
        }
        if (innerClassName != null) {
            assert  parent != null;
            parent.typeBuilder.addType(typeBuilder.build());
        } else {
            try {
                JavaFile
                        .builder(
                                root.dtoType.getPackageName(),
                                typeBuilder.build()
                        )
                        .indent("    ")
                        .build()
                        .writeTo(filer);
            } catch (IOException ex) {
                throw new GeneratorException(
                        String.format(
                                "Cannot generate dto type '%s' for '%s'",
                                dtoType.getName(),
                                dtoType.getBaseType().getQualifiedName()
                        ),
                        ex
                );
            }
        }
    }

    public String getSimpleName() {
        return innerClassName != null ? innerClassName : dtoType.getName();
    }

    private ClassName getDtoClassName() {
        return getDtoClassName(null);
    }

    ClassName getDtoClassName(String nestedClassName) {
        if (innerClassName != null) {
            List<String> list = new ArrayList<>();
            collectNames(list);
            List<String> simpleNames = list.subList(1, list.size());
            if (nestedClassName != null) {
                simpleNames = new ArrayList<>(simpleNames);
                simpleNames.add(nestedClassName);
            }
            return ClassName.get(
                    root.dtoType.getPackageName(),
                    list.get(0),
                    simpleNames.toArray(EMPTY_STR_ARR)
            );
        }
        if (nestedClassName == null) {
            return ClassName.get(
                    root.dtoType.getPackageName(),
                    dtoType.getName()
            );
        }
        return ClassName.get(
                root.dtoType.getPackageName(),
                dtoType.getName(),
                nestedClassName
        );
    }

    private void addMembers() {

        boolean isSpecification = dtoType.getModifiers().contains(DtoModifier.SPECIFICATION);
        if (!isSpecification) {
            addMetadata();
        }

        if (!dtoType.getModifiers().contains(DtoModifier.SPECIFICATION)) {
            for (DtoProp<ImmutableType, ImmutableProp> prop : dtoType.getDtoProps()) {
                addAccessorField(prop);
            }
        }
        for (DtoProp<ImmutableType, ImmutableProp> prop : dtoType.getDtoProps()) {
            addField(prop);
            addStateField(prop);
        }
        for (UserProp prop : dtoType.getUserProps()) {
            addField(prop);
        }

        addDefaultConstructor();
        if (!isSpecification) {
            addConverterConstructor();
        }

        for (DtoProp<ImmutableType, ImmutableProp> prop : dtoType.getDtoProps()) {
            addAccessors(prop);
        }
        for (UserProp prop : dtoType.getUserProps()) {
            addAccessors(prop);
        }

        if (dtoType.getModifiers().contains(DtoModifier.SPECIFICATION)) {
            addEntityType();
            addApplyTo();
        } else {
            addToEntity();
        }

        addHashCode();
        addEquals();
        addToString();

        for (DtoProp<ImmutableType, ImmutableProp> prop : dtoType.getDtoProps()) {
            addSpecificationConverter(prop);
        }

        for (DtoProp<ImmutableType, ImmutableProp> prop : dtoType.getDtoProps()) {
            DtoType<ImmutableType, ImmutableProp> targetType = prop.getTargetType();
            if (targetType == null) {
                continue;
            }
            if (!prop.isRecursive() || targetType.isFocusedRecursion()) {
                new DtoGenerator(
                        ctx,
                        targetType,
                        null,
                        this,
                        targetSimpleName(prop)
                ).generate();
            }
        }

        if (dtoType.getModifiers().contains(DtoModifier.INPUT)) {
            new InputBuilderGenerator(this).generate();
        }
    }

    private void addMetadata() {
        FieldSpec.Builder builder = FieldSpec
                .builder(
                        ParameterizedTypeName.get(
                                org.babyfish.jimmer.apt.immutable.generator.Constants.VIEW_METADATA_CLASS_NAME,
                                dtoType.getBaseType().getClassName(),
                                getDtoClassName()
                        ),
                        "METADATA"
                )
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL);
        CodeBlock.Builder cb = CodeBlock
                .builder()
                .indent()
                .add("\n")
                .add(
                        "new $T<$T, $T>(\n",
                        org.babyfish.jimmer.apt.immutable.generator.Constants.VIEW_METADATA_CLASS_NAME,
                        dtoType.getBaseType().getClassName(),
                        getDtoClassName()
                )
                .indent()
                .add("$T.$L", dtoType.getBaseType().getFetcherClassName(), "$")
                .indent();
        for (DtoProp<ImmutableType, ImmutableProp> prop : dtoType.getDtoProps()) {
            if (prop.getNextProp() == null) {
                addFetcherField(prop, cb);
            }
        }
        for (DtoProp<ImmutableType, ImmutableProp> hiddenProp : dtoType.getHiddenFlatProps()) {
            addHiddenFetcherField(hiddenProp, cb);
        }
        cb
                .add(",\n")
                .unindent()
                .add("$T::new\n", getDtoClassName())
                .unindent()
                .unindent()
                .add(")");
        builder.initializer(cb.build());
        typeBuilder.addField(builder.build());
    }

    private void addFetcherField(DtoProp<ImmutableType, ImmutableProp> prop, CodeBlock.Builder cb) {
        if (prop.getBaseProp().getAnnotation(Id.class) == null) {
            if (prop.getTargetType() != null) {
                if (prop.getTargetType() != null) {
                    if (prop.isRecursive()) {
                        cb.add("\n.$N()", StringUtil.identifier("recursive", prop.getBaseProp().getName()));
                    } else {
                        cb.add("\n.$N($T.METADATA.getFetcher())", prop.getBaseProp().getName(), getPropElementName(prop));
                    }
                }
            } else {
                cb.add("\n.$N()", prop.getBaseProp().getName());
            }
        }
    }

    private void addAccessorField(DtoProp<ImmutableType, ImmutableProp> prop) {
        if (isSimpleProp(prop)) {
            return;
        }
        FieldSpec.Builder builder = FieldSpec.builder(
                org.babyfish.jimmer.apt.immutable.generator.Constants.DTO_PROP_ACCESSOR_CLASS_NAME,
                StringUtil.snake(prop.getName() + "Accessor", StringUtil.SnakeCase.UPPER),
                Modifier.PRIVATE,
                Modifier.STATIC,
                Modifier.FINAL
        );
        CodeBlock.Builder cb = CodeBlock.builder();
        cb.add("new $T(", org.babyfish.jimmer.apt.immutable.generator.Constants.DTO_PROP_ACCESSOR_CLASS_NAME);
        cb.indent();

        DtoProp<ImmutableType, ImmutableProp> tailProp = prop.toTailProp();
        if (prop.isNullable() && (
                !prop.toTailProp().getBaseProp().isNullable() ||
                        dtoType.getModifiers().contains(DtoModifier.SPECIFICATION) ||
                        dtoType.getModifiers().contains(DtoModifier.FUZZY))
        ) {
            cb.add("\nfalse");
        } else {
            cb.add("\ntrue");
        }

        if (prop.getNextProp() == null) {
            cb.add(",\nnew int[] { $T.$L }", dtoType.getBaseType().getProducerClassName(), prop.getBaseProp().getSlotName());
        } else {
            cb.add(",\nnew int[] {");
            cb.indent();
            boolean addComma = false;
            for (DtoProp<ImmutableType, ImmutableProp> p = prop; p != null; p = p.getNextProp()) {
                if (addComma) {
                    cb.add(",");
                } else {
                    addComma = true;
                }
                cb.add("\n$T.$L", p.getBaseProp().getDeclaringType().getProducerClassName(), p.getBaseProp().getSlotName());
            }
            cb.unindent();
            cb.add("\n}");
        }

        if (prop.isIdOnly()) {
            if (dtoType.getModifiers().contains(DtoModifier.SPECIFICATION)) {
                cb.add(",\nnull");
            } else {
                cb.add(
                        ",\n$T.$L($T.class, ",
                        org.babyfish.jimmer.apt.immutable.generator.Constants.DTO_PROP_ACCESSOR_CLASS_NAME,
                        tailProp.getBaseProp().isList() ? "idListGetter" : "idReferenceGetter",
                        tailProp.getBaseProp().getTargetType().getClassName()
                );
                addConverterLoading(cb, prop, false);
                cb.add(")");
            }
            cb.add(
                    ",\n$T.$L($T.class, ",
                    org.babyfish.jimmer.apt.immutable.generator.Constants.DTO_PROP_ACCESSOR_CLASS_NAME,
                    tailProp.getBaseProp().isList() ? "idListSetter" : "idReferenceSetter",
                    tailProp.getBaseProp().getTargetType().getClassName()
            );
            addConverterLoading(cb, prop, false);
            cb.add(")");
        } else if (tailProp.getTargetType() != null) {
            if (dtoType.getModifiers().contains(DtoModifier.SPECIFICATION)) {
                cb.add(",\nnull");
            } else {
                cb.add(
                        ",\n$T.<$T, $T>$L($T::new)",
                        org.babyfish.jimmer.apt.immutable.generator.Constants.DTO_PROP_ACCESSOR_CLASS_NAME,
                        tailProp.getBaseProp().getTargetType().getClassName(),
                        getPropElementName(tailProp),
                        tailProp.getBaseProp().isList() ? "objectListGetter" : "objectReferenceGetter",
                        getPropElementName(tailProp)
                );
            }
            cb.add(
                    ",\n$T.$L($T::$L)",
                    org.babyfish.jimmer.apt.immutable.generator.Constants.DTO_PROP_ACCESSOR_CLASS_NAME,
                    tailProp.getBaseProp().isList() ? "objectListSetter" : "objectReferenceSetter",
                    getPropElementName(tailProp),
                    tailProp.getTargetType().getBaseType().isEntity() ? "toEntity" : "toImmutable"
            );
        } else if (prop.getEnumType() != null) {
            EnumType enumType = prop.getEnumType();
            TypeName enumTypeName = tailProp.getBaseProp().getTypeName();
            if (dtoType.getModifiers().contains(DtoModifier.SPECIFICATION)) {
                cb.add(",\nnull");
            } else {
                cb.add(",\narg -> {\n");
                cb.indent();
                cb.beginControlFlow("switch (($T)arg)", enumTypeName);
                for (Map.Entry<String, String> e: enumType.getValueMap().entrySet()) {
                    cb.add("case $L:\n", e.getKey());
                    cb.indent();
                    cb.addStatement("return $L", e.getValue());
                    cb.unindent();
                }
                cb.add("default:\n");
                cb.indent();
                cb.addStatement(
                        "throw new AssertionError($S)",
                        "Internal bug"
                );
                cb.unindent();
                cb.endControlFlow();
                cb.unindent();
                cb.add("}");
            }
            cb.add(",\narg -> {\n");
            cb.indent();
            addValueToEnum(cb, prop, "arg");
            cb.unindent();
            cb.add("}");
        } else if (converterMetadataOf(prop) != null) {
            cb.add(",\narg -> ");
            addConverterLoading(cb, prop, true);
            cb.add(".output(arg)");
            cb.add(",\narg -> ");
            addConverterLoading(cb, prop, true);
            cb.add(".input(arg)");
        }

        cb.unindent();
        cb.add("\n)");
        builder.initializer(cb.build());
        typeBuilder.addField(builder.build());
    }

    private void addValueToEnum(CodeBlock.Builder cb, DtoProp<ImmutableType, ImmutableProp> prop, String variableName) {
        EnumType enumType = prop.getEnumType();
        TypeName enumTypeName = prop.toTailProp().getBaseProp().getTypeName();
        cb.beginControlFlow("switch (($T)$L)", enumType.isNumeric() ? TypeName.INT : org.babyfish.jimmer.apt.immutable.generator.Constants.STRING_CLASS_NAME, variableName);
        for (Map.Entry<String, String> e: enumType.getConstantMap().entrySet()) {
            cb.add("case $L:\n", e.getKey());
            cb.indent();
            cb.addStatement("return $T.$L", enumTypeName, e.getValue());
            cb.unindent();
        }
        cb.add("default:\n");
        cb.indent();
        cb.addStatement(
                "throw new IllegalArgumentException($S + $L + $S)",
                "Illegal value `\"",
                variableName,
                "\"`for enum type: \"" + enumTypeName + "\""
        );
        cb.unindent();
        cb.endControlFlow();
    }

    private void addConverterLoading(CodeBlock.Builder cb, DtoProp<ImmutableType, ImmutableProp> prop, boolean forList) {
        ImmutableProp baseProp = prop.toTailProp().getBaseProp();
        cb.add(
                "$T.$L.unwrap().$L",
                dtoType.getBaseType().getPropsClassName(),
                StringUtil.snake(baseProp.getName(), StringUtil.SnakeCase.UPPER),
                prop.toTailProp().getBaseProp().isAssociation(true) ?
                        "getAssociatedIdConverter(" + forList + ")" :
                        "getConverter()"
        );
    }

    private boolean isSimpleProp(DtoProp<ImmutableType, ImmutableProp> prop) {
        if (prop.getNextProp() != null) {
            return false;
        }
        if (prop.isNullable() && (
                !prop.isBaseNullable() || dtoType.getModifiers().contains(DtoModifier.SPECIFICATION))) {
            return false;
        }
        return getPropTypeName(prop).equals(prop.getBaseProp().getTypeName());
    }

    private void addHiddenFetcherField(DtoProp<ImmutableType, ImmutableProp> prop, CodeBlock.Builder cb) {
        if (!"flat".equals(prop.getFuncName())) {
            addFetcherField(prop, cb);
            return;
        }
        DtoType<ImmutableType, ImmutableProp> targetDtoType = prop.getTargetType();
        assert targetDtoType != null;
        cb.add("\n.$N($>", prop.getBaseProp().getName());
        cb.add("$T.$L$>", prop.getBaseProp().getTargetType().getFetcherClassName(), "$");
        for (DtoProp<ImmutableType, ImmutableProp> childProp : targetDtoType.getDtoProps()) {
            addHiddenFetcherField(childProp, cb);
        }
        cb.add("$<$<\n)");
    }

    private void addField(DtoProp<ImmutableType, ImmutableProp> prop) {
        TypeName typeName = getPropTypeName(prop);
        if (isFieldNullable(prop)) {
            typeName = typeName.box();
        }
        FieldSpec.Builder builder = FieldSpec
                .builder(typeName, prop.getName())
                .addModifiers(Modifier.PRIVATE);
        for (AnnotationMirror annotationMirror : prop.getBaseProp().getAnnotations()) {
            if (isCopyableAnnotation(annotationMirror, false) &&
                    prop.getAnnotations().stream().noneMatch(
                            it -> it.getQualifiedName().equals(Annotations.qualifiedName(annotationMirror))
                    )
            ) {
                builder.addAnnotation(AnnotationSpec.get(annotationMirror));
            }
        }
        for (Anno anno : prop.getAnnotations()) {
            if (hasElementType(anno, ElementType.FIELD)) {
                builder.addAnnotation(annotationOf(anno));
            }
        }
        typeBuilder.addField(builder.build());
    }

    private void addField(UserProp prop) {
        TypeName typeName = getPropTypeName(prop);
        if (isFieldNullable(prop)) {
            typeName = typeName.box();
        }
        FieldSpec.Builder builder = FieldSpec
                .builder(typeName, prop.getAlias())
                .addModifiers(Modifier.PRIVATE);
        for (Anno anno : prop.getAnnotations()) {
            if (hasElementType(anno, ElementType.FIELD)) {
                builder.addAnnotation(annotationOf(anno));
            }
        }
        typeBuilder.addField(builder.build());
    }

    private void addStateField(DtoProp<ImmutableType, ImmutableProp> prop) {
        String stateFieldName = stateFieldName(prop);
        if (stateFieldName == null) {
            return;
        }
        typeBuilder.addField(
                TypeName.BOOLEAN,
                stateFieldName,
                Modifier.PRIVATE
        );
    }

    @SuppressWarnings("unchecked")
    private void addAccessors(AbstractProp prop) {
        TypeName typeName = getPropTypeName(prop);
        String getterName = getterName(prop);
        String setterName = setterName(prop);
        String stateFieldName = stateFieldName(prop);

        MethodSpec.Builder getterBuilder = MethodSpec
                .methodBuilder(getterName)
                .addModifiers(Modifier.PUBLIC)
                .returns(typeName);
        if (interfaceMethodNames.contains(getterName)) {
            getterBuilder.addAnnotation(Override.class);
        }
        String doc = document.get(prop);
        if (prop instanceof DtoProp<?, ?>) {
            DtoProp<ImmutableType, ImmutableProp> dtoProp = (DtoProp<ImmutableType, ImmutableProp>) prop;
            if (doc == null && dtoProp.getBasePropMap().size() == 1 && dtoProp.getFuncName() == null) {
                doc = ctx.getElements().getDocComment(dtoProp.getBaseProp().toElement());
            }
        }
        if (doc != null) {
            getterBuilder.addJavadoc(doc.replace("$", "$$"));
        }
        if (!typeName.isPrimitive()) {
            if (prop.isNullable()) {
                getterBuilder.addAnnotation(Nullable.class);
            } else {
                getterBuilder.addAnnotation(NotNull.class);
            }
        }
        if (prop instanceof DtoProp<?, ?>) {
            DtoProp<ImmutableType, ImmutableProp> dtoProp = (DtoProp<ImmutableType, ImmutableProp>) prop;
            for (AnnotationMirror annotationMirror : dtoProp.getBaseProp().getAnnotations()) {
                if (isCopyableAnnotation(annotationMirror, true) &&
                        prop.getAnnotations().stream().noneMatch(
                                it -> it.getQualifiedName().equals(Annotations.qualifiedName(annotationMirror))
                        )
                ) {
                    getterBuilder.addAnnotation(AnnotationSpec.get(annotationMirror));
                }
            }
        }
        for (Anno anno : prop.getAnnotations()) {
            if (hasElementType(anno, ElementType.METHOD)) {
                getterBuilder.addAnnotation(annotationOf(anno));
            }
        }
        if (stateFieldName != null) {
            getterBuilder.beginControlFlow(
                    "if ($L)",
                    '!' + stateFieldName
            );
            getterBuilder.addStatement(
                    "throw new IllegalStateException($S)",
                    "The property \"" + prop.getName() + "\" is not specified"
            );
            getterBuilder.endControlFlow();
        }
        if (!prop.isNullable() && isFieldNullable(prop)) {
            getterBuilder.beginControlFlow(
                    "if ($L == null)",
                    prop.getName()
            );
            getterBuilder.addStatement(
                    "throw new IllegalStateException($S)",
                    "The property \"" + prop.getName() + "\" is not specified"
            );
            getterBuilder.endControlFlow();
        }
        getterBuilder.addStatement("return $L", prop.getName());
        typeBuilder.addMethod(getterBuilder.build());

        ParameterSpec.Builder parameterBuilder = ParameterSpec.builder(typeName, prop.getName());
        if (!typeName.isPrimitive()) {
            if (prop.isNullable()) {
                parameterBuilder.addAnnotation(Nullable.class);
            } else {
                parameterBuilder.addAnnotation(NotNull.class);
            }
        }
        MethodSpec.Builder setterBuilder = MethodSpec
                .methodBuilder(setterName)
                .addParameter(parameterBuilder.build())
                .addModifiers(Modifier.PUBLIC);
        if (interfaceMethodNames.contains(setterName)) {
            setterBuilder.addAnnotation(Override.class);
        }
        setterBuilder.addStatement("this.$L = $L", prop.getName(), prop.getName());
        if (stateFieldName != null) {
            setterBuilder.addStatement("this.$L = true", stateFieldName);
        }
        typeBuilder.addMethod(setterBuilder.build());

        if (stateFieldName != null) {
            MethodSpec.Builder isLoadedBuilder = MethodSpec
                    .methodBuilder(StringUtil.identifier("is", prop.getName(), "Loaded"))
                    .returns(TypeName.BOOLEAN)
                    .addModifiers(Modifier.PUBLIC)
                    .addAnnotation(ApiIgnore.class)
                    .addAnnotation(JsonIgnore.class)
                    .addStatement("return this.$L", stateFieldName);
            typeBuilder.addMethod(isLoadedBuilder.build());
            MethodSpec.Builder setLoadedBuilder = MethodSpec
                    .methodBuilder(StringUtil.identifier("set", prop.getName(), "Loaded"))
                    .addParameter(TypeName.BOOLEAN, "loaded")
                    .addStatement("this.$L = loaded", stateFieldName);
            typeBuilder.addMethod(setLoadedBuilder.build());
        }
    }

    private void addDefaultConstructor() {
        MethodSpec.Builder builder = MethodSpec
                .constructorBuilder()
                .addModifiers(Modifier.PUBLIC);
        typeBuilder.addMethod(builder.build());
    }

    private void addConverterConstructor() {
        MethodSpec.Builder builder = MethodSpec
                .constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addParameter(
                        ParameterSpec
                                .builder(dtoType.getBaseType().getClassName(), "base")
                                .addAnnotation(NotNull.class)
                                .build()
                );
        for (DtoProp<ImmutableType, ImmutableProp> prop : dtoType.getDtoProps()) {
            if (isSimpleProp(prop)) {
                if (prop.isNullable()) {
                    builder.addStatement(
                            "this.$L = (($T)base).__isLoaded($T.byIndex($T.$L)) ? base.$L() : null",
                            prop.getName(),
                            ImmutableSpi.class,
                            org.babyfish.jimmer.apt.immutable.generator.Constants.PROP_ID_CLASS_NAME,
                            dtoType.getBaseType().getProducerClassName(),
                            prop.getBaseProp().getSlotName(),
                            prop.getBaseProp().getGetterName()
                    );
                } else {
                    builder.addStatement(
                            "this.$L = base.$L()",
                            prop.getName(),
                            prop.getBaseProp().getGetterName()
                    );
                }
            } else {
                if (!prop.isNullable() && prop.isBaseNullable()) {
                    builder.addStatement(
                            "this.$L = $L.get($>\n" +
                                    "base,\n" +
                                    "$S\n" +
                                    "$<)",
                            prop.getName(),
                            StringUtil.snake(prop.getName() + "Accessor", StringUtil.SnakeCase.UPPER),
                            "Cannot convert \"" +
                                    dtoType.getBaseType().getClassName() +
                                    "\" to " +
                                    "\"" +
                                    getDtoClassName() +
                                    "\" because the cannot get non-null " +
                                    "value for \"" +
                                    prop.getName() +
                                    "\""
                    );
                } else {
                    builder.addStatement(
                            "this.$L = $L.get(base)",
                            prop.getName(),
                            StringUtil.snake(prop.getName() + "Accessor", StringUtil.SnakeCase.UPPER)
                    );
                }
            }
        }
        typeBuilder.addMethod(builder.build());
    }

    private void addToEntity() {
        MethodSpec.Builder builder = MethodSpec
                .methodBuilder(dtoType.getBaseType().isEntity() ? "toEntity" : "toImmutable")
                .addModifiers(Modifier.PUBLIC)
                .returns(dtoType.getBaseType().getClassName());
        if (dtoType.getBaseType().isEntity()) {
            builder.addAnnotation(Override.class);
        }
        builder.addCode(
                "return $T.$L.produce(__draft -> {$>\n",
                dtoType.getBaseType().getDraftClassName(),
                "$"
        );
        for (DtoProp<ImmutableType, ImmutableProp> prop : dtoType.getDtoProps()) {
            if (prop.getBaseProp().isJavaFormula()) {
                continue;
            }
            String stateFieldName = stateFieldName(prop);
            if (stateFieldName != null) {
                builder.beginControlFlow("if ($L)", stateFieldName);
            }
            if (isSimpleProp(prop)) {
                builder.addStatement("__draft.$L($L)", prop.getBaseProp().getSetterName(), prop.getName());
            } else {
                ImmutableProp tailBaseProp = prop.toTailProp().getBaseProp();
                if (tailBaseProp.isList() && tailBaseProp.isAssociation(true)) {
                    builder.addStatement(
                            "$L.set(__draft, $L != null ? $L : $T.emptyList())",
                            StringUtil.snake(prop.getName() + "Accessor", StringUtil.SnakeCase.UPPER),
                            prop.getName(),
                            prop.getName(),
                            org.babyfish.jimmer.apt.immutable.generator.Constants.COLLECTIONS_CLASS_NAME
                    );
                } else {
                    builder.addStatement(
                            "$L.set(__draft, $L)",
                            StringUtil.snake(prop.getName() + "Accessor", StringUtil.SnakeCase.UPPER),
                            prop.getName()
                    );
                }
            }
            if (stateFieldName != null) {
                builder.endControlFlow();
            }
        }
        builder.addCode("$<});\n");
        typeBuilder.addMethod(builder.build());
    }

    private void addEntityType() {
        MethodSpec.Builder builder = MethodSpec
                .methodBuilder("entityType")
                .returns(
                        ParameterizedTypeName.get(
                                org.babyfish.jimmer.apt.immutable.generator.Constants.CLASS_CLASS_NAME,
                                dtoType.getBaseType().getClassName()
                        )
                )
                .addModifiers(Modifier.PUBLIC)
                .addStatement("return $T.class", dtoType.getBaseType().getClassName());
        if (isImpl()) {
            builder.addAnnotation(Override.class);
        }
        typeBuilder.addMethod(builder.build());
    }

    private void addApplyTo() {
        MethodSpec.Builder builder = MethodSpec
                .methodBuilder("applyTo")
                .addModifiers(Modifier.PUBLIC);
        if (isImpl()) {
            builder.addAnnotation(Override.class)
                    .addParameter(
                            ParameterSpec.builder(
                                    ParameterizedTypeName.get(
                                            org.babyfish.jimmer.apt.immutable.generator.Constants.SPECIFICATION_ARGS_CLASS_NAME,
                                            dtoType.getBaseType().getClassName(),
                                            dtoType.getBaseType().getTableClassName()
                                    ),
                                    "args"
                            ).build()
                    );
        } else {
            builder.addParameter(
                    ParameterSpec
                            .builder(
                                    org.babyfish.jimmer.apt.immutable.generator.Constants.PREDICATE_APPLIER_CLASS_NAME,
                                    "__applier"
                            )
                            .build()
            );
        }

        List<ImmutableProp> stack = Collections.emptyList();
        if (isImpl()) {
            builder.addStatement(
                    "$T __applier = args.getApplier()",
                    org.babyfish.jimmer.apt.immutable.generator.Constants.PREDICATE_APPLIER_CLASS_NAME
            );
        }
        for (DtoProp<ImmutableType, ImmutableProp> prop : dtoType.getDtoProps()) {
            List<ImmutableProp> newStack = new ArrayList<>(stack.size() + 2);
            DtoProp<ImmutableType, ImmutableProp> tailProp = prop.toTailProp();
            for (DtoProp<ImmutableType, ImmutableProp> p = prop; p != null; p = p.getNextProp()) {
                if (p != tailProp || p.getTargetType() != null) {
                    newStack.add(p.getBaseProp());
                }
            }
            stack = addStackOperations(builder, stack, newStack);
            addPredicateOperation(builder, tailProp);
        }
        addStackOperations(builder, stack, Collections.emptyList());
        typeBuilder.addMethod(builder.build());
    }

    private List<ImmutableProp> addStackOperations(
            MethodSpec.Builder builder,
            List<ImmutableProp> stack,
            List<ImmutableProp> newStack
    ) {
        int size = Math.min(stack.size(), newStack.size());
        int sameCount = size;
        for (int i = 0; i < size; i++) {
            if (stack.get(i) != newStack.get(i)) {
                sameCount = i;
                break;
            }
        }
        for (int i = stack.size() - sameCount; i > 0; --i) {
            builder.addStatement("__applier.pop()");
        }
        for (ImmutableProp prop : newStack.subList(sameCount, newStack.size())) {
            builder.addStatement(
                    "__applier.push($T.$L.unwrap())",
                    prop.getDeclaringType().getPropsClassName(),
                    StringUtil.snake(prop.getName(), StringUtil.SnakeCase.UPPER)
            );
        }
        return newStack;
    }

    private void addPredicateOperation(MethodSpec.Builder builder, DtoProp<ImmutableType, ImmutableProp> prop) {

        if (prop.getTargetType() != null) {
            builder.beginControlFlow("if (this.$L != null)", prop.getName());
            if (prop.getTargetType().getBaseType().isEntity()) {
                builder.addStatement("this.$L.applyTo(args.child())", prop.getName());
            } else {
                builder.addStatement("this.$L.applyTo(args.getApplier())", prop.getName());
            }
            builder.endControlFlow();
            return;
        }

        String funcName = prop.getFuncName();
        String javaMethodName = funcName;
        if (funcName == null) {
            funcName = "eq";
            javaMethodName = "eq";
        } else if ("null".equals(funcName)) {
            javaMethodName = "isNull";
        } else if ("notNull".equals(funcName)) {
            javaMethodName = "isNotNull";
        } else if ("id".equals(funcName)) {
            funcName = "associatedIdEq";
            javaMethodName = "associatedIdEq";
        }

        CodeBlock.Builder cb = CodeBlock.builder();
        if (org.babyfish.jimmer.dto.compiler.Constants.MULTI_ARGS_FUNC_NAMES.contains(funcName)) {
            cb.add("__applier.$L(new $T[] { ", javaMethodName, org.babyfish.jimmer.apt.immutable.generator.Constants.IMMUTABLE_PROP_CLASS_NAME);
            boolean addComma = false;
            for (ImmutableProp baseProp : prop.getBasePropMap().values()) {
                if (addComma) {
                    cb.add(", ");
                } else {
                    addComma = true;
                }
                cb.add(
                        "$T.$L.unwrap()",
                        baseProp.getDeclaringType().getPropsClassName(),
                        StringUtil.snake(baseProp.getName(), StringUtil.SnakeCase.UPPER)
                );
            }
            cb.add(" }, ");
        } else {
            cb.add(
                    "__applier.$L($T.$L.unwrap(), ",
                    funcName,
                    prop.getBaseProp().getDeclaringType().getPropsClassName(),
                    StringUtil.snake(prop.getBaseProp().getName(), StringUtil.SnakeCase.UPPER)
            );
        }
        if (isSpecificationConverterRequired(prop)) {
            cb.add(
                    "$L(this.$L)",
                    StringUtil.identifier("__convert", prop.getName()),
                    prop.getName()
            );
        } else {
            cb.add("this.$L", prop.getName());
        }
        if ("like".equals(funcName) || "notLike".equals(funcName)) {
            cb.add(", ");
            cb.add(prop.getLikeOptions().contains(LikeOption.INSENSITIVE) ? "true" : "false");
            cb.add(", ");
            cb.add(prop.getLikeOptions().contains(LikeOption.MATCH_START) ? "true" : "false");
            cb.add(", ");
            cb.add(prop.getLikeOptions().contains(LikeOption.MATCH_END) ? "true" : "false");
        }
        cb.addStatement(")");
        builder.addCode(cb.build());
    }

    private void addSpecificationConverter(DtoProp<ImmutableType, ImmutableProp> prop) {
        if (!isSpecificationConverterRequired(prop)) {
            return;
        }
        ImmutableProp baseProp = prop.toTailProp().getBaseProp();
        TypeName baseTypeName = null;
        String funcName = prop.getFuncName();
        if (funcName != null) {
            switch (funcName) {
                case "id":
                    baseTypeName = baseProp.getTargetType().getIdProp().getTypeName();
                    if (baseProp.isList() && !dtoType.getModifiers().contains(DtoModifier.SPECIFICATION)) {
                        baseTypeName = ParameterizedTypeName.get(
                                org.babyfish.jimmer.apt.immutable.generator.Constants.LIST_CLASS_NAME,
                                baseTypeName.box()
                        );
                    }
                    break;
                case "valueIn":
                case "valueNotIn":
                    baseTypeName = ParameterizedTypeName.get(
                            org.babyfish.jimmer.apt.immutable.generator.Constants.LIST_CLASS_NAME,
                            baseProp.getTypeName().box()
                    );
                    break;
                case "associatedIdEq":
                case "associatedIdNe":
                    baseTypeName = baseProp.getTargetType().getIdProp().getTypeName();
                    break;
                case "associatedIdIn":
                case "associatedIdNotIn":
                    baseTypeName = ParameterizedTypeName.get(
                            org.babyfish.jimmer.apt.immutable.generator.Constants.LIST_CLASS_NAME,
                            baseProp.getTargetType().getIdProp().getTypeName().box()
                    );
            }
        }
        if (baseTypeName == null) {
            baseTypeName = baseProp.getTypeName();
        }
        baseTypeName = baseTypeName.box();
        MethodSpec.Builder builder = MethodSpec
                .methodBuilder(StringUtil.identifier("__convert", prop.getName()))
                .addModifiers(Modifier.PRIVATE)
                .addParameter(getPropTypeName(prop), "value")
                .returns(baseTypeName);
        CodeBlock.Builder cb = CodeBlock.builder();
        cb.beginControlFlow("if ($L == null)", prop.getName());
        cb.addStatement("return null");
        cb.endControlFlow();
        if (prop.getEnumType() != null) {
            addValueToEnum(cb, prop, "value");
        } else {
            cb.addStatement(
                    "return $T.$L.unwrap().<$T, $T>$L.input(value)",
                    dtoType.getBaseType().getPropsClassName(),
                    StringUtil.snake(baseProp.getName(), StringUtil.SnakeCase.UPPER),
                    baseTypeName,
                    getPropTypeName(prop).box(),
                    baseProp.isAssociation(true) ?
                            "getAssociatedIdConverter(true)" :
                            "getConverter(" + (prop.isFunc("valueIn", "valueNotIn") ? "true" : "") + ")"
            );
        }
        builder.addCode(cb.build());
        typeBuilder.addMethod(builder.build());
    }

    @SuppressWarnings("unchecked")
    public TypeName getPropTypeName(AbstractProp prop) {
        if (prop instanceof DtoProp<?, ?>) {
            return getPropTypeName((DtoProp<ImmutableType, ImmutableProp>) prop);
        }
        return getTypeName(((UserProp)prop).getTypeRef());
    }

    private TypeName getPropTypeName(DtoProp<ImmutableType, ImmutableProp> prop) {

        ImmutableProp baseProp = prop.toTailProp().getBaseProp();

        EnumType enumType = prop.getEnumType();
        if (enumType != null) {
            if (enumType.isNumeric()) {
                return prop.isNullable() ? TypeName.INT.box() : TypeName.INT;
            }
            return org.babyfish.jimmer.apt.immutable.generator.Constants.STRING_CLASS_NAME;
        }
        ConverterMetadata metadata = converterMetadataOf(prop);
        if (dtoType.getModifiers().contains(DtoModifier.SPECIFICATION)) {
            String funcName = prop.toTailProp().getFuncName();
            if (funcName != null) {
                switch (funcName) {
                    case "null":
                    case "notNull":
                        return TypeName.BOOLEAN;
                    case "valueIn":
                    case "valueNotIn":
                        return ParameterizedTypeName.get(
                                org.babyfish.jimmer.apt.immutable.generator.Constants.COLLECTION_CLASS_NAME,
                                metadata != null ?
                                        metadata.getTargetTypeName() :
                                        toListType(
                                            getPropElementName(prop),
                                            baseProp.isList()
                                        )
                        );
                    case "id":
                    case "associatedIdEq":
                    case "associatedIdNe":
                        final TypeName clientTypeName = baseProp.getTargetType().getIdProp().getClientTypeName();
                        if (prop.isNullable()) {
                            return clientTypeName.box();
                        }
                        return clientTypeName;
                    case "associatedIdIn":
                    case "associatedIdNotIn":
                        return ParameterizedTypeName.get(
                                org.babyfish.jimmer.apt.immutable.generator.Constants.COLLECTION_CLASS_NAME,
                                baseProp.getTargetType().getIdProp().getClientTypeName().box()
                        );
                }
            }
            if (baseProp.isAssociation(true)) {
                return getPropElementName(prop);
            }
        }
        if (metadata != null) {
            return metadata.getTargetTypeName();
        }

        return toListType(getPropElementName(prop), baseProp.isList());
    }

    private static TypeName toListType(TypeName typeName, boolean isList) {
        return isList ?
                ParameterizedTypeName.get(
                        org.babyfish.jimmer.apt.immutable.generator.Constants.LIST_CLASS_NAME,
                        typeName.box()
                ) :
                typeName;
    }

    private static TypeName getTypeName(@Nullable TypeRef typeRef) {
        if (typeRef == null) {
            return WildcardTypeName.subtypeOf(TypeName.OBJECT);
        }
        TypeName typeName;
        switch (typeRef.getTypeName()) {
            case "Boolean":
                typeName = typeRef.isNullable() ? TypeName.BOOLEAN.box() : TypeName.BOOLEAN;
                break;
            case "Char":
                typeName = typeRef.isNullable() ? TypeName.CHAR.box() : TypeName.CHAR;
                break;
            case "Byte":
                typeName = typeRef.isNullable() ? TypeName.BYTE.box() : TypeName.BYTE;
                break;
            case "Short":
                typeName = typeRef.isNullable() ? TypeName.SHORT.box() : TypeName.SHORT;
                break;
            case "Int":
                typeName = typeRef.isNullable() ? TypeName.INT.box() : TypeName.INT;
                break;
            case "Long":
                typeName = typeRef.isNullable() ? TypeName.LONG.box() : TypeName.LONG;
                break;
            case "Float":
                typeName = typeRef.isNullable() ? TypeName.FLOAT.box() : TypeName.FLOAT;
                break;
            case "Double":
                typeName = typeRef.isNullable() ? TypeName.DOUBLE.box() : TypeName.DOUBLE;
                break;
            case "Any":
                typeName = TypeName.OBJECT;
                break;
            case "String":
                typeName = org.babyfish.jimmer.apt.immutable.generator.Constants.STRING_CLASS_NAME;
                break;
            case "Array":
                typeName = ArrayTypeName.of(
                        typeRef.getArguments().get(0).getTypeRef() == null ?
                                TypeName.OBJECT :
                                getTypeName(typeRef.getArguments().get(0).getTypeRef())
                );
                break;
            case "Iterable":
            case "MutableIterable":
                typeName = ClassName.get(Iterable.class);
                break;
            case "Collection":
            case "MutableCollection":
                typeName = ClassName.get(Collection.class);
                break;
            case "List":
            case "MutableList":
                typeName = ClassName.get(List.class);
                break;
            case "Set":
            case "MutableSet":
                typeName = ClassName.get(Set.class);
                break;
            case "Map":
            case "MutableMap":
                typeName = ClassName.get(Map.class);
                break;
            default:
                typeName = ClassName.bestGuess(typeRef.getTypeName());
                break;
        }
        int argCount = typeRef.getArguments().size();
        if (argCount == 0 || typeName instanceof ArrayTypeName) {
            return typeName;
        }
        TypeName[] argTypeNames = new TypeName[argCount];
        for (int i = 0; i < argCount; i++) {
            TypeRef.Argument arg = typeRef.getArguments().get(i);
            TypeName argTypeName = getTypeName(arg.getTypeRef());
            if (arg.isIn()) {
                argTypeName = WildcardTypeName.supertypeOf(argTypeName);
            } else if (arg.getTypeRef() != null && (arg.isOut() || isForceOut(typeRef.getTypeName()))) {
                argTypeName = WildcardTypeName.subtypeOf(argTypeName);
            }
            argTypeNames[i] = argTypeName;
        }
        return ParameterizedTypeName.get(
                (ClassName) typeName,
                argTypeNames
        );
    }

    private void addHashCode() {
        MethodSpec.Builder builder = MethodSpec
                .methodBuilder("hashCode")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .returns(TypeName.INT);
        boolean first = true;
        for (DtoProp<ImmutableType, ImmutableProp> prop : dtoType.getDtoProps()) {
            CodeBlock.Builder cb = CodeBlock.builder();
            if (first) {
                cb.add("int hash = ");
                first = false;
            } else {
                cb.add("hash = hash * 31 + ");
            }
            TypeName typeName = getPropTypeName(prop);
            if (typeName.isPrimitive()) {
                cb.add(
                        "$T.hashCode($L)",
                        typeName.box(),
                        prop.getName().equals("hash") ? "this." + prop.getName() : prop.getName()
                );
            } else {
                cb.add("$T.hashCode($L)", Objects.class, prop.getName());
            }
            builder.addStatement(cb.build());
            String stateFieldName = stateFieldName(prop);
            if (stateFieldName != null) {
                builder.addStatement("hash = hash * 31 + Boolean.hashCode($L)", stateFieldName);
            }
        }
        for (UserProp prop : dtoType.getUserProps()) {
            CodeBlock.Builder cb = CodeBlock.builder();
            if (first) {
                cb.add("int hash = ");
                first = false;
            } else {
                cb.add("hash = hash * 31 + ");
            }
            TypeName typeName = getTypeName(prop.getTypeRef());
            if (typeName.isPrimitive()) {
                cb.add(
                        "$T.hashCode($L)",
                        typeName.box(),
                        prop.getAlias().equals("hash") ? "this." + prop.getAlias() : prop.getAlias()
                );
            } else {
                cb.add("$T.hashCode($L)", Objects.class, prop.getAlias());
            }
            builder.addStatement(cb.build());
        }
        builder.addStatement(first ? "return 0" : "return hash");
        typeBuilder.addMethod(builder.build());
    }

    private void addEquals() {
        MethodSpec.Builder builder = MethodSpec
                .methodBuilder("equals")
                .addModifiers(Modifier.PUBLIC)
                .addParameter(TypeName.OBJECT, "o")
                .addAnnotation(Override.class)
                .returns(TypeName.BOOLEAN);
        builder.beginControlFlow("if (o == null || this.getClass() != o.getClass())")
                .addStatement("return false")
                .endControlFlow();
        builder.addStatement("$L other = ($L) o", getSimpleName(), getSimpleName());
        for (DtoProp<ImmutableType, ImmutableProp> prop : dtoType.getDtoProps()) {
            String propName = prop.getName();
            String stateFieldName = stateFieldName(prop);
            if (stateFieldName != null) {
                builder.beginControlFlow("if ($L != other.$L)", stateFieldName, stateFieldName);
                builder.addStatement("return false");
                builder.endControlFlow();
            }
            String thisProp = propName.equals("o") || propName.equals("other") ? "this" + propName : propName;
            if (stateFieldName != null) {
                if (getPropTypeName(prop).isPrimitive()) {
                    builder.beginControlFlow(
                            "if ($L && $L != other.$L)",
                            stateFieldName,
                            thisProp,
                            propName
                    );
                } else {
                    builder.beginControlFlow(
                            "if ($L && !$T.equals($L, other.$L))",
                            stateFieldName,
                            Objects.class,
                            thisProp,
                            propName
                    );
                }
            } else {
                if (getPropTypeName(prop).isPrimitive()) {
                    builder.beginControlFlow("if ($L != other.$L)", thisProp, propName);
                } else {
                    builder.beginControlFlow("if (!$T.equals($L, other.$L))", Objects.class, thisProp, propName);
                }
            }
            builder.addStatement("return false");
            builder.endControlFlow();
        }
        for (UserProp prop : dtoType.getUserProps()) {
            String propName = prop.getAlias();
            String thisProp = propName.equals("o") || propName.equals("other") ? "this" + propName : propName;
            if (getTypeName(prop.getTypeRef()).isPrimitive()) {
                builder.beginControlFlow("if ($L != other.$L)", thisProp, propName);
            } else {
                builder.beginControlFlow("if (!$T.equals($L, other.$L))", Objects.class, thisProp, propName);
            }
            builder.addStatement("return false");
            builder.endControlFlow();
        }
        builder.addStatement("return true");
        typeBuilder.addMethod(builder.build());
    }

    private void addToString() {
        MethodSpec.Builder builder = MethodSpec
                .methodBuilder("toString")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .returns(org.babyfish.jimmer.apt.immutable.generator.Constants.STRING_CLASS_NAME);
        builder.addStatement("StringBuilder builder = new StringBuilder()");
        builder.addStatement("builder.append($S).append('(')", simpleNamePath());
        String separator = "";
        for (DtoProp<ImmutableType, ImmutableProp> prop : dtoType.getDtoProps()) {
            String stateFieldName = stateFieldName(prop);
            if (stateFieldName != null) {
                builder.beginControlFlow("if ($L)", stateFieldName);
            } else if (prop.getInputModifier() == DtoModifier.FUZZY) {
                builder.beginControlFlow("if ($L != null)", prop.getName());
            }
            if (prop.getName().equals("builder")) {
                builder.addStatement("builder.append($S).append(this.$L)", separator + prop.getName() + '=', prop.getName());
            } else {
                builder.addStatement("builder.append($S).append($L)", separator + prop.getName() + '=', prop.getName());
            }
            if (stateFieldName != null || prop.getInputModifier() == DtoModifier.FUZZY) {
                builder.endControlFlow();
            }
            separator = ", ";
        }
        for (UserProp prop : dtoType.getUserProps()) {
            if (prop.getAlias().equals("builder")) {
                builder.addStatement("builder.append($S).append(this.$L)", separator + prop.getAlias() + '=', prop.getAlias());
            } else {
                builder.addStatement("builder.append($S).append($L)", separator + prop.getAlias() + '=', prop.getAlias());
            }
            separator = ", ";
        }
        builder.addStatement("builder.append(')')");
        builder.addStatement("return builder.toString()");
        typeBuilder.addMethod(builder.build());
    }

    private String simpleNamePath() {
        String name = getSimpleName();
        if (parent != null) {
            return parent.simpleNamePath() + '.' + name;
        }
        return name;
    }

    public TypeName getPropElementName(DtoProp<ImmutableType, ImmutableProp> prop) {
        DtoProp<ImmutableType, ImmutableProp> tailProp = prop.toTailProp();
        DtoType<ImmutableType, ImmutableProp> targetType = tailProp.getTargetType();
        if (targetType != null) {
            if (tailProp.isRecursive() && !targetType.isFocusedRecursion()) {
                return getDtoClassName();
            }
            if (targetType.getName() == null) {
                List<String> list = new ArrayList<>();
                collectNames(list);
                if (!tailProp.isRecursive() || targetType.isFocusedRecursion()) {
                    list.add(targetSimpleName(tailProp));
                }
                return ClassName.get(
                        root.dtoType.getPackageName(),
                        list.get(0),
                        list.subList(1, list.size()).toArray(EMPTY_STR_ARR)
                );
            }
            return ClassName.get(
                    root.dtoType.getPackageName(),
                    targetType.getName()
            );
        }
        ImmutableProp baseProp = tailProp.getBaseProp();
        TypeName typeName;
        if (tailProp.isIdOnly()) {
            typeName = tailProp.getBaseProp().getTargetType().getIdProp().getTypeName();
        } else if (baseProp.getIdViewBaseProp() != null) {
            typeName = baseProp.getIdViewBaseProp().getTargetType().getIdProp().getClientTypeName();
        } else {
            typeName = tailProp.getBaseProp().getClientTypeName();
        }
        if (typeName.isPrimitive() && prop.isNullable()) {
            return typeName.box();
        }
        return typeName;
    }

    private void collectNames(List<String> list) {
        if (parent == null) {
            list.add(dtoType.getName());
        } else {
            parent.collectNames(list);
            list.add(innerClassName);
        }
    }

    private String targetSimpleName(DtoProp<ImmutableType, ImmutableProp> prop) {
        DtoType<ImmutableType, ImmutableProp> targetType = prop.getTargetType();
        if (targetType == null) {
            throw new IllegalArgumentException("prop is not association");
        }
        if (targetType.getName() != null) {
            return targetType.getName();
        }
        if (prop.isRecursive() && !targetType.isFocusedRecursion()) {
            return innerClassName != null ? innerClassName : dtoType.getName();
        }
        return standardTargetSimpleName("TargetOf_" + prop.getName());
    }

    private String standardTargetSimpleName(String targetSimpleName) {
        boolean conflict = false;
        for (DtoGenerator generator = this; generator != null; generator = generator.parent) {
            if (generator.getSimpleName().equals(targetSimpleName)) {
                conflict = true;
                break;
            }
        }
        if (!conflict) {
            return targetSimpleName;
        }
        for (int i = 2; i < 100; i++) {
            conflict = false;
            String newTargetSimpleName = targetSimpleName + '_' + i;
            for (DtoGenerator generator = this; generator != null; generator = generator.parent) {
                if (generator.getSimpleName().equals(newTargetSimpleName)) {
                    conflict = true;
                    break;
                }
            }
            if (!conflict) {
                return newTargetSimpleName;
            }
        }
        throw new AssertionError("Dto is too deep");
    }

    private boolean isSpecificationConverterRequired(DtoProp<ImmutableType, ImmutableProp> prop) {
        if (!dtoType.getModifiers().contains(DtoModifier.SPECIFICATION)) {
            return false;
        }
        return prop.getEnumType() != null || converterMetadataOf(prop) != null;
    }

    private ConverterMetadata converterMetadataOf(DtoProp<ImmutableType, ImmutableProp> prop) {
        ImmutableProp baseProp = prop.toTailProp().getBaseProp();
        ConverterMetadata metadata = baseProp.getConverterMetadata();
        if (metadata != null) {
            return metadata;
        }
        String funcName = prop.getFuncName();
        if ("id".equals(funcName)) {
            metadata = baseProp.getTargetType().getIdProp().getConverterMetadata();
            if (metadata != null && baseProp.isList() && !dtoType.getModifiers().contains(DtoModifier.SPECIFICATION)) {
                metadata = metadata.toListMetadata(baseProp.context());
            }
            return metadata;
        }
        if ("associatedInEq".equals(funcName) || "associatedInNe".equals(funcName)) {
            return baseProp.getTargetType().getIdProp().getConverterMetadata();
        }
        if ("associatedIdIn".equals(funcName) || "associatedIdNotIn".equals(funcName)) {
            metadata = baseProp.getTargetType().getIdProp().getConverterMetadata();
            if (metadata != null) {
                return metadata.toListMetadata(baseProp.context());
            }
        }
        if (baseProp.getIdViewBaseProp() != null) {
            metadata = baseProp.getIdViewBaseProp().getTargetType().getIdProp().getConverterMetadata();
            if (metadata != null) {
                return baseProp.isList() ? metadata.toListMetadata(baseProp.context()) : metadata;
            }
        }
        return null;
    }

    private boolean hasElementType(Anno anno, ElementType elementType) {
        TypeElement annoElement = ctx.getElements().getTypeElement(anno.getQualifiedName());
        if (annoElement == null) {
            throw new DtoException(
                    "Cannot find the annotation declaration whose type is \"" +
                            anno.getQualifiedName() +
                            "\""
            );
        }
        Target target = annoElement.getAnnotation(Target.class);
        if (target != null) {
            for (ElementType et : target.value()) {
                if (et == elementType) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isCopyableAnnotation(AnnotationMirror annotationMirror, boolean forMethod) {
        Target target = annotationMirror.getAnnotationType().asElement().getAnnotation(Target.class);
        if (target != null) {
            boolean acceptField = Arrays.stream(target.value()).anyMatch(it -> it == (forMethod ? ElementType.METHOD : ElementType.FIELD));
            if (acceptField) {
                String qualifiedName = ((TypeElement) annotationMirror.getAnnotationType().asElement()).getQualifiedName().toString();
                if (isNullityAnnotation(qualifiedName)) {
                    return false;
                }
                return !qualifiedName.startsWith("org.babyfish.jimmer.") ||
                        qualifiedName.startsWith("org.babyfish.jimmer.client.");
            }
        }
        return false;
    }

    private static boolean isNullityAnnotation(String qualifiedName) {
        int lastDotIndex = qualifiedName.lastIndexOf('.');
        if (lastDotIndex != -1) {
            qualifiedName = qualifiedName.substring(lastDotIndex + 1);
        }
        switch (qualifiedName) {
            case "Null":
            case "Nullable":
            case "NotNull":
            case "NonNull":
                return true;
            default:
                return false;
        }
    }

    private static AnnotationSpec annotationOf(Anno anno) {
        AnnotationSpec.Builder builder = AnnotationSpec
                .builder(ClassName.bestGuess(anno.getQualifiedName()));
        for (Map.Entry<String, Anno.Value> e : anno.getValueMap().entrySet()) {
            String name = e.getKey();
            Anno.Value value = e.getValue();
            builder.addMember(name, codeBlockOf(value));
        }
        return builder.build();
    }

    private static CodeBlock codeBlockOf(Anno.Value value) {
        CodeBlock.Builder builder = CodeBlock.builder();
        if (value instanceof Anno.ArrayValue) {
            builder.add("{\n$>");
            boolean addSeparator = false;
            for (Anno.Value element : ((Anno.ArrayValue)value).elements) {
                if (addSeparator) {
                    builder.add(", \n");
                } else {
                    addSeparator = true;
                }
                builder.add("$L", codeBlockOf(element));
            }
            builder.add("$<\n}");
        } else if (value instanceof Anno.AnnoValue) {
            builder.add("$L", annotationOf(((Anno.AnnoValue)value).anno));
        } else if(value instanceof Anno.TypeRefValue) {
            builder.add("$T.class", getTypeName(((Anno.TypeRefValue)value).typeRef));
        } else if (value instanceof Anno.EnumValue) {
            builder.add(
                    "$T.$L",
                    ClassName.bestGuess(((Anno.EnumValue)value).qualifiedName),
                    ((Anno.EnumValue)value).constant
            );
        } else if (value instanceof Anno.LiteralValue) {
            builder.add(((Anno.LiteralValue)value).value);
        }
        return builder.build();
    }

    private static boolean isForceOut(String typeName) {
        switch (typeName) {
            case "Iterable":
            case "Collection":
            case "List":
            case "Set":
            case "Map":
                return true;
            default:
                return false;
        }
    }

    @SuppressWarnings("unchecked")
    private String getterName(AbstractProp prop) {
        TypeName typeName = prop instanceof DtoProp<?, ?> ?
                getPropTypeName((DtoProp<ImmutableType, ImmutableProp>) prop) :
                getTypeName(((UserProp)prop).getTypeRef());
        String suffix = prop instanceof DtoProp<?, ?> ?
                ((DtoProp<ImmutableType, ImmutableProp>)prop).getName() :
                prop.getAlias();
        if (suffix.startsWith("is") &&
                suffix.length() > 2 &&
                Character.isUpperCase(suffix.charAt(2)) &&
                typeName.equals(TypeName.BOOLEAN)) {
            suffix = suffix.substring(2);
        }
        return StringUtil.identifier(
                typeName.equals(TypeName.BOOLEAN) ? "is" : "get",
                suffix
        );
    }

    @SuppressWarnings("unchecked")
    private String setterName(AbstractProp prop) {
        TypeName typeName = prop instanceof DtoProp<?, ?> ?
                getPropTypeName((DtoProp<ImmutableType, ImmutableProp>) prop) :
                getTypeName(((UserProp)prop).getTypeRef());
        String suffix = prop instanceof DtoProp<?, ?> ?
                ((DtoProp<ImmutableType, ImmutableProp>)prop).getName() :
                prop.getAlias();
        if (suffix.startsWith("is") &&
                suffix.length() > 2 &&
                Character.isUpperCase(suffix.charAt(2)) &&
                typeName.equals(TypeName.BOOLEAN)) {
            suffix = suffix.substring(2);
        }
        return StringUtil.identifier("set", suffix);
    }

    private class Document {

        private final Context ctx;

        private final Doc dtoTypeDoc;

        private final Doc baseTypeDoc;

        private String result;

        public Document(Context ctx, DtoType<ImmutableType, ImmutableProp> dtoType) {
            this.ctx = ctx;
            dtoTypeDoc = Doc.parse(dtoType.getDoc());
            baseTypeDoc = Doc.parse(ctx.getElements().getDocComment(dtoType.getBaseType().getTypeElement()));
        }

        public String get() {
            String ret = result;
            if (ret == null) {
                if (dtoTypeDoc != null) {
                    ret = dtoTypeDoc.toString();
                } else if (baseTypeDoc != null) {
                    ret = baseTypeDoc.toString();
                } else {
                    ret = "";
                }
                ret = ret.replace("$", "$$");
                this.result = ret;
            }
            return ret.isEmpty() ? null : ret;
        }

        public String get(AbstractProp prop) {
            String value = getImpl(prop);
            if (value == null) {
                return null;
            }
            return value.replace("$", "$$");
        }

        @SuppressWarnings("unchecked")
        private String getImpl(AbstractProp prop) {
            ImmutableProp baseProp;
            if (prop instanceof DtoProp<?, ?>) {
                baseProp = ((DtoProp<?, ImmutableProp>) prop).getBaseProp();
            } else {
                baseProp = null;
            }
            if (prop.getDoc() != null) {
                Doc doc = Doc.parse(prop.getDoc());
                if (doc != null) {
                    return doc.toString();
                }
            }
            if (dtoTypeDoc != null) {
                String name = prop.getAlias();
                if (name == null) {
                    assert baseProp != null;
                    name = baseProp.getName();
                }
                String doc = dtoTypeDoc.getParameterValueMap().get(name);
                if (doc != null) {
                    return doc;
                }
            }
            if (baseProp != null) {
                Doc doc = Doc.parse(ctx.getElements().getDocComment(baseProp.toElement()));
                if (doc != null) {
                    return doc.toString();
                }
            }
            if (baseTypeDoc != null && baseProp != null) {
                String doc = baseTypeDoc.getParameterValueMap().get(baseProp.getName());
                if (doc != null) {
                    return doc;
                }
            }
            return null;
        }
    }

    private boolean isImpl() {
        return dtoType.getBaseType().isEntity() ||
                !dtoType.getModifiers().contains(DtoModifier.SPECIFICATION);
    }

    TypeSpec.Builder getTypeBuilder() {
        return typeBuilder;
    }

    @Nullable
    String stateFieldName(AbstractProp prop) {
        if (!prop.isNullable()) {
            return null;
        }
        if (!(prop instanceof DtoProp<?, ?>)) {
            return null;
        }
        if (!dtoType.getModifiers().contains(DtoModifier.INPUT)) {
            return null;
        }
        DtoModifier modifier = ((DtoProp<?, ?>) prop).getInputModifier();
        if (modifier == DtoModifier.STATIC || modifier == DtoModifier.FUZZY) {
            return null;
        }
        return StringUtil.identifier("_is", prop.getName(), "Loaded");
    }

    private static boolean isFieldNullable(AbstractProp prop) {
        if (prop instanceof DtoProp<?, ?>) {
            String funcName = ((DtoProp<?, ?>) prop).getFuncName();
            return !"null".equals(funcName) && !"notNull".equals(funcName);
        }
        return true;
    }
}
