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
import java.util.stream.Collectors;

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
                                dtoType.getModifiers().contains(DtoTypeModifier.INPUT_ONLY) ?
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

        boolean inputOnly = dtoType.getModifiers().contains(DtoTypeModifier.INPUT_ONLY);
        if (!inputOnly) {
            addMetadata();
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
        if (dtoType.getDtoProps().stream().anyMatch(it -> it.isNullable() || it.getNextProp() != null)) {
            builder.addStatement("$T spi = ($T)base", ImmutableSpi.class, ImmutableSpi.class);
        }
        for (DtoProp<ImmutableType, ImmutableProp> prop : dtoType.getDtoProps()) {
            if (prop.getNextProp() != null) {
                builder.addCode(
                        "this.$L = $T.get(\n$>spi,\n",
                        prop.getName(),
                        Constants.FLAT_UTILS_CLASS_NAME
                );
                builder.addCode("new int[] {$>\n");
                for (DtoProp<ImmutableType, ImmutableProp> p = prop; p != null; p = p.getNextProp()) {
                    builder.addCode(
                            "$T.$N",
                            p.getBaseProp().getDeclaringType().getProducerClassName(),
                            p.getBaseProp().getSlotName()
                    );
                    if (p.getNextProp() != null) {
                        builder.addCode(",\n");
                    } else {
                        builder.addCode("\n");
                    }
                }
                builder.addCode("$<},\n");
                if (prop.getTargetType() != null) {
                    builder.addCode(
                            "it -> new $T(($T)it)\n",
                            getPropTypeName(prop),
                            prop.toTailProp().getBaseProp().getTypeName()
                    );
                } else if (prop.getEnumType() != null) {
                    builder.addCode("it -> ");
                    builder.beginControlFlow("");
                    appendEnumToValue(builder, prop, "it", false);
                    builder.addStatement("return __$L", prop.getName());
                    builder.endControlFlow();
                } else {
                    builder.addCode("null\n");
                }
                builder.addCode("$<);\n");
            } else if (prop.isIdOnly()) {
                if (prop.getBaseProp().isList()) {
                    if (prop.isNullable()) {
                        builder.addStatement(
                                "this.$L = spi.__isLoaded($T.$L.unwrap().getId())$L ? \n" +
                                        "    base.$L().stream().map($T::$L).collect($T.toList()) : \n" +
                                        "    $L",
                                prop.getName(),
                                dtoType.getBaseType().getPropsClassName(),
                                Strings.upper(prop.getBaseProp().getName()),
                                prop.isRecursive() ? "" : " && !base." + prop.getBaseProp().getGetterName() + "().isEmpty()",
                                prop.getBaseProp().getGetterName(),
                                prop.getBaseProp().getTargetType().getClassName(),
                                prop.getBaseProp().getTargetType().getIdProp().getName(),
                                Collectors.class,
                                defaultValue(prop)
                        );
                    } else if (prop.getBaseProp().isNullable()) {
                        builder.addStatement(
                                "this.$L = $T.requireNonNull(base.$L(), $S).stream().map($T::$L).collect($T.toList())",
                                Objects.class,
                                prop.getName(),
                                "\"`base." + prop.getBaseProp().getGetterName() + "()` cannot be null\"",
                                prop.getBaseProp().getGetterName(),
                                prop.getBaseProp().getTargetType().getClassName(),
                                prop.getBaseProp().getTargetType().getIdProp().getName(),
                                Collectors.class
                        );
                    } else {
                        builder.addStatement(
                                "this.$L = base.$L().stream().map($T::$L).collect($T.toList())",
                                prop.getName(),
                                prop.getBaseProp().getGetterName(),
                                prop.getBaseProp().getTargetType().getClassName(),
                                prop.getBaseProp().getTargetType().getIdProp().getName(),
                                Collectors.class
                        );
                    }
                } else {
                    if (prop.isNullable()) {
                        builder.addStatement(
                                "$T _tmp_$L = spi.__isLoaded($T.$L.unwrap().getId()) ? base.$L() : null",
                                prop.getBaseProp().getTypeName(),
                                prop.getBaseProp().getName(),
                                dtoType.getBaseType().getPropsClassName(),
                                Strings.upper(prop.getBaseProp().getName()),
                                prop.getBaseProp().getGetterName()
                        );
                    } else if (prop.getBaseProp().isNullable()) {
                        builder.addStatement(
                                "$T _tmp_$L = $T.requireNonNull(base.$L(), $S)",
                                prop.getBaseProp().getTypeName(),
                                prop.getBaseProp().getName(),
                                Objects.class,
                                prop.getBaseProp().getGetterName(),
                                "\"`base." + prop.getBaseProp().getGetterName() + "()` cannot be null\""
                        );
                    } else {
                        builder.addStatement(
                                "$T _tmp_$L = base.$L()",
                                prop.getBaseProp().getTypeName(),
                                prop.getBaseProp().getName(),
                                prop.getBaseProp().getGetterName()
                        );
                    }
                    if (prop.isNullable()) {
                        builder.addStatement(
                                "this.$L = _tmp_$L != null ? _tmp_$L.$L() : null",
                                prop.getName(),
                                prop.getBaseProp().getName(),
                                prop.getBaseProp().getName(),
                                prop.getBaseProp().getTargetType().getIdProp().getGetterName()
                        );
                    } else {
                        builder.addStatement(
                                "this.$L = _tmp_$L.$L()",
                                prop.getName(),
                                prop.getBaseProp().getName(),
                                prop.getBaseProp().getTargetType().getIdProp().getGetterName()
                        );
                    }
                }
            } else if (prop.getTargetType() != null) {
                if (prop.getBaseProp().isList()) {
                    if (prop.isNullable()) {
                        builder.addStatement(
                                "this.$L = spi.__isLoaded($T.$L.unwrap().getId())$L ? \n" +
                                        "    base.$L().stream().map($T::new).collect($T.toList()) : \n" +
                                        "    $L",
                                prop.getName(),
                                dtoType.getBaseType().getPropsClassName(),
                                Strings.upper(prop.getBaseProp().getName()),
                                prop.isRecursive() ? "" : " && !base." + prop.getBaseProp().getGetterName() + "().isEmpty()",
                                prop.getBaseProp().getGetterName(),
                                getPropElementName(prop),
                                Collectors.class,
                                defaultValue(prop)
                        );
                    } else if (prop.getBaseProp().isNullable()) {
                        builder.addStatement(
                                "this.$L = $T.requireNonNull(base.$L(), $S).stream().map($T::new).collect($T.toList())",
                                prop.getName(),
                                Objects.class,
                                prop.getBaseProp().getGetterName(),
                                "\"`base." + prop.getBaseProp().getGetterName() + "()` cannot be null\"",
                                getPropElementName(prop),
                                Collectors.class
                        );
                    } else {
                        builder.addStatement(
                                "this.$L = base.$L().stream().map($T::new).collect($T.toList())",
                                prop.getName(),
                                prop.getBaseProp().getGetterName(),
                                getPropElementName(prop),
                                Collectors.class
                        );
                    }
                } else {
                    if (prop.isNullable()) {
                        builder.addStatement(
                                "$T _tmp_$L = spi.__isLoaded($T.$L.unwrap().getId()) ? base.$L() : null",
                                prop.getBaseProp().getTypeName(),
                                prop.getBaseProp().getName(),
                                dtoType.getBaseType().getPropsClassName(),
                                Strings.upper(prop.getBaseProp().getName()),
                                prop.getBaseProp().getGetterName()
                        );
                    } else if (prop.getBaseProp().isNullable()) {
                        builder.addStatement(
                                "$T _tmp_$L = $T.requireNonNull(base.$L(), $L)",
                                prop.getBaseProp().getTypeName(),
                                prop.getBaseProp().getName(),
                                Objects.class,
                                prop.getBaseProp().getGetterName(),
                                "\"`base." + prop.getBaseProp().getGetterName() + "()` cannot be null\""
                        );
                    } else {
                        builder.addStatement(
                                "$T _tmp_$L = base.$L()",
                                prop.getBaseProp().getTypeName(),
                                prop.getBaseProp().getName(),
                                prop.getBaseProp().getGetterName()
                        );
                    }
                    if (prop.isNullable()) {
                        builder.addStatement(
                                "this.$L = _tmp_$L != null ? new $T(_tmp_$L) : null",
                                prop.getName(),
                                prop.getBaseProp().getName(),
                                getPropElementName(prop),
                                prop.getBaseProp().getName()
                        );
                    } else {
                        builder.addStatement(
                                "this.$L = new $T(_tmp_$L)",
                                prop.getName(),
                                getPropElementName(prop),
                                prop.getBaseProp().getName()
                        );
                    }
                }
            } else if (prop.getEnumType() != null) {
                if (prop.isNullable()) {
                    builder.beginControlFlow(
                            "if (!spi.__isLoaded($T.$L.unwrap().getId()))",
                            dtoType.getBaseType().getPropsClassName(),
                            Strings.upper(prop.getBaseProp().getName())
                    );
                    builder.addStatement("this.$L = null", prop.getName());
                    builder.nextControlFlow("else");
                }
                appendEnumToValue(
                        builder,
                        prop,
                        "base." + prop.getBaseProp().getGetterName() + "()",
                        true
                );
                builder.addStatement(
                        "this.$L = __$L",
                        prop.getName(),
                        prop.getName()
                );
                if (prop.isNullable()) {
                    builder.endControlFlow();
                }
            } else {
                if (prop.isNullable()) {
                    builder.addStatement(
                            "this.$L = spi.__isLoaded($T.$L.unwrap().getId()) ? base.$L() : $L",
                            prop.getName(),
                            dtoType.getBaseType().getPropsClassName(),
                            Strings.upper(prop.getBaseProp().getName()),
                            prop.getBaseProp().getGetterName(),
                            defaultValue(prop)
                    );
                } else {
                    builder.addStatement(
                            "this.$L = base.$L()",
                            prop.getName(),
                            prop.getBaseProp().getGetterName()
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
                "return $T.$L.produce(draft -> {$>\n",
                dtoType.getBaseType().getDraftClassName(),
                "$"
        );
        for (DtoProp<ImmutableType, ImmutableProp> prop : dtoType.getDtoProps()) {
            if (prop.getEnumType() != null) {
                appendValueToEnum(
                        builder,
                        prop,
                        "this." + prop.getName()
                );
            }
            if (prop.getNextProp() != null) {
                builder.addCode("$T.set(\n$>", Constants.FLAT_UTILS_CLASS_NAME);
                builder.addCode("draft,\n");
                builder.addCode("new int[] {\n$>");
                for (DtoProp<ImmutableType, ImmutableProp> p = prop; p != null; p = p.getNextProp()) {
                    builder.addCode(
                            "$T.$N",
                            p.getBaseProp().getDeclaringType().getProducerClassName(),
                            p.getBaseProp().getSlotName()
                    );
                    if (p.getNextProp() != null) {
                        builder.addCode(",\n");
                    } else {
                        builder.addCode("\n");
                    }
                }
                builder.addCode("$<},\n");
                if (prop.getTargetType() != null) {
                    builder.addCode("this.$L != null ? this.$L.toEntity() : null\n", prop.getName(), prop.getName());
                } else if (prop.getEnumType() != null) {
                    builder.addCode("__$L\n", prop.getName());
                } else {
                    builder.addCode("this.$L\n", prop.getName());
                }
                builder.addCode("$<);\n");
            } else if (prop.isNullable() && (prop.getBaseProp().isAssociation(false) || !prop.getBaseProp().isNullable())) {
                builder.beginControlFlow("if ($L != null)", prop.getName());
                addAssignment(prop, builder);
                if (prop.getBaseProp().isAssociation(true)) {
                    if (prop.getBaseProp().isList()) {
                        builder.nextControlFlow("else");
                        builder.addStatement(
                                "draft.$L($T.emptyList())",
                                prop.getBaseProp().getSetterName(),
                                Constants.COLLECTIONS_CLASS_NAME
                        );
                    } else if (prop.getBaseProp().isNullable()) {
                        builder.nextControlFlow("else");
                        builder.addStatement(
                                "draft.$L(($T)null)",
                                prop.getBaseProp().getSetterName(),
                                prop.getBaseProp().getTargetType().getClassName()
                        );
                    }
                }
                builder.endControlFlow();
            } else if (prop.getEnumType() != null) {
                builder.addStatement("draft.$L(__$L)", prop.getBaseProp().getSetterName(), prop.getName());
            } else {
                addAssignment(prop, builder);
            }
        }
        builder.addCode("$<});\n");
        typeBuilder.addMethod(builder.build());
    }

    private void addAssignment(DtoProp<ImmutableType, ImmutableProp> prop, MethodSpec.Builder builder) {
        ImmutableProp immutableProp = prop.getBaseProp();
        if (prop.isIdOnly()) {
            if (immutableProp.isList()) {
                builder.beginControlFlow("if ($L.isEmpty())", prop.getName());
                builder.addStatement(
                        "draft.$L($T.emptyList())",
                        immutableProp.getSetterName(),
                        Collections.class
                );
                builder.nextControlFlow("else");
                builder.beginControlFlow(
                        "for ($T __e : $L)",
                        getPropElementName(prop),
                        prop.getName()
                );
                builder.addStatement(
                        "draft.$L(targetDraft -> targetDraft.$L($L))",
                        immutableProp.getAdderByName(),
                        immutableProp.getTargetType().getIdProp().getSetterName(),
                        "__e"
                );
                builder.endControlFlow();
                builder.endControlFlow();
            } else {
                builder.addStatement(
                        "draft.$L(targetDraft -> targetDraft.$L($L))",
                        immutableProp.getApplierName(),
                        immutableProp.getTargetType().getIdProp().getSetterName(),
                        prop.getName()
                );
            }
        } else if (prop.getTargetType() != null) {
            if (immutableProp.isList()) {
                builder.beginControlFlow("if ($L.isEmpty())", prop.getName());
                builder.addStatement(
                        "draft.$L($T.emptyList())",
                        immutableProp.getSetterName(),
                        Collections.class
                );
                builder.nextControlFlow("else");
                builder.beginControlFlow(
                        "for ($T __e : $L)",
                        getPropElementName(prop),
                        prop.getName()
                );
                builder.addStatement(
                        "draft.$L(true).add(($T)__e.toEntity())",
                        immutableProp.getGetterName(),
                        immutableProp.getTargetType().getDraftClassName()
                );
                builder.endControlFlow();
                builder.endControlFlow();
            } else {
                builder.addStatement(
                        "draft.$L($L.toEntity())",
                        immutableProp.getSetterName(),
                        prop.getName()
                );
            }
        } else if (prop.getEnumType() != null) {
            builder.addStatement("draft.$L(__$L)", prop.getBaseProp().getSetterName(), prop.getName());
        } else {
            builder.addStatement("draft.$L($L)", prop.getBaseProp().getSetterName(), prop.getName());
        }
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
                tailProp.getBaseProp().getTypeName();
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

    private static String defaultValue(DtoProp<?, ImmutableProp> prop) {
        TypeName typeName = prop.getBaseProp().getTypeName();
        return defaultValue(typeName);
    }

    private static String defaultValue(TypeName typeName) {
        if (typeName.isPrimitive()) {
            if (typeName.equals(TypeName.BOOLEAN)) {
                return "false";
            }
            if (typeName.equals(TypeName.CHAR)) {
                return "'\0'";
            }
            return "0";
        }
        return "null";
    }

    private String appendEnumToValue(
            MethodSpec.Builder builder,
            DtoProp<ImmutableType, ImmutableProp> prop,
            String parameterName,
            boolean parameterIsEnum
    ) {
        EnumType enumType = prop.getEnumType();
        if (enumType == null) {
            return null;
        }
        builder.addStatement("$T __$L", getPropTypeName(prop), prop.getName());
        if (prop.isNullable()) {
            builder.beginControlFlow("if ($L != null)", parameterName);
        }
        if (parameterIsEnum) {
            builder.beginControlFlow("switch ($L)", parameterName);
        } else {
            builder.beginControlFlow("switch (($T)$L)", prop.toTailProp().getBaseProp().getTypeName(),  parameterName);
        }
        for (Map.Entry<String, String> e : enumType.getValueMap().entrySet()) {
            builder.addStatement("case $L: __$L = $L; break", e.getKey(), prop.getName(), e.getValue());
        }
        builder.addStatement("default: throw new AssertionError($S)", "Internal bug");
        builder.endControlFlow();
        if (prop.isNullable()) {
            builder.nextControlFlow("else");
            builder.addStatement("__$L = null", prop.getName());
            builder.endControlFlow();
        }
        return null;
    }

    private String appendValueToEnum(
            MethodSpec.Builder builder,
            DtoProp<ImmutableType, ImmutableProp> prop,
            String parameterName
    ) {
        EnumType enumType = prop.getEnumType();
        if (enumType == null) {
            return null;
        }
        builder.addStatement("$T __$L", prop.toTailProp().getBaseProp().getTypeName(), prop.getName());
        if (prop.isNullable()) {
            builder.beginControlFlow("if ($L != null)", parameterName);
        }
        TypeName enumTypeName = prop.toTailProp().getBaseProp().getTypeName();
        builder.beginControlFlow("switch ($L)", parameterName);
        for (Map.Entry<String, String> e : enumType.getConstantMap().entrySet()) {
            builder.addStatement("case $L: __$L = $T.$L; break", e.getKey(), prop.getName(), enumTypeName, e.getValue());
        }
        builder.addStatement(
                "default: throw new IllegalArgumentException($>" +
                        "\"Illegal value '\" + " +
                        "$L + " +
                        "\"' for enum type $L\"" +
                        "$<)",
                parameterName,
                enumTypeName.toString()
        );
        builder.endControlFlow();
        if (prop.isNullable()) {
            builder.nextControlFlow("else");
            builder.addStatement("__$L = null", prop.getName());
            builder.endControlFlow();
        }
        return null;
    }
}
