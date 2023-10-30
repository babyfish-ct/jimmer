package org.babyfish.jimmer.apt.generator;

import com.squareup.javapoet.*;
import org.babyfish.jimmer.apt.GeneratorException;
import org.babyfish.jimmer.apt.meta.ImmutableProp;
import org.babyfish.jimmer.apt.meta.ImmutableType;
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

    private final DtoType<ImmutableType, ImmutableProp> dtoType;

    private final Filer filer;

    private final DtoGenerator parent;

    private final DtoGenerator root;

    private final String innerClassName;

    private TypeSpec.Builder typeBuilder;

    public DtoGenerator(
            DtoType<ImmutableType, ImmutableProp> dtoType,
            Filer filer
    ) {
        this(dtoType, filer, null, null);
    }

    private DtoGenerator(
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
        this.dtoType = dtoType;
        this.filer = filer;
        this.parent = parent;
        this.root = parent != null ? parent.root : this;
        this.innerClassName = innerClassName;
    }

    public void generate() {
        String simpleName = getSimpleName();
        typeBuilder = TypeSpec
                .classBuilder(simpleName)
                .addModifiers(Modifier.PUBLIC)
                .addSuperinterface(
                        ParameterizedTypeName.get(
                                dtoType.getModifiers().contains(DtoTypeModifier.SPECIFICATION) ?
                                        Constants.INPUT_CLASS_NAME :
                                        dtoType.getModifiers().contains(DtoTypeModifier.INPUT) ?
                                                Constants.VIEWABLE_INPUT_CLASS_NAME :
                                                Constants.VIEW_CLASS_NAME,
                                dtoType.getBaseType().getClassName()
                        )
                );
        if (parent == null) {
            typeBuilder.addAnnotation(
                    AnnotationSpec
                            .builder(Constants.GENERATED_BY_CLASS_NAME)
                            .addMember(
                                    "file",
                                    "$S",
                                    dtoType.getPath()
                            )
                            .build()
            );
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
            parent.typeBuilder.addType(typeBuilder.build());
        } else {
            try {
                JavaFile
                        .builder(
                                getPackageName(),
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

    public String getPackageName() {
        String pkg = dtoType.getBaseType().getPackageName();
        return pkg.isEmpty() ? "dto" : pkg + ".dto";
    }

    public String getSimpleName() {
        return innerClassName != null ? innerClassName : dtoType.getName();
    }

    public ClassName getDtoClassName(String ... nestedNames) {
        if (innerClassName != null) {
            List<String> list = new ArrayList<>();
            collectNames(list);
            list.addAll(Arrays.asList(nestedNames));
            return ClassName.get(
                    root.getPackageName(),
                    list.get(0),
                    list.subList(1, list.size()).toArray(EMPTY_STR_ARR)
            );
        }
        return ClassName.get(
                root.getPackageName(),
                dtoType.getName(),
                nestedNames
        );
    }

    private void addMembers() {

        boolean inputOnly = dtoType.getModifiers().contains(DtoTypeModifier.SPECIFICATION);
        if (!inputOnly) {
            addMetadata();
        }

        for (DtoProp<ImmutableType, ImmutableProp> prop : dtoType.getDtoProps()) {
            addAccessorField(prop);
        }
        for (DtoProp<ImmutableType, ImmutableProp> prop : dtoType.getDtoProps()) {
            addField(prop);
        }
        for (UserProp prop : dtoType.getUserProps()) {
            addField(prop);
        }

        addDefaultConstructor();
        if (!inputOnly) {
            addConverterConstructor();
            addOf();
        }

        for (DtoProp<ImmutableType, ImmutableProp> prop : dtoType.getDtoProps()) {
            addAccessors(prop);
        }
        for (UserProp prop : dtoType.getUserProps()) {
            addAccessors(prop);
        }

        addToEntity();

        addHashCode();
        addEquals();
        addToString();

        for (DtoProp<ImmutableType, ImmutableProp> prop : dtoType.getDtoProps()) {
            if (prop.isNewTarget() && prop.getTargetType() != null && prop.getTargetType().getName() == null) {
                new DtoGenerator(
                        prop.getTargetType(),
                        null,
                        this,
                        targetSimpleName(prop)
                ).generate();
            }
        }
    }

    private void addMetadata() {
        FieldSpec.Builder builder = FieldSpec
                .builder(
                        ParameterizedTypeName.get(
                                Constants.VIEW_METADATA_CLASS_NAME,
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
                        Constants.VIEW_METADATA_CLASS_NAME,
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
                if (prop.isNewTarget()) {
                    cb.add("\n.$N($T.METADATA.getFetcher()", prop.getBaseProp().getName(), getPropElementName(prop));
                    if (prop.isRecursive()) {
                        cb.add(", $T::recursive", Constants.RECURSIVE_FIELD_CONFIG_CLASS_NAME);
                    }
                    cb.add(")");
                }
            } else {
                cb.add("\n.$N()", prop.getBaseProp().getName());
            }
        }
    }

    private void addAccessorField(DtoProp<ImmutableType, ImmutableProp> prop) {
        if (prop.isUnmapped() || isSimpleProp(prop)) {
            return;
        }
        FieldSpec.Builder builder = FieldSpec.builder(
                Constants.DTO_PROP_ACCESSOR_CLASS_NAME,
                StringUtil.snake(prop.getName() + "Accessor", StringUtil.SnakeCase.UPPER),
                Modifier.PRIVATE,
                Modifier.STATIC,
                Modifier.FINAL
        );
        CodeBlock.Builder cb = CodeBlock.builder();
        cb.add("new $T(", Constants.DTO_PROP_ACCESSOR_CLASS_NAME);
        cb.indent();

        DtoProp<ImmutableType, ImmutableProp> tailProp = prop.toTailProp();
        if (prop.isNullable() && (
                !tailProp.getBaseProp().isNullable() ||
                        dtoType.getModifiers().contains(DtoTypeModifier.SPECIFICATION))
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
            if (dtoType.getModifiers().contains(DtoTypeModifier.SPECIFICATION)) {
                cb.add(",\nnull");
            } else {
                cb.add(
                        ",\n$T.$L($T.class)",
                        Constants.DTO_PROP_ACCESSOR_CLASS_NAME,
                        tailProp.getBaseProp().isList() ? "idListGetter" : "idReferenceGetter",
                        tailProp.getBaseProp().getTargetType().getClassName()
                );
            }
            cb.add(
                    ",\n$T.$L($T.class)",
                    Constants.DTO_PROP_ACCESSOR_CLASS_NAME,
                    tailProp.getBaseProp().isList() ? "idListSetter" : "idReferenceSetter",
                    tailProp.getBaseProp().getTargetType().getClassName()
            );
        } else if (tailProp.getTargetType() != null) {
            if (dtoType.getModifiers().contains(DtoTypeModifier.SPECIFICATION)) {
                cb.add(",\nnull");
            } else {
                cb.add(
                        ",\n$T.<$T, $L>$L($L::new)",
                        Constants.DTO_PROP_ACCESSOR_CLASS_NAME,
                        tailProp.getBaseProp().getTargetType().getClassName(),
                        targetSimpleName(tailProp),
                        tailProp.getBaseProp().isList() ? "objectListGetter" : "objectReferenceGetter",
                        targetSimpleName(tailProp)
                );
            }
            cb.add(
                    ",\n$T.$L($L::toEntity)",
                    Constants.DTO_PROP_ACCESSOR_CLASS_NAME,
                    tailProp.getBaseProp().isList() ? "objectListSetter" : "objectReferenceSetter",
                    targetSimpleName(tailProp)
            );
        } else if (prop.getEnumType() != null) {
            EnumType enumType = prop.getEnumType();
            TypeName enumTypName = tailProp.getBaseProp().getTypeName();
            if (dtoType.getModifiers().contains(DtoTypeModifier.SPECIFICATION)) {
                cb.add(",\nnull");
            } else {
                cb.add(",\narg -> {\n");
                cb.indent();
                cb.beginControlFlow("switch (($T)arg)", enumTypName);
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
            cb.beginControlFlow("switch (($T)arg)", enumType.isNumeric() ? TypeName.INT : Constants.STRING_CLASS_NAME);
            for (Map.Entry<String, String> e: enumType.getConstantMap().entrySet()) {
                cb.add("case $L:\n", e.getKey());
                cb.indent();
                cb.addStatement("return $T.$L", enumTypName, e.getValue());
                cb.unindent();
            }
            cb.add("default:\n");
            cb.indent();
            cb.addStatement(
                    "throw new IllegalArgumentException($S + arg + $S)",
                    "Illegal value `\"",
                    "\"`for enum type: \"" + enumTypName + "\""
            );
            cb.unindent();
            cb.endControlFlow();
            cb.unindent();
            cb.add("}");
        }

        cb.unindent();
        cb.add("\n)");
        builder.initializer(cb.build());
        typeBuilder.addField(builder.build());
    }

    private boolean isSimpleProp(DtoProp<ImmutableType, ImmutableProp> prop) {
        if (prop.getNextProp() != null) {
            return false;
        }
        if (prop.isNullable() && (
                !prop.getBaseProp().isNullable() || dtoType.getModifiers().contains(DtoTypeModifier.SPECIFICATION))) {
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
        FieldSpec.Builder builder = FieldSpec
                .builder(typeName, prop.getName())
                .addModifiers(Modifier.PRIVATE);
        boolean hasNullity = false;
        if (prop.getAnnotations().isEmpty()) {
            for (AnnotationMirror annotationMirror : prop.getBaseProp().getAnnotations()) {
                if (isCopyableAnnotation(annotationMirror, false)) {
                    builder.addAnnotation(AnnotationSpec.get(annotationMirror));
                    hasNullity |= isNullityAnnotation(
                            annotationMirror.getAnnotationType().asElement().getSimpleName().toString()
                    );
                }
            }
        } else {
            for (Anno anno : prop.getAnnotations()) {
                builder.addAnnotation(annotationOf(anno));
                hasNullity |= isNullityAnnotation(anno.getQualifiedName());
            }
        }
        if (!hasNullity && !typeName.isPrimitive()) {
            if (prop.isNullable()) {
                builder.addAnnotation(Nullable.class);
            } else {
                builder.addAnnotation(NotNull.class);
            }
        }
        typeBuilder.addField(builder.build());
    }

    private void addAccessors(DtoProp<ImmutableType, ImmutableProp> prop) {
        TypeName typeName = getPropTypeName(prop);
        String suffix = prop.getName();
        if (suffix.startsWith("is") &&
                suffix.length() > 2 &&
                Character.isUpperCase(suffix.charAt(2)) &&
                typeName.equals(TypeName.BOOLEAN)) {
            suffix = suffix.substring(2);
        }
        MethodSpec.Builder getterBuilder = MethodSpec
                .methodBuilder(
                        StringUtil.identifier(
                                typeName.equals(TypeName.BOOLEAN) ? "is" : "get",
                                suffix
                        )
                )
                .addModifiers(Modifier.PUBLIC)
                .returns(typeName);
        if (!typeName.isPrimitive()) {
            if (prop.isNullable()) {
                getterBuilder.addAnnotation(Nullable.class);
            } else {
                getterBuilder.addAnnotation(NotNull.class);
            }
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
                .methodBuilder(StringUtil.identifier("set", suffix))
                .addParameter(parameterBuilder.build())
                .addModifiers(Modifier.PUBLIC);
        setterBuilder.addStatement("this.$L = $L", prop.getName(), prop.getName());
        typeBuilder.addMethod(setterBuilder.build());
    }

    private void addField(UserProp prop) {
        TypeName typeName = getTypeName(prop.getTypeRef());
        FieldSpec.Builder builder = FieldSpec
                .builder(typeName, prop.getAlias())
                .addModifiers(Modifier.PRIVATE);
        for (Anno anno : prop.getAnnotations()) {
            builder.addAnnotation(annotationOf(anno));
        }
        if (!typeName.isPrimitive()) {
            if (prop.getTypeRef().isNullable()) {
                builder.addAnnotation(Nullable.class);
            } else {
                builder.addAnnotation(NotNull.class);
            }
        }
        typeBuilder.addField(builder.build());
    }

    private void addAccessors(UserProp prop) {
        TypeName typeName = getTypeName(prop.getTypeRef());
        String suffix = prop.getAlias();
        if (suffix.startsWith("is") &&
                suffix.length() > 2 &&
                Character.isUpperCase(suffix.charAt(2)) &&
                typeName.equals(TypeName.BOOLEAN)) {
            suffix = suffix.substring(2);
        }
        MethodSpec.Builder getterBuilder = MethodSpec
                .methodBuilder(
                        StringUtil.identifier(
                                typeName.equals(TypeName.BOOLEAN) ? "is" : "get",
                                suffix
                        )
                )
                .addModifiers(Modifier.PUBLIC)
                .returns(typeName);
        if (!typeName.isPrimitive()) {
            if (prop.getTypeRef().isNullable()) {
                getterBuilder.addAnnotation(Nullable.class);
            } else {
                getterBuilder.addAnnotation(NotNull.class);
            }
        }
        getterBuilder.addStatement("return $L", prop.getAlias());
        typeBuilder.addMethod(getterBuilder.build());

        ParameterSpec.Builder parameterBuilder = ParameterSpec.builder(typeName, prop.getAlias());
        if (!typeName.isPrimitive()) {
            if (prop.getTypeRef().isNullable()) {
                parameterBuilder.addAnnotation(Nullable.class);
            } else {
                parameterBuilder.addAnnotation(NotNull.class);
            }
        }
        MethodSpec.Builder setterBuilder = MethodSpec
                .methodBuilder(StringUtil.identifier("set", suffix))
                .addParameter(parameterBuilder.build())
                .addModifiers(Modifier.PUBLIC);
        setterBuilder.addStatement("this.$L = $L", prop.getAlias(), prop.getAlias());
        typeBuilder.addMethod(setterBuilder.build());
    }

    private void addOf() {
        MethodSpec.Builder builder = MethodSpec
                .methodBuilder("of")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addParameter(
                        ParameterSpec
                                .builder(dtoType.getBaseType().getClassName(), "base")
                                .addAnnotation(NotNull.class)
                                .build()
                )
                .returns(getDtoClassName())
                .addCode("return new $T(base);", getDtoClassName());
        typeBuilder.addMethod(builder.build());
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
                            Constants.PROP_ID_CLASS_NAME,
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
                ImmutableProp tailBaseProp = prop.toTailProp().getBaseProp();
                if (!prop.isNullable() && tailBaseProp.isAssociation(true) && tailBaseProp.isList()) {
                    builder.addStatement(
                            "$T __$L = $L.get(base)",
                            getPropTypeName(prop),
                            prop.getName(),
                            StringUtil.snake(prop.getName() + "Accessor", StringUtil.SnakeCase.UPPER)
                    );
                    builder.addStatement(
                            "this.$L = __$L != null ? __$L : $T.emptyList()",
                            prop.getName(),
                            prop.getName(),
                            prop.getName(),
                            Constants.COLLECTIONS_CLASS_NAME
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
                .methodBuilder("toEntity")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .returns(dtoType.getBaseType().getClassName());
        builder.addCode(
                "return $T.$L.produce(__draft -> {$>\n",
                dtoType.getBaseType().getDraftClassName(),
                "$"
        );
        for (DtoProp<ImmutableType, ImmutableProp> prop : dtoType.getDtoProps()) {
            if (prop.isUnmapped()) {
                continue;
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
                            Constants.COLLECTIONS_CLASS_NAME
                    );
                } else {
                    builder.addStatement(
                            "$L.set(__draft, $L)",
                            StringUtil.snake(prop.getName() + "Accessor", StringUtil.SnakeCase.UPPER),
                            prop.getName()
                    );
                }
            }
        }
        builder.addCode("$<});\n");
        typeBuilder.addMethod(builder.build());
    }

    public TypeName getPropTypeName(DtoProp<ImmutableType, ImmutableProp> prop) {
        EnumType enumType = prop.getEnumType();
        if (enumType != null) {
            if (enumType.isNumeric()) {
                return prop.isNullable() ? TypeName.INT.box() : TypeName.INT;
            }
            return Constants.STRING_CLASS_NAME;
        }
        TypeName elementTypeName = getPropElementName(prop);
        return prop.toTailProp().getBaseProp().isList() ?
                ParameterizedTypeName.get(
                        Constants.LIST_CLASS_NAME,
                        elementTypeName.isPrimitive() ?
                                elementTypeName.box() :
                                elementTypeName
                ) :
                elementTypeName;
    }

    public TypeName getTypeName(@Nullable TypeRef typeRef) {
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
                typeName = Constants.STRING_CLASS_NAME;
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
        builder.addStatement("return hash");
        typeBuilder.addMethod(builder.build());
    }

    private void addEquals() {
        MethodSpec.Builder builder = MethodSpec
                .methodBuilder("equals")
                .addModifiers(Modifier.PUBLIC)
                .addParameter(TypeName.OBJECT, "o")
                .returns(TypeName.BOOLEAN);
        builder.beginControlFlow("if (o == null || this.getClass() != o.getClass())")
                .addStatement("return false")
                .endControlFlow();
        builder.addStatement("$L other = ($L) o", getSimpleName(), getSimpleName());
        for (DtoProp<ImmutableType, ImmutableProp> prop : dtoType.getDtoProps()) {
            String propName = prop.getName();
            String thisProp = propName.equals("o") || propName.equals("other") ? "this" + propName : propName;
            if (getPropTypeName(prop).isPrimitive()) {
                builder.beginControlFlow("if ($L != other.$L)", thisProp, propName);
            } else {
                builder.beginControlFlow("if (!$T.equals($L, other.$L))", Objects.class, thisProp, propName);
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
                .returns(Constants.STRING_CLASS_NAME);
        builder.addStatement("StringBuilder builder = new StringBuilder()");
        addSimpleName(builder, true);
        String separator = "";
        for (DtoProp<ImmutableType, ImmutableProp> prop : dtoType.getDtoProps()) {
            if (prop.getName().equals("builder")) {
                builder.addStatement("builder.append($S).append(this.$L)", separator + prop.getName() + '=', prop.getName());
            } else {
                builder.addStatement("builder.append($S).append($L)", separator + prop.getName() + '=', prop.getName());
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

    private void addSimpleName(MethodSpec.Builder builder, boolean isLeaf) {
        if (parent != null) {
            parent.addSimpleName(builder, false);
        }
        builder.addStatement("builder.append($S).append('$L')", getSimpleName(), isLeaf ? "(" : ".");
    }

    public TypeName getPropElementName(DtoProp<ImmutableType, ImmutableProp> prop) {
        DtoProp<ImmutableType, ImmutableProp> tailProp = prop.toTailProp();
        DtoType<ImmutableType, ImmutableProp> targetType = tailProp.getTargetType();
        if (targetType != null) {
            if (targetType.getName() == null) {
                List<String> list = new ArrayList<>();
                collectNames(list);
                if (tailProp.isNewTarget()) {
                    list.add(targetSimpleName(tailProp));
                }
                return ClassName.get(
                        getPackageName(),
                        list.get(0),
                        list.subList(1, list.size()).toArray(EMPTY_STR_ARR)
                );
            }
            return ClassName.get(
                    getPackageName(),
                    targetType.getName()
            );
        }
        TypeName typeName = tailProp.isIdOnly() ?
                tailProp.getBaseProp().getTargetType().getIdProp().getTypeName() :
                tailProp.getBaseProp().getElementTypeName();
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

    private static String targetSimpleName(DtoProp<ImmutableType, ImmutableProp> prop) {
        DtoType<ImmutableType, ImmutableProp> targetType = prop.getTargetType();
        if (targetType == null) {
            throw new IllegalArgumentException("prop is not association");
        }
        if (targetType.getName() != null) {
            return targetType.getName();
        }
        return "TargetOf_" + prop.getName();
    }

    private static boolean isCopyableAnnotation(AnnotationMirror annotationMirror, boolean forMethod) {
        Target target = annotationMirror.getAnnotationType().asElement().getAnnotation(Target.class);
        if (target != null) {
            boolean acceptField = Arrays.stream(target.value()).anyMatch(it -> it == ElementType.FIELD);
            if (acceptField) {
                String qualifiedName = ((TypeElement) annotationMirror.getAnnotationType().asElement()).getQualifiedName().toString();
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
}
