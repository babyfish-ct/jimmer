package org.babyfish.jimmer.apt.generator;

import com.squareup.javapoet.*;
import org.babyfish.jimmer.apt.GeneratorException;
import org.babyfish.jimmer.apt.meta.ImmutableProp;
import org.babyfish.jimmer.apt.meta.ImmutableType;
import org.babyfish.jimmer.meta.impl.dto.ast.DtoProp;
import org.babyfish.jimmer.meta.impl.dto.ast.DtoType;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.sql.Id;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.processing.Filer;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.validation.constraints.Null;
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
        this.innerClassName = innerClassName;
    }

    public DtoType<ImmutableType, ImmutableProp> getDtoType() {
        return dtoType;
    }

    public TypeSpec.Builder getTypeBuilder() {
        return typeBuilder;
    }

    public void generate() {
        String simpleName = getSimpleName();
        typeBuilder = TypeSpec
                .classBuilder(simpleName)
                .addModifiers(Modifier.PUBLIC)
                .addSuperinterface(
                        ParameterizedTypeName.get(
                                dtoType.isInput() ? Constants.INPUT_CLASS_NAME : Constants.STATIC_CLASS_NAME,
                                dtoType.getBaseType().getClassName()
                        )
                );
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
        if (dtoType.getBaseType().getPackageName().isEmpty()) {
            return "dto";
        }
        return dtoType.getBaseType().getPackageName() + ".dto";
    }

    public String getSimpleName() {
        return innerClassName != null ? innerClassName : dtoType.getName();
    }

    public ClassName getClassName(String ... nestedNames) {
        if (innerClassName != null) {
            List<String> list = new ArrayList<>();
            collectNames(list);
            list.addAll(Arrays.asList(nestedNames));
            return ClassName.get(
                    getPackageName(),
                    list.get(0),
                    list.subList(1, list.size()).toArray(EMPTY_STR_ARR)
            );
        }
        return ClassName.get(
                getPackageName(),
                dtoType.getName(),
                nestedNames
        );
    }

    private void addMembers() {

        addMetadata();

        for (DtoProp<ImmutableType, ImmutableProp> prop : dtoType.getProps()) {
            addField(prop);
        }
        addJsonConstructor();
        addConverterConstructor();
        addNewBuilder(false);
        addNewBuilder(true);
        for (DtoProp<ImmutableType, ImmutableProp> prop : dtoType.getProps()) {
            addGetter(prop);
        }
        addToEntity();
        addToEntityWithBase();
        addToString();

        new DtoBuilderGenerator(this).generate();

        for (DtoProp<ImmutableType, ImmutableProp> prop : dtoType.getProps()) {
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
                                Constants.STATIC_METADATA_CLASS_NAME,
                                dtoType.getBaseType().getClassName(),
                                getClassName()
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
                        Constants.STATIC_METADATA_CLASS_NAME,
                        dtoType.getBaseType().getClassName(),
                        getClassName()
                )
                .indent()
                .add("$T.$L", dtoType.getBaseType().getFetcherClassName(), "$")
                .indent();
        for (DtoProp<ImmutableType, ImmutableProp> prop : dtoType.getProps()) {
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
        cb
                .add(",\n")
                .unindent()
                .add("$T::new\n", getClassName())
                .unindent()
                .unindent()
                .add(")");
        builder.initializer(cb.build());
        typeBuilder.addField(builder.build());
    }

    private void addField(DtoProp<ImmutableType, ImmutableProp> prop) {
        FieldSpec.Builder builder = FieldSpec
                .builder(
                        getPropTypeName(prop),
                        prop.getName()
                )
                .addModifiers(Modifier.PRIVATE, Modifier.FINAL);
        if (prop.isNullable()) {
            builder.addAnnotation(Nullable.class).addAnnotation(Null.class);
        } else {
            builder.addAnnotation(NotNull.class).addAnnotation(javax.validation.constraints.NotNull.class);
        }
        for (AnnotationMirror annotationMirror : prop.getBaseProp().getAnnotations()) {
            if (isCopyableAnnotation(annotationMirror, false)) {
                builder.addAnnotation(AnnotationSpec.get(annotationMirror));
            }
        }
        typeBuilder.addField(builder.build());
    }

    private void addNewBuilder(boolean withBase) {
        MethodSpec.Builder builder = MethodSpec
                .methodBuilder("newBuilder")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(getClassName("Builder"));
        if (withBase) {
            builder.addParameter(
                    ParameterSpec
                            .builder(getClassName(), "base")
                            .addAnnotation(Nullable.class)
                            .build()
            );
        }
        builder.addStatement(
                "return new $T($L)",
                getClassName("Builder"),
                withBase ? "base" : null
        );
        typeBuilder.addMethod(builder.build());
    }

    private void addJsonConstructor() {
        MethodSpec.Builder builder = MethodSpec
                .constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Constants.JSON_CREATOR_CLASS_NAME);
        for (DtoProp<ImmutableType, ImmutableProp> prop : dtoType.getProps()) {
            ParameterSpec.Builder parameterBuilder = ParameterSpec
                    .builder(
                            getPropTypeName(prop),
                            prop.getName()
                    );
            AnnotationSpec.Builder jsonPropertySpecBuilder =
                    AnnotationSpec
                            .builder(Constants.JSON_PROPERTY_CLASS_NAME)
                            .addMember("value", "$S", prop.getName());
            if (prop.isNullable()) {
                parameterBuilder.addAnnotation(Nullable.class).addAnnotation(Null.class);
            } else {
                parameterBuilder.addAnnotation(NotNull.class).addAnnotation(javax.validation.constraints.NotNull.class);
                jsonPropertySpecBuilder.addMember("required", "true");
            }
            parameterBuilder.addAnnotation(jsonPropertySpecBuilder.build());
            builder.addParameter(parameterBuilder.build());
        }
        for (DtoProp<ImmutableType, ImmutableProp> prop : dtoType.getProps()) {
            if (prop.isNullable() || prop.getBaseProp().getTypeName().isPrimitive()) {
                builder.addStatement("this.$L = $L", prop.getName(), prop.getName());
            } else {
                builder.addStatement(
                        "this.$L = $T.requireNonNull($L, $S)",
                        prop.getName(),
                        Constants.OBJECTS_CLASS_NAME,
                        prop.getName(),
                        prop.getName()
                );
            }
        }
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
        if (dtoType.getProps().stream().anyMatch(DtoProp::isNullable)) {
            builder.addStatement("$T spi = ($T)base", ImmutableSpi.class, ImmutableSpi.class);
        }
        for (DtoProp<ImmutableType, ImmutableProp> prop : dtoType.getProps()) {
            if (prop.isIdOnly()) {
                if (prop.getBaseProp().isList()) {
                    if (prop.isNullable()) {
                        builder.addStatement(
                                "this.$L = spi.__isLoaded($L) ? base.$L().stream().map($T::$L).collect($T.toList()) : $L",
                                prop.getName(),
                                prop.getBaseProp().getId(),
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
                                "$T _tmp_$L = spi.__isLoaded($L) ? base.$L() : null",
                                prop.getBaseProp().getTypeName(),
                                prop.getBaseProp().getName(),
                                prop.getBaseProp().getId(),
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
                                "this.$L = spi.__isLoaded($L) ? base.$L().stream().map($T::new).collect($T.toList()) : $L",
                                prop.getName(),
                                prop.getBaseProp().getId(),
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
                                "$T _tmp_$L = spi.__isLoaded($L) ? base.$L() : null",
                                prop.getBaseProp().getTypeName(),
                                prop.getBaseProp().getName(),
                                prop.getBaseProp().getId(),
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
            } else {
                if (prop.isNullable()) {
                    builder.addStatement(
                            "this.$L = spi.__isLoaded($L) ? base.$L() : $L",
                            prop.getName(),
                            prop.getBaseProp().getId(),
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

    private void addGetter(DtoProp<ImmutableType, ImmutableProp> prop) {
        MethodSpec.Builder builder = MethodSpec
                .methodBuilder(dtoGetterName(prop))
                .addModifiers(Modifier.PUBLIC)
                .returns(getPropTypeName(prop));
        if (prop.isNullable()) {
            builder.addAnnotation(Nullable.class).addAnnotation(Null.class);
        } else {
            builder.addAnnotation(NotNull.class).addAnnotation(javax.validation.constraints.NotNull.class);
        }
        for (AnnotationMirror annotationMirror : prop.getBaseProp().getAnnotations()) {
            if (isCopyableAnnotation(annotationMirror, true)) {
                builder.addAnnotation(AnnotationSpec.get(annotationMirror));
            }
        }
        builder.addStatement("return $L", prop.getName());
        typeBuilder.addMethod(builder.build());
    }

    private void addToEntity() {
        MethodSpec.Builder builder = MethodSpec
                .methodBuilder("toEntity")
                .addModifiers(Modifier.PUBLIC)
                .returns(dtoType.getBaseType().getClassName())
                .addStatement("return toEntity(null)")
                .addAnnotation(Override.class);
        typeBuilder.addMethod(builder.build());
    }

    private void addToEntityWithBase() {
        MethodSpec.Builder builder = MethodSpec
                .methodBuilder("toEntity")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .addParameter(
                        ParameterSpec
                                .builder(
                                        dtoType.getBaseType().getClassName(),
                                        "base"
                                )
                                .addAnnotation(Nullable.class)
                                .build()
                )
                .returns(dtoType.getBaseType().getClassName());
        builder.addCode(
                "return $T.$L.produce(base, draft -> {$>\n",
                dtoType.getBaseType().getDraftClassName(),
                "$"
        );
        for (DtoProp<ImmutableType, ImmutableProp> prop : dtoType.getProps()) {
            if (prop.isNullable() && (prop.getBaseProp().isAssociation(false) || !prop.getBaseProp().isNullable())) {
                builder.beginControlFlow("if ($L != null)", prop.getName());
                addAssignment(prop, builder);
                if (prop.getBaseProp().isAssociation(true) &&
                        !prop.getBaseProp().isList() &&
                        prop.getBaseProp().isNullable()) {
                    builder.nextControlFlow("else");
                    builder.addStatement(
                            "draft.$L(($T)null)",
                            prop.getBaseProp().getSetterName(),
                            prop.getBaseProp().getTargetType().getClassName()
                    );
                }
                builder.endControlFlow();
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
                        immutableProp.getSetterName(),
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
        } else {
            builder.addStatement("draft.$L($L)", prop.getBaseProp().getSetterName(), prop.getName());
        }
    }

    private void addToString() {
        MethodSpec.Builder builder = MethodSpec
                .methodBuilder("toString")
                .returns(String.class)
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class);
        List<String> list = new ArrayList<>();
        collectNames(list);

        builder.addCode("return new StringBuilder()\n$>");
        builder.addCode(".append($S)\n", String.join(".", list) + '{');
        boolean addComma = false;
        for (DtoProp<ImmutableType, ImmutableProp> prop : dtoType.getProps()) {
            if (addComma) {
                builder.addCode(".append($S)\n", ", ");
            } else {
                addComma = true;
            }
            builder.addCode(".append($S).append('=').append($L)\n", prop.getName(), prop.getName());
        }
        builder.addCode(".append('}')\n").addCode(".toString();\n");

        typeBuilder.addMethod(builder.build());
    }

    public TypeName getPropTypeName(DtoProp<ImmutableType, ImmutableProp> prop) {
        TypeName elementTypeName = getPropElementName(prop);
        return prop.getBaseProp().isList() ?
                ParameterizedTypeName.get(
                        Constants.LIST_CLASS_NAME,
                        elementTypeName.isPrimitive() ?
                                elementTypeName.box() :
                                elementTypeName
                ) :
                elementTypeName;
    }

    public TypeName getPropElementName(DtoProp<ImmutableType, ImmutableProp> prop) {
        DtoType<ImmutableType, ImmutableProp> targetType = prop.getTargetType();
        if (targetType != null) {
            if (targetType.getName() == null) {
                List<String> list = new ArrayList<>();
                collectNames(list);
                if (prop.isNewTarget()) {
                    list.add(targetSimpleName(prop));
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
        TypeName typeName = prop.isIdOnly() ?
                prop.getBaseProp().getTargetType().getIdProp().getTypeName() :
                prop.getBaseProp().getTypeName();
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
            boolean acceptMethod = Arrays.stream(target.value()).anyMatch(it -> it == ElementType.METHOD);
            if (forMethod ? acceptMethod && !acceptField : acceptField) {
                String qualifiedName = ((TypeElement) annotationMirror.getAnnotationType().asElement()).getQualifiedName().toString();
                return !qualifiedName.equals(NotNull.class.getName()) &&
                        !qualifiedName.equals(javax.validation.constraints.NotNull.class.getName()) &&
                        !qualifiedName.equals(Nullable.class.getName()) &&
                        !qualifiedName.equals(Null.class.getName()) &&
                        !qualifiedName.startsWith("org.babyfish.jimmer.");
            }
        }
        return false;
    }

    private static String defaultValue(DtoProp<?, ImmutableProp> prop) {
        TypeName typeName = prop.getBaseProp().getTypeName();
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

    static String dtoGetterName(DtoProp<ImmutableType, ImmutableProp> prop) {
        String name = prop.getName();
        name = Character.toUpperCase(name.charAt(0)) + name.substring(1);
        if (prop.getBaseProp().getTypeName().equals(TypeName.BOOLEAN)) {
            return "is" + name;
        }
        return "get" + name;
    }
}
