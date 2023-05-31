package org.babyfish.jimmer.apt.generator;

import com.squareup.javapoet.*;
import org.babyfish.jimmer.CircularReferenceException;
import org.babyfish.jimmer.ImmutableObjects;
import org.babyfish.jimmer.apt.meta.ImmutableProp;
import org.babyfish.jimmer.apt.meta.ImmutableType;
import org.babyfish.jimmer.meta.PropId;
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
                .addSuperinterface(type.getImplementorClassName())
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
        addSet(PropId.class);
        addSet(String.class);
        addShow(PropId.class);
        addShow(String.class);
        addUnload(PropId.class);
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
                                "\n    new $T<>($T.class, $S, $T.class, $T.byIndex($L))",
                                VALIDATOR_CLASS_NAME,
                                e.getKey(),
                                e.getValue(),
                                type.getClassName(),
                                PROP_ID_CLASS_NAME,
                                prop.getSlotName()
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
                .addStatement("$L = new $T()", DRAFT_FIELD_MODIFIED, type.getImplClassName())
                .endControlFlow();
        typeBuilder.addMethod(builder.build());
    }

    private void addReadonlyMethods() {
        typeBuilder.addMethod(
                MethodSpec
                        .methodBuilder("__isLoaded")
                        .addModifiers(Modifier.PUBLIC)
                        .addAnnotation(Override.class)
                        .addParameter(PropId.class, "prop")
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
                        .addParameter(PropId.class, "prop")
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
        typeBuilder.addMethod(
                MethodSpec
                        .methodBuilder("toString")
                        .addModifiers(Modifier.PUBLIC)
                        .addAnnotation(Override.class)
                        .returns(String.class)
                        .addStatement("return $T.toString($L)", ImmutableObjects.class, UNMODIFIED)
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
                                "return __target.$L()",
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
        ImmutableProp realProp = prop.getIdViewBaseProp();
        if (realProp == null) {
            realProp = prop;
        }
        MethodSpec.Builder builder = MethodSpec
                .methodBuilder(prop.getGetterName())
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .addParameter(boolean.class, "autoCreate")
                .returns(prop.getDraftTypeName(true));
        if (prop.isNullable()) {
            builder.beginControlFlow(
                    "if (autoCreate && (!__isLoaded($T.byIndex($L)) || $L() == null))",
                    PROP_ID_CLASS_NAME,
                    realProp.getSlotName(),
                    realProp.getGetterName()
            );
        } else {
            builder.beginControlFlow(
                    "if (autoCreate && (!__isLoaded($T.byIndex($L))))",
                    PROP_ID_CLASS_NAME,
                    realProp.getSlotName()
            );
        }
        if (prop.isList()) {
            builder.addStatement(
                    "$L(new $T<>())",
                    realProp.getSetterName(),
                    ArrayList.class
            );
        } else {
            builder.addStatement(
                    "$L($T.$L.produce(null, null))",
                    realProp.getSetterName(),
                    realProp.getDraftElementTypeName(),
                    "$"
            );
        }
        builder.endControlFlow();
        if (prop.isList()) {
            if (realProp != prop) {
                builder.addStatement(
                        "return new $T<>($T.TYPE, $L())",
                        MUTABLE_ID_VIEW_LIST_CLASS_NAME,
                        realProp.getTargetType().getProducerClassName(),
                        realProp.getGetterName()
                );
            } else {
                builder.addCode(
                        "return $L.$L($L.$L(), $T.class, $L);",
                        DRAFT_FIELD_CTX,
                        "toDraftList",
                        UNMODIFIED,
                        prop.getGetterName(),
                        prop.getElementType(),
                        prop.isAssociation(false)
                );
            }
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
            if (!prop.getReturnType().getKind().isPrimitive()) {
                builder.beginControlFlow("if ($L != null)", prop.getName());
            }
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
            if (!prop.getReturnType().getKind().isPrimitive()) {
                builder.nextControlFlow("else");
                if (prop.isList()) {
                    builder.addStatement("$L($T.emptyList())", baseProp.getSetterName(), COLLECTIONS_CLASS_NAME);
                } else {
                    builder.addStatement("$L(null)", baseProp.getSetterName());
                }
                builder.endControlFlow();
            }
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
        CaseAppender appender = new CaseAppender(builder, type, argType);
        if (argType == PropId.class) {
            builder.addStatement("int __propIndex = prop.asIndex()");
            builder.beginControlFlow("switch (__propIndex)");
            appender.addIllegalCase();
            builder.addStatement("__set(prop.asName(), value)");
            builder.addStatement("return");
        } else {
            builder.beginControlFlow("switch (prop)");
        }
        for (ImmutableProp prop : type.getPropsOrderById()) {
            Object castTo = prop.getBoxType();
            if (castTo == null) {
                castTo = prop.getTypeName();
            }
            appender.addCase(prop);
            if (prop.isJavaFormula() || prop.getManyToManyViewBaseProp() != null) {
                builder.addStatement("break");
            } else if (prop.getTypeName().isPrimitive()) {
                builder.addStatement(
                        "if (value == null) throw new $T($S);\n" +
                                "$L(($T)value);\n" +
                                "break",
                        IllegalArgumentException.class,
                        "'" + prop.getName() + "' cannot be null",
                        prop.getSetterName(),
                        castTo
                );
            } else {
                builder.addStatement(
                        "$L(($T)value);break",
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
        CaseAppender appender = new CaseAppender(builder, type, argType);
        if (argType == PropId.class) {
            builder.addStatement("int __propIndex = prop.asIndex()");
            builder.beginControlFlow("switch (__propIndex)");
            appender.addIllegalCase();
            builder.addStatement("__show(prop.asName(), visible)");
            builder.addStatement("return");
        } else {
            builder.beginControlFlow("switch (prop)");
        }
        for (ImmutableProp prop : type.getPropsOrderById()) {
            appender.addCase(prop);
            builder.addStatement(
                    "$L().__visibility.show($L, visible);break",
                    DRAFT_FIELD_MODIFIED,
                    prop.getSlotName()
            );
        }
        builder.addStatement(
                "default: throw new IllegalArgumentException(\n$>$S + \nprop + \n$S\n$<)",
                "Illegal property " +
                        (argType == String.class ? "name" : "id") +
                        " for \"" + type + "\": \"",
                "\",it does not exists"
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
        CaseAppender appender = new CaseAppender(builder, type, argType);
        if (argType == PropId.class) {
            builder.addStatement("int __propIndex = prop.asIndex()");
            builder.beginControlFlow("switch (__propIndex)");
            appender.addIllegalCase();
            builder.addStatement("__unload(prop.asName())");
            builder.addStatement("return");
        } else {
            builder.beginControlFlow("switch (prop)");
        }
        for (ImmutableProp prop : type.getPropsOrderById()) {
            appender.addCase(prop);
            if (prop.getBaseProp() != null) {
                builder.addStatement(
                        "__unload($T.byIndex($L));break",
                        PROP_ID_CLASS_NAME,
                        prop.getBaseProp().getSlotName()
                );
            } else if (prop.isJavaFormula()) {
                builder.addStatement("break");
            } else if (prop.isLoadedStateRequired()) {
                builder.addStatement(
                        "$L().$L = false;break",
                        DRAFT_FIELD_MODIFIED,
                        prop.getLoadedStateName()
                );
            } else {
                builder.addStatement(
                        "$L().$L = null;break",
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
                    builder.beginControlFlow(
                            "if (base.__isLoaded($T.byIndex($L)))",
                            PROP_ID_CLASS_NAME,
                            prop.getSlotName()
                    );
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
                        "if (" +
                                DRAFT_FIELD_BASE +
                                " != null && (__tmpModified == null || $T.equals(base, __tmpModified, true)))",
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