package org.babyfish.jimmer.apt.generator;

import com.squareup.javapoet.*;
import org.babyfish.jimmer.CircularReferenceException;
import org.babyfish.jimmer.ImmutableObjects;
import org.babyfish.jimmer.apt.meta.ImmutableProp;
import org.babyfish.jimmer.apt.meta.ImmutableType;
import org.babyfish.jimmer.runtime.DraftContext;
import org.babyfish.jimmer.runtime.DraftSpi;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.runtime.NonSharedList;
import org.jetbrains.annotations.Nullable;

import javax.lang.model.element.Modifier;
import javax.validation.constraints.Email;
import javax.validation.constraints.Pattern;

import java.util.ArrayList;
import java.util.Map;

import static org.babyfish.jimmer.apt.generator.Constants.*;

public class DraftImplGenerator {

    private final ImmutableType type;

    private final ClassName draftSpiClassName;

    private TypeSpec.Builder typeBuilder;

    public DraftImplGenerator(ImmutableType type) {
        this.type = type;
        draftSpiClassName = ClassName.get(DraftSpi.class);
    }

    public void generate(TypeSpec.Builder parentBuilder) {
        typeBuilder = TypeSpec.classBuilder("DraftImpl")
                .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
                .superclass(type.getImplementorClassName())
                .addSuperinterface(draftSpiClassName)
                .addSuperinterface(type.getDraftClassName());
        addFields();
        addStaticFields();
        addConstructor();
        addReadonlyMethods();
        for (ImmutableProp prop : type.getProps().values()) {
            addGetter(prop);
            addCreator(prop);
            addSetter(prop);
            addUtilMethod(prop, false);
            addUtilMethod(prop, true);
        }
        addSet(int.class);
        addSet(String.class);
        addShow(int.class);
        addShow(String.class);
        addUnload(int.class);
        addUnload(String.class);
        addDraftContext();
        addResolve();
        addModified();
        parentBuilder.addType(typeBuilder.build());
    }

    private void addFields() {
        typeBuilder.addField(
                FieldSpec
                        .builder(
                                DRAFT_CONTEXT_CLASS_NAME,
                                DRAFT_FIELD_CTX,
                                Modifier.PRIVATE
                        )
                        .build()
        );
        typeBuilder.addField(
                FieldSpec
                        .builder(
                                type.getImplClassName(),
                                DRAFT_FIELD_BASE,
                                Modifier.PRIVATE
                        )
                        .build()
        );
        typeBuilder.addField(
                FieldSpec
                        .builder(
                                type.getImplClassName(),
                                DRAFT_FIELD_MODIFIED,
                                Modifier.PRIVATE
                        )
                        .build()
        );
        typeBuilder.addField(
                FieldSpec
                        .builder(
                                boolean.class,
                                DRAFT_FIELD_RESOLVING,
                                Modifier.PRIVATE
                        )
                        .build()
        );
    }

    private void addStaticFields() {
        boolean hasEmail = false;
        for (ImmutableProp prop : type.getProps().values()) {
            Email[] emails = prop.getAnnotations(Email.class);
            Pattern[] patterns = prop.getAnnotations(Pattern.class);
            if (emails.length != 0) {
                hasEmail = true;
            }
            for (int i = 0; i < patterns.length; i++) {
                int flags = 0;
                for (Pattern.Flag flag : patterns[i].flags()) {
                    flags |= flag.getValue();
                }
                FieldSpec.Builder builder = FieldSpec
                        .builder(
                                java.util.regex.Pattern.class,
                                regexpPatternFieldName(prop, i),
                                Modifier.PRIVATE,
                                Modifier.STATIC,
                                Modifier.FINAL
                        )
                        .initializer(
                                "$T.compile($S, $L)",
                                java.util.regex.Pattern.class,
                                patterns[i].regexp(),
                                flags
                        );
                typeBuilder.addField(builder.build());
            }
        }
        if (hasEmail) {
            FieldSpec.Builder builder = FieldSpec
                    .builder(
                            java.util.regex.Pattern.class,
                            DRAFT_FIELD_EMAIL_PATTERN,
                            Modifier.PRIVATE,
                            Modifier.STATIC,
                            Modifier.FINAL
                    )
                    .initializer(
                            "$T.compile($S)",
                            java.util.regex.Pattern.class,
                            "^[^@]+@[^@]+$"
                    );
            typeBuilder.addField(builder.build());
        }
        for (Map.Entry<ClassName, String> e : type.getValidationMessageMap().entrySet()) {
            FieldSpec.Builder builder = FieldSpec
                    .builder(
                            ParameterizedTypeName.get(
                                    VALIDATOR_CLASS_NAME,
                                    type.getClassName()
                            ),
                            Constants.validatorFieldName(e.getKey()),
                            Modifier.PRIVATE,
                            Modifier.STATIC,
                            Modifier.FINAL
                    )
                    .initializer(
                            "\n    new $T<>($T.class, $S, $T.class, null)",
                            VALIDATOR_CLASS_NAME,
                            e.getKey(),
                            e.getValue(),
                            type.getClassName()
                    );
            typeBuilder.addField(builder.build());
        }
        for (ImmutableProp prop : type.getProps().values()) {
            for (Map.Entry<ClassName, String> e : prop.getValidationMessageMap().entrySet()) {
                FieldSpec.Builder builder = FieldSpec
                        .builder(
                                ParameterizedTypeName.get(
                                        VALIDATOR_CLASS_NAME,
                                        prop.getTypeName()
                                ),
                                Constants.validatorFieldName(prop, e.getKey()),
                                Modifier.PRIVATE,
                                Modifier.STATIC,
                                Modifier.FINAL
                        )
                        .initializer(
                                "\n    new $T<>($T.class, $S, $T.class, $L)",
                                VALIDATOR_CLASS_NAME,
                                e.getKey(),
                                e.getValue(),
                                type.getClassName(),
                                prop.getId()
                        );
                typeBuilder.addField(builder.build());
            }
        }
    }

    private void addConstructor() {
        MethodSpec.Builder builder = MethodSpec
                .constructorBuilder()
                .addParameter(DRAFT_CONTEXT_CLASS_NAME, "ctx")
                .addParameter(type.getClassName(), "base")
                .addStatement("$L = ctx", DRAFT_FIELD_CTX)
                .beginControlFlow("if (base != null)")
                .addStatement("$L = ($T)base", DRAFT_FIELD_BASE, type.getImplClassName())
                .endControlFlow()
                .beginControlFlow("else")
                .addStatement("$L = new $T()", DRAFT_FIELD_BASE, type.getImplClassName())
                .endControlFlow();
        typeBuilder.addMethod(builder.build());
    }

    private void addReadonlyMethods() {
        typeBuilder.addMethod(
                MethodSpec
                        .methodBuilder("__isLoaded")
                        .addModifiers(Modifier.PUBLIC)
                        .addAnnotation(Override.class)
                        .addParameter(int.class, "prop")
                        .returns(boolean.class)
                        .addStatement("return $L.__isLoaded(prop)", UNMODIFIED)
                        .build()
        );
        typeBuilder.addMethod(
                MethodSpec
                        .methodBuilder("__isLoaded")
                        .addModifiers(Modifier.PUBLIC)
                        .addAnnotation(Override.class)
                        .addParameter(String.class, "prop")
                        .returns(boolean.class)
                        .addStatement("return $L.__isLoaded(prop)", UNMODIFIED)
                        .build()
        );
        typeBuilder.addMethod(
                MethodSpec
                        .methodBuilder("__isVisible")
                        .addModifiers(Modifier.PUBLIC)
                        .addAnnotation(Override.class)
                        .addParameter(int.class, "prop")
                        .returns(boolean.class)
                        .addStatement("return $L.__isVisible(prop)", UNMODIFIED)
                        .build()
        );
        typeBuilder.addMethod(
                MethodSpec
                        .methodBuilder("__isVisible")
                        .addModifiers(Modifier.PUBLIC)
                        .addAnnotation(Override.class)
                        .addParameter(String.class, "prop")
                        .returns(boolean.class)
                        .addStatement("return $L.__isVisible(prop)", UNMODIFIED)
                        .build()
        );
        typeBuilder.addMethod(
                MethodSpec
                        .methodBuilder("hashCode")
                        .addModifiers(Modifier.PUBLIC)
                        .addAnnotation(Override.class)
                        .returns(int.class)
                        .addStatement("return $L.hashCode()", UNMODIFIED)
                        .build()
        );
        typeBuilder.addMethod(
                MethodSpec
                        .methodBuilder("__hashCode")
                        .addModifiers(Modifier.PUBLIC)
                        .addAnnotation(Override.class)
                        .addParameter(boolean.class, "shallow")
                        .returns(int.class)
                        .addStatement("return $L.__hashCode(shallow)", UNMODIFIED)
                        .build()
        );
        typeBuilder.addMethod(
                MethodSpec
                        .methodBuilder("equals")
                        .addModifiers(Modifier.PUBLIC)
                        .addAnnotation(Override.class)
                        .addParameter(Object.class, "obj")
                        .returns(boolean.class)
                        .addStatement("return $L.equals(obj)", UNMODIFIED)
                        .build()
        );
        typeBuilder.addMethod(
                MethodSpec
                        .methodBuilder("__equals")
                        .addModifiers(Modifier.PUBLIC)
                        .addAnnotation(Override.class)
                        .addParameter(Object.class, "obj")
                        .addParameter(boolean.class, "shallow")
                        .returns(boolean.class)
                        .addStatement("return $L.__equals(obj, shallow)", UNMODIFIED)
                        .build()
        );
    }

    private void addGetter(ImmutableProp prop) {
        if (prop.getManyToManyViewBaseProp() != null) {
            return;
        }

        MethodSpec.Builder builder = MethodSpec
                .methodBuilder(prop.getGetterName())
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .returns(prop.getDraftTypeName(false));
        if (prop.isBeanStyle()) {
            builder.addAnnotation(JSON_IGNORE_CLASS_NAME);
        }
        if (prop.isNullable()) {
            builder.addAnnotation(Nullable.class);
        }

        ImmutableProp baseProp = prop.getIdViewBaseProp();
        if (baseProp != null) {
            if (baseProp.isList()) {
                builder.addStatement(
                        "$T<$T> __ids = new $T($L().size())",
                        LIST_CLASS_NAME,
                        baseProp.getTargetType().getIdProp().getTypeName().box(),
                        ArrayList.class,
                        baseProp.getGetterName()
                );
                builder.beginControlFlow(
                        "for ($T __target : $L())",
                        baseProp.getElementTypeName(),
                        baseProp.getGetterName()
                );
                builder.addStatement(
                        "__ids.add(__target.$L())",
                        baseProp.getTargetType().getIdProp().getGetterName()
                );
                builder.endControlFlow();
                builder.addStatement("return __ids");
            } else {
                builder.addStatement("$T __target = $L()", baseProp.getElementTypeName(), baseProp.getGetterName());
                builder.addStatement(
                        prop.isNullable() ?
                                "return __target != null ? __target.$L() : null" :
                                "__target.$L()",
                        baseProp.getTargetType().getIdProp().getGetterName()
                );
            }
        } else if (prop.isList()) {
            builder.addCode(
                    "return $L.$L($L.$L(), $T.class, $L);",
                    DRAFT_FIELD_CTX,
                    "toDraftList",
                    UNMODIFIED,
                    prop.getGetterName(),
                    prop.getElementTypeName(),
                    prop.isAssociation(false)
            );
        } else if (prop.isAssociation(false)) {
            builder.addCode(
                    "return $L.$L($L.$L());",
                    DRAFT_FIELD_CTX,
                    "toDraftObject",
                    UNMODIFIED,
                    prop.getGetterName()
            );
        } else {
            builder.addCode("return $L.$L();", UNMODIFIED, prop.getGetterName());
        }
        typeBuilder.addMethod(builder.build());
    }

    private void addCreator(ImmutableProp prop) {
        if (prop.getManyToManyViewBaseProp() != null) {
            return;
        }
        if (!prop.isAssociation(false) && !prop.isList()) {
            return;
        }
        MethodSpec.Builder builder = MethodSpec
                .methodBuilder(prop.getGetterName())
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .addParameter(boolean.class, "autoCreate")
                .returns(prop.getDraftTypeName(true));
        if (prop.isNullable()) {
            builder.beginControlFlow(
                    "if (autoCreate && (!__isLoaded($L) || $L() == null))",
                    prop.getId(),
                    prop.getGetterName()
            );
        } else {
            builder.beginControlFlow(
                    "if (autoCreate && (!__isLoaded($L)))",
                    prop.getId()
            );
        }
        if (prop.isList()) {
            builder.addStatement(
                    "$L(new $T<>())",
                    prop.getSetterName(),
                    ArrayList.class
            );
        } else {
            builder.addStatement(
                    "$L($T.$L.produce(null, null))",
                    prop.getSetterName(),
                    prop.getDraftElementTypeName(),
                    "$"
            );
        }
        builder.endControlFlow();
        if (prop.isList()) {
            builder.addCode(
                    "return $L.$L($L.$L(), $T.class, $L);",
                    DRAFT_FIELD_CTX,
                    "toDraftList",
                    UNMODIFIED,
                    prop.getGetterName(),
                    prop.getElementType(),
                    prop.isAssociation(false)
            );
        } else {
            builder.addCode(
                    "return $L.$L($L.$L());",
                    DRAFT_FIELD_CTX,
                    "toDraftObject",
                    UNMODIFIED,
                    prop.getGetterName()
            );
        }
        typeBuilder.addMethod(builder.build());
    }

    private void addSetter(ImmutableProp prop) {
        if (prop.isJavaFormula() || prop.getManyToManyViewBaseProp() != null) {
            return;
        }
        MethodSpec.Builder builder = MethodSpec
                .methodBuilder(prop.getSetterName())
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .addParameter(prop.getTypeName(), prop.getName())
                .returns(type.getDraftClassName());

        ImmutableProp baseProp = prop.getIdViewBaseProp();
        if (baseProp != null) {
            builder.beginControlFlow("if ($L != null)", prop.getName());
            if (prop.isList()) {
                builder.addStatement(
                        "$T<$T> __targets = new $T($L.size())",
                        LIST_CLASS_NAME,
                        baseProp.getElementTypeName(),
                        ArrayList.class,
                        prop.getName()
                );
                builder.beginControlFlow(
                        "for ($T __id : $L)",
                        baseProp.getTargetType().getIdProp().getTypeName(),
                        prop.getName()
                );
                builder.addStatement(
                        "__targets.add($T.makeIdOnly($T.class, __id))",
                        ImmutableObjects.class,
                        baseProp.getElementTypeName()
                );
                builder.endControlFlow();
                builder.addStatement("$L(__targets)", baseProp.getSetterName());
            } else {
                builder.addStatement(
                        "$L($T.makeIdOnly($T.class, $L))",
                        baseProp.getSetterName(),
                        ImmutableObjects.class,
                        baseProp.getElementTypeName(),
                        prop.getName()
                );
            }
            builder.nextControlFlow("else");
            if (prop.isList()) {
                builder.addStatement("$L($T.emptyList())", baseProp.getSetterName(), COLLECTIONS_CLASS_NAME);
            } else {
                builder.addStatement("$L(null)", baseProp.getSetterName());
            }
            builder.endControlFlow();
        } else {

            new ValidationGenerator(prop, prop.getName(), builder).generate();

            builder.addStatement("$T __tmpModified = $L()", type.getImplClassName(), DRAFT_FIELD_MODIFIED);
            if (prop.isList()) {
                builder.addStatement(
                        "__tmpModified.$L = $T.of(__tmpModified.$L, $L)",
                        prop.getName(),
                        NonSharedList.class,
                        prop.getName(),
                        prop.getName()
                );
            } else {
                builder.addStatement("__tmpModified.$L = $L", prop.getName(), prop.getName());
            }
            if (prop.isLoadedStateRequired()) {
                builder.addStatement("__tmpModified.$L = true", prop.getLoadedStateName());
            }
        }
        builder.addStatement("return this");
        typeBuilder.addMethod(builder.build());
    }

    private void addUtilMethod(ImmutableProp prop, boolean withBase) {
        if (!prop.isAssociation(false) || prop.getManyToManyViewBaseProp() != null) {
            return;
        }
        String methodName = prop.isList() ? prop.getAdderByName() : prop.getApplierName();
        MethodSpec.Builder builder = MethodSpec
                .methodBuilder(methodName)
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .returns(type.getDraftClassName());
        if (withBase) {
            builder.addParameter(prop.getElementTypeName(), "base");
        }

        ParameterizedTypeName consumerTypeName = ParameterizedTypeName.get(
                DRAFT_CONSUMER_CLASS_NAME,
                prop.getDraftElementTypeName()
        );
        builder.addParameter(
                consumerTypeName,
                "block"
        );
        if (withBase) {
            if (prop.isList()) {
                builder.addStatement(
                        "$L(true).add(($T)$T.$L.produce(base, block))",
                        prop.getGetterName(),
                        prop.getDraftElementTypeName(),
                        prop.getDraftElementTypeName(),
                        "$"
                );
            } else {
                builder.addStatement(
                        "$L($T.$L.produce(base, block))",
                        prop.getSetterName(),
                        prop.getDraftElementTypeName(),
                        "$"
                );
            }
        } else {
            builder.addStatement("$L(null, $L)", methodName, "block");
        }
        builder.addStatement("return this");
        typeBuilder.addMethod(builder.build());
    }

    private void addSet(Class<?> argType) {
        MethodSpec.Builder builder = MethodSpec
                .methodBuilder("__set")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(
                        AnnotationSpec
                                .builder(SuppressWarnings.class)
                                .addMember("value", "$S", "unchecked")
                                .build()
                )
                .addAnnotation(Override.class)
                .addParameter(argType, "prop")
                .addParameter(Object.class, "value");
        builder.beginControlFlow("switch (prop)");
        for (ImmutableProp prop : type.getPropsOrderById()) {
            Object arg = argType == int.class ? prop.getId() : '"' + prop.getName() + '"';
            Object castTo = prop.getBoxType();
            if (castTo == null) {
                castTo = prop.getTypeName();
            }
            if (prop.isJavaFormula() || prop.getManyToManyViewBaseProp() != null) {
                builder.addStatement("case $L: break", arg);
            } else if (prop.getTypeName().isPrimitive()) {
                builder.addStatement(
                        "case $L: \n" +
                                "if (value == null) throw new $T($S);\n" +
                                "$L(($T)value);\n" +
                                "break",
                        arg,
                        IllegalArgumentException.class,
                        "'" + prop.getName() + "' cannot be null",
                        prop.getSetterName(),
                        castTo
                );
            } else {
                builder.addStatement(
                        "case $L: $L(($T)value);break",
                        arg,
                        prop.getSetterName(),
                        castTo
                );
            }
        }
        builder.addStatement(
                "default: throw new IllegalArgumentException($S + prop + $S)",
                "Illegal property " +
                        (argType == String.class ? "name" : "id") +
                        " for \"" + type + "\": \"",
                "\""
        );
        builder.endControlFlow();
        typeBuilder.addMethod(builder.build());
    }

    private void addShow(Class<?> argType) {
        MethodSpec.Builder builder = MethodSpec
                .methodBuilder("__show")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .addParameter(argType, "prop")
                .addParameter(TypeName.BOOLEAN, "visible");
        builder.beginControlFlow("switch (prop)");
        for (ImmutableProp prop : type.getPropsOrderById()) {
            if (prop.isVisibilityControllable()) {
                Object arg = argType == int.class ? prop.getId() : '"' + prop.getName() + '"';
                builder.addStatement(
                        "case $L: $L().$L = visible;break",
                        arg,
                        DRAFT_FIELD_MODIFIED,
                        prop.getVisibleName()
                );
            }
        }
        builder.addStatement(
                "default: throw new IllegalArgumentException(\n$>$S + \nprop + \n$S + \n$S\n$<)",
                "Illegal property " +
                        (argType == String.class ? "name" : "id") +
                        " for \"" + type + "\": \"",
                "\",it does not exists or the visibility of that property is not controllable",
                "(Only non-abstract formula property can be used)"
        );
        builder.endControlFlow();
        typeBuilder.addMethod(builder.build());
    }

    private void addUnload(Class<?> argType) {
        MethodSpec.Builder builder = MethodSpec
                .methodBuilder("__unload")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .addParameter(argType, "prop");
        builder.beginControlFlow("switch (prop)");
        for (ImmutableProp prop : type.getPropsOrderById()) {
            Object arg = argType == int.class ? prop.getId() : '"' + prop.getName() + '"';
            if (prop.getBaseProp() != null) {
                builder.addStatement(
                        "case $L: __unload($L);break",
                        arg,
                        prop.getBaseProp().getId()
                );
            } else if (prop.isJavaFormula()) {
                builder.addStatement("case $L: break", arg);
            } else if (prop.isLoadedStateRequired()) {
                builder.addStatement(
                        "case $L: $L().$L = false;break",
                        arg,
                        DRAFT_FIELD_MODIFIED,
                        prop.getLoadedStateName()
                );
            } else {
                builder.addStatement(
                        "case $L: $L().$L = null;break",
                        arg,
                        DRAFT_FIELD_MODIFIED,
                        prop.getName()
                );
            }
        }
        builder.addStatement(
                "default: throw new IllegalArgumentException($S + prop + $S)",
                "Illegal property " +
                        (argType == String.class ? "name" : "id") +
                        " for \"" + type + "\": \"",
                "\", it does not exist or its loaded state is not controllable"
        );
        builder.endControlFlow();
        typeBuilder.addMethod(builder.build());
    }

    private void addDraftContext() {
        MethodSpec.Builder builder = MethodSpec
                .methodBuilder("__draftContext")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .returns(DraftContext.class)
                .addStatement("return $L", DRAFT_FIELD_CTX);
        typeBuilder.addMethod(builder.build());
    }

    private void addResolve() {
        MethodSpec.Builder builder = MethodSpec
                .methodBuilder("__resolve")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .returns(Object.class);

        builder
                .beginControlFlow("if ($L)", DRAFT_FIELD_RESOLVING)
                .addStatement("throw new $T()", CircularReferenceException.class)
                .endControlFlow();

        builder
                .addStatement("$L = true", DRAFT_FIELD_RESOLVING)
                .beginControlFlow("try");
        addResolveCode(builder);
        builder
                .endControlFlow()
                .beginControlFlow("finally")
                .addStatement("$L = false", DRAFT_FIELD_RESOLVING)
                .endControlFlow();
        typeBuilder.addMethod(builder.build());
    }

    private void addResolveCode(MethodSpec.Builder builder) {
        builder
                .addStatement("Implementor base = $L", DRAFT_FIELD_BASE)
                .addStatement("Impl __tmpModified = $L", DRAFT_FIELD_MODIFIED);

        if (type.getProps().values().stream().anyMatch(it -> it.isAssociation(false) || it.isList())) {
            builder.beginControlFlow("if (__tmpModified == null)");
            for (ImmutableProp prop : type.getProps().values()) {
                if (prop.isValueRequired() && (prop.isAssociation(false) || prop.isList())) {
                    builder.beginControlFlow("if (base.__isLoaded($L))", prop.getId());
                    builder.addStatement(
                            "$T oldValue = base.$L()",
                            prop.getTypeName(),
                            prop.getGetterName()
                    );
                    builder.addStatement(
                            "$T newValue = $L.$L(oldValue)",
                            prop.getTypeName(),
                            DRAFT_FIELD_CTX,
                            prop.isList() ? "resolveList" : "resolveObject"
                    );
                    if (prop.isList()) {
                        builder.beginControlFlow("if (oldValue != newValue)");
                    } else {
                        builder.beginControlFlow(
                                "if (!$T.equals(oldValue, newValue, true))",
                                ImmutableSpi.class
                        );
                    }
                    builder.addStatement("$L(newValue)", prop.getSetterName());
                    builder.endControlFlow();
                    builder.endControlFlow();
                }
            }
            builder.addStatement("__tmpModified = $L", DRAFT_FIELD_MODIFIED);
            builder.endControlFlow();

            builder.beginControlFlow("else");
            for (ImmutableProp prop : type.getProps().values()) {
                if (prop.isValueRequired()) {
                    if (prop.isList()) {
                        builder.addStatement(
                                "__tmpModified.$L = $T.of(__tmpModified.$L, $L.$L(__tmpModified.$L))",
                                prop.getName(),
                                NonSharedList.class,
                                prop.getName(),
                                DRAFT_FIELD_CTX,
                                "resolveList",
                                prop.getName()
                        );
                    } else if (prop.isAssociation(false)) {
                        builder.addStatement(
                                "__tmpModified.$L = $L.$L(__tmpModified.$L)",
                                prop.getName(),
                                DRAFT_FIELD_CTX,
                                "resolveObject",
                                prop.getName()
                        );
                    }
                }
            }
            builder.endControlFlow();
        }

        builder
                .beginControlFlow(
                        "if (__tmpModified == null || $T.equals(base, __tmpModified, true))",
                        ImmutableSpi.class
                )
                .addStatement("return base")
                .endControlFlow();
        for (Map.Entry<ClassName, String> e : type.getValidationMessageMap().entrySet()) {
            builder.addStatement("$L.validate(__tmpModified)", Constants.validatorFieldName(e.getKey()));
        }
        builder.addStatement("return __tmpModified");
    }

    private void addModified() {
        MethodSpec.Builder builder = MethodSpec
                .methodBuilder(DRAFT_FIELD_MODIFIED)
                .addModifiers(Modifier.PRIVATE)
                .returns(type.getImplClassName())
                .addStatement("$T __tmpModified = $L", type.getImplClassName(), DRAFT_FIELD_MODIFIED)
                .beginControlFlow("if (__tmpModified == null)")
                .addStatement("__tmpModified = $L.clone()", DRAFT_FIELD_BASE)
                .addStatement("$L = __tmpModified", DRAFT_FIELD_MODIFIED)
                .endControlFlow()
                .addStatement("return __tmpModified");
        typeBuilder.addMethod(builder.build());
    }

    private static final String UNMODIFIED =
            "(" +
                    DRAFT_FIELD_MODIFIED +
                    "!= null ? " +
                    DRAFT_FIELD_MODIFIED +
                    " : " +
                    DRAFT_FIELD_BASE +
                    ")";
}