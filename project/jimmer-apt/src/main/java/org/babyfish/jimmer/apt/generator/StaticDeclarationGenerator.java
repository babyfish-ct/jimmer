package org.babyfish.jimmer.apt.generator;

import com.squareup.javapoet.*;
import org.babyfish.jimmer.apt.GeneratorException;
import org.babyfish.jimmer.apt.meta.ImmutableProp;
import org.babyfish.jimmer.apt.meta.StaticDeclaration;
import org.babyfish.jimmer.apt.meta.StaticProp;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.sql.Id;
import org.babyfish.jimmer.sql.Key;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.processing.Filer;
import javax.lang.model.element.Modifier;
import javax.validation.constraints.Null;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class StaticDeclarationGenerator {

    private static final String[] EMPTY_STR_ARR = new String[0];

    private final StaticDeclaration declaration;

    private final Filer filer;

    private final List<StaticProp> props;

    private final String innerClassName;

    private final StaticDeclarationGenerator parent;

    private TypeSpec.Builder typeBuilder;

    public StaticDeclarationGenerator(StaticDeclaration declaration, Filer filer) {
        this(declaration, filer, null, null);
    }

    public StaticDeclarationGenerator(
            StaticDeclaration declaration,
            String innerClassName,
            StaticDeclarationGenerator parent
    ) {
        this(declaration, null, innerClassName, parent);
    }

    private StaticDeclarationGenerator(
            StaticDeclaration declaration,
            Filer filer,
            String innerClassName,
            StaticDeclarationGenerator parent
    ) {
        this.declaration = declaration;
        this.filer = filer;
        this.innerClassName = innerClassName;
        this.parent = parent;

        List<StaticProp> props = new ArrayList<>();
        String alias = declaration.getAlias();
        boolean hasKey = declaration
                .getImmutableType()
                .getProps()
                .values()
                .stream()
                .anyMatch(it -> it.getAnnotation(Key.class) != null);
        for (ImmutableProp prop : declaration.getImmutableType().getProps().values()) {
            if (prop.isTransient()) {
                continue;
            }
            StaticProp staticProp = prop.getStaticProp(alias);
            if (staticProp == null) {
                if (declaration.isAllScalars() && !prop.isAssociation(true)) {
                    staticProp = new StaticProp(prop, alias, prop.getName(), true, declaration.isAllOptional(), false, "");
                    if (!staticProp.isOptional() && prop.getAnnotation(Id.class) != null && hasKey) {
                        staticProp = staticProp.optional(true);
                    }
                    props.add(staticProp);
                }
            } else if (staticProp.isEnabled()) {
                props.add(staticProp.optional(declaration.isAllOptional()));
            }
        }
        this.props = Collections.unmodifiableList(props);
    }

    public List<StaticProp> getProps() {
        return props;
    }

    public TypeSpec.Builder getTypeBuilder() {
        return typeBuilder;
    }

    public String getSimpleName() {
        return innerClassName != null ? innerClassName : declaration.getTopLevelName();
    }

    public ClassName getClassName(String ... nestedNames) {
        if (innerClassName != null) {
            List<String> list = new ArrayList<>();
            collectNames(list);
            list.addAll(Arrays.asList(nestedNames));
            return ClassName.get(
                    declaration.getImmutableType().getPackageName(),
                    list.get(0),
                    list.subList(1, list.size()).toArray(EMPTY_STR_ARR)
            );
        }
        return ClassName.get(
                declaration.getImmutableType().getPackageName(),
                declaration.getTopLevelName(),
                nestedNames
        );
    }

    public void generate() {
        String simpleName = getSimpleName();
        typeBuilder = TypeSpec
                .classBuilder(simpleName)
                .addModifiers(Modifier.PUBLIC);
        if (simpleName.endsWith("Input")) {
            typeBuilder.addSuperinterface(
                    ParameterizedTypeName.get(
                            Constants.INPUT_CLASS_NAME,
                            declaration.getImmutableType().getClassName()
                    )
            );
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
                                declaration.getImmutableType().getPackageName(),
                                typeBuilder.build()
                        )
                        .indent("    ")
                        .build()
                        .writeTo(filer);
            } catch (IOException ex) {
                throw new GeneratorException(
                        String.format(
                                "Cannot generate static type for '%s'",
                                declaration.getTopLevelName()
                        ),
                        ex
                );
            }
        }
    }

    private void addMembers() {
        for (StaticProp prop : props) {
            addField(prop);
        }
        addJsonConstructor();
        addConverterConstructor();
        addNewBuilder(false);
        addNewBuilder(true);
        for (StaticProp prop : props) {
            addGetter(prop);
        }
        for (StaticProp prop : props) {
            if (prop.getTarget() != null && prop.getTarget().getTopLevelName().isEmpty()) {
                new StaticDeclarationGenerator(
                        prop.getTarget(),
                        targetSimpleName(prop),
                        this
                ).generate();
            }
        }
        addToEntity();
        addToEntityWithBase();
        addToString();
        new StaticDeclarationBuilderGenerator(this).generate();
    }

    private void addField(StaticProp prop) {
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
        for (StaticProp prop : props) {
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
        for (StaticProp prop : props) {
            if (prop.isNullable() || prop.getImmutableProp().getTypeName().isPrimitive()) {
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
                .addParameter(
                        ParameterSpec
                                .builder(declaration.getImmutableType().getClassName(), "base")
                                .addAnnotation(NotNull.class)
                                .build()
                );
        builder.addStatement("$T spi = ($T)base", ImmutableSpi.class, ImmutableSpi.class);
        for (StaticProp prop : props) {
            if (prop.isIdOnly()) {
                if (prop.getImmutableProp().isList()) {
                    if (prop.isNullable()) {
                        builder.addStatement(
                                "this.$L = spi.__isLoaded($S) ? base.$L().stream().map($T::$L).collect($T.toList()) : $L",
                                prop.getName(),
                                prop.getImmutableProp().getName(),
                                prop.getImmutableProp().getGetterName(),
                                prop.getImmutableProp().getTargetType().getClassName(),
                                prop.getImmutableProp().getTargetType().getIdProp().getName(),
                                Collectors.class,
                                prop.getDefaultValue()
                        );
                    } else {
                        builder.addStatement(
                                "this.$L = base.$L().stream().map($T::$L).collect($T.toList())",
                                prop.getName(),
                                prop.getImmutableProp().getGetterName(),
                                prop.getImmutableProp().getTargetType().getClassName(),
                                prop.getImmutableProp().getTargetType().getIdProp().getName(),
                                Collectors.class
                        );
                    }
                } else {
                    if (prop.isNullable()) {
                        builder.addStatement(
                                "$T _tmpFor$L = spi.__isLoaded($S) ? base.$L() : null",
                                prop.getImmutableProp().getTypeName(),
                                prop.getName(),
                                prop.getImmutableProp().getName(),
                                prop.getImmutableProp().getGetterName()
                        );
                    } else {
                        builder.addStatement(
                                "$T _tmpFor$L = base.$L()",
                                prop.getImmutableProp().getTypeName(),
                                prop.getName(),
                                prop.getImmutableProp().getGetterName()
                        );
                    }
                    builder.addStatement(
                            "this.$L = _tmpFor$L != null ? _tmpFor$L.$L() : null",
                            prop.getName(),
                            prop.getName(),
                            prop.getName(),
                            prop.getImmutableProp().getTargetType().getIdProp().getGetterName()
                    );
                }
            } else if (prop.getTarget() != null) {
                if (prop.getImmutableProp().isList()) {
                    if (prop.isNullable()) {
                        builder.addStatement(
                                "this.$L = spi.__isLoaded($S) ? base.$L().stream().map($T::new).collect($T.toList()) : $L",
                                prop.getName(),
                                prop.getImmutableProp().getName(),
                                prop.getImmutableProp().getGetterName(),
                                getPropElementName(prop),
                                Collectors.class,
                                prop.getDefaultValue()
                        );
                    } else {
                        builder.addStatement(
                                "this.$L = base.$L().stream().map($T::new).collect($T.toList())",
                                prop.getName(),
                                prop.getImmutableProp().getGetterName(),
                                getPropElementName(prop),
                                Collectors.class
                        );
                    }
                } else {
                    if (prop.isNullable()) {
                        builder.addStatement(
                                "$T _tmpFor$L = spi.__isLoaded($S) ? base.$L() : null",
                                prop.getImmutableProp().getTypeName(),
                                prop.getName(),
                                prop.getImmutableProp().getName(),
                                prop.getImmutableProp().getGetterName()
                        );
                    } else {
                        builder.addStatement(
                                "$T _tmpFor$L = base.$L()",
                                prop.getImmutableProp().getTypeName(),
                                prop.getName(),
                                prop.getImmutableProp().getGetterName()
                        );
                    }
                    builder.addStatement(
                            "this.$L = _tmpFor$L != null ? new $T(_tmpFor$L) : null",
                            prop.getName(),
                            prop.getName(),
                            getPropElementName(prop),
                            prop.getName()
                    );
                }
            } else {
                if (prop.isNullable()) {
                    builder.addStatement(
                            "this.$L = spi.__isLoaded($S) ? base.$L() : $L",
                            prop.getName(),
                            prop.getImmutableProp().getName(),
                            prop.getImmutableProp().getGetterName(),
                            prop.getDefaultValue()
                    );
                } else {
                    builder.addStatement(
                            "this.$L = base.$L()",
                            prop.getName(),
                            prop.getImmutableProp().getGetterName()
                    );
                }
            }
        }
        typeBuilder.addMethod(builder.build());
    }

    private void addGetter(StaticProp prop) {
        MethodSpec.Builder builder = MethodSpec
                .methodBuilder(prop.getGetterName())
                .returns(getPropTypeName(prop));
        if (prop.isNullable()) {
            builder.addAnnotation(Nullable.class).addAnnotation(Null.class);
        } else {
            builder.addAnnotation(NotNull.class).addAnnotation(javax.validation.constraints.NotNull.class);
        }
        builder.addStatement("return $L", prop.getName());
        typeBuilder.addMethod(builder.build());
    }

    private void addToEntity() {
        MethodSpec.Builder builder = MethodSpec
                .methodBuilder("toEntity")
                .addModifiers(Modifier.PUBLIC)
                .returns(declaration.getImmutableType().getClassName())
                .addStatement("return toEntity(null)");
        if (getSimpleName().endsWith("Input")) {
            builder.addAnnotation(Override.class);
        }
        typeBuilder.addMethod(builder.build());
    }

    private void addToEntityWithBase() {
        MethodSpec.Builder builder = MethodSpec
                .methodBuilder("toEntity")
                .addModifiers(Modifier.PUBLIC)
                .addParameter(
                        ParameterSpec
                                .builder(
                                        declaration.getImmutableType().getClassName(),
                                        "base"
                                )
                                .addAnnotation(Nullable.class)
                                .build()
                )
                .returns(declaration.getImmutableType().getClassName());
        builder.addCode(
                "return $T.$L.produce(base, draft -> {$>\n",
                declaration.getImmutableType().getDraftClassName(),
                "$"
        );
        for (StaticProp prop : props) {
            if (prop.isNullable() && (prop.getImmutableProp().isAssociation(false) || !prop.getImmutableProp().isNullable())) {
                builder.beginControlFlow("if ($L != null)", prop.getName());
                addAssignment(prop, builder);
                builder.endControlFlow();
            } else {
                addAssignment(prop, builder);
            }
        }
        builder.addCode("$<});\n");
        typeBuilder.addMethod(builder.build());
    }

    private void addAssignment(StaticProp prop, MethodSpec.Builder builder) {
        ImmutableProp immutableProp = prop.getImmutableProp();
        if (prop.isIdOnly()) {
            if (immutableProp.isList()) {
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
            } else {
                builder.addStatement(
                        "draft.$L(targetDraft -> targetDraft.$L($L))",
                        immutableProp.getSetterName(),
                        immutableProp.getTargetType().getIdProp().getSetterName(),
                        prop.getName()
                );
            }
        } else if (prop.getTarget() != null) {
            if (immutableProp.isList()) {
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
            } else {
                builder.addStatement(
                        "draft.$L($L.toEntity())",
                        immutableProp.getSetterName(),
                        prop.getName()
                );
            }
        } else {
            builder.addStatement("draft.$L($L)", prop.getImmutableProp().getSetterName(), prop.getName());
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
        for (StaticProp prop : props) {
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

    public TypeName getPropTypeName(StaticProp prop) {
        return prop.getImmutableProp().isList() ?
                ParameterizedTypeName.get(Constants.LIST_CLASS_NAME, getPropElementName(prop)) :
                getPropElementName(prop);
    }

    public TypeName getPropElementName(StaticProp prop) {
        StaticDeclaration target = prop.getTarget();
        if (target != null) {
            if (target.getTopLevelName().isEmpty()) {
                List<String> list = new ArrayList<>();
                collectNames(list);
                list.add(targetSimpleName(prop));
                return ClassName.get(
                        declaration.getImmutableType().getPackageName(),
                        list.get(0),
                        list.subList(1, list.size()).toArray(EMPTY_STR_ARR)
                );
            }
            return ClassName.get(
                    target.getImmutableType().getPackageName(),
                    target.getTopLevelName()
            );
        }
        TypeName typeName = prop.isIdOnly() ?
                prop.getImmutableProp().getTargetType().getIdProp().getTypeName() :
                prop.getImmutableProp().getTypeName();
        if (typeName.isPrimitive() && prop.isNullable()) {
            return typeName.box();
        }
        return typeName;
    }

    private void collectNames(List<String> list) {
        if (parent == null) {
            list.add(declaration.getTopLevelName());
        } else {
            parent.collectNames(list);
            list.add(innerClassName);
        }
    }

    private static String targetSimpleName(StaticProp prop) {
        StaticDeclaration target = prop.getTarget();
        if (target == null) {
            return null;
        }
        if (!target.getTopLevelName().isEmpty()) {
            return target.getTopLevelName();
        }
        return "TargetOf_" + prop.getName();
    }
}
