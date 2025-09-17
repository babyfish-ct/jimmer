package org.babyfish.jimmer.apt.immutable.generator;

import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import org.babyfish.jimmer.apt.immutable.meta.ImmutableProp;
import org.babyfish.jimmer.impl.util.StringUtil;
import org.babyfish.jimmer.lang.OldChain;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import javax.lang.model.element.Modifier;
import java.util.Objects;

class AssociatedIdGenerator {

    private final TypeSpec.Builder typeBuilder;

    private final boolean withImplementation;

    public AssociatedIdGenerator(TypeSpec.Builder typeBuilder, boolean withImplementation) {
        this.typeBuilder = typeBuilder;
        this.withImplementation = withImplementation;
    }

    void getter(ImmutableProp prop) {
        if (skip(prop)) {
            return;
        }
        TypeName idTypeName = prop.getTargetType().getIdProp().getTypeName();
        if (prop.isNullable()) {
            idTypeName = idTypeName.box();
        }
        MethodSpec.Builder builder = MethodSpec
                .methodBuilder(StringUtil.identifier(prop.getGetterName(), "Id"))
                .addModifiers(Modifier.PUBLIC)
                .returns(idTypeName);
        if (!idTypeName.isPrimitive()) {
            builder.addAnnotation(prop.isNullable() ? Nullable.class : NonNull.class);
        }
        builder.addAnnotation(Constants.JSON_IGNORE_CLASS_NAME);
        if (!withImplementation) {
            builder.addModifiers(Modifier.ABSTRACT);
        } else {
            builder.addAnnotation(Override.class);
            if (prop.isNullable()) {
                builder.addStatement("$T $L = $L()", prop.getTargetType().getClassName(), prop.getName(), prop.getGetterName());
                builder.beginControlFlow("if ($L == null)", prop.getName());
                builder.addStatement("return null");
                builder.endControlFlow();
                builder.addStatement("return $L.$L()", prop.getName(), prop.getTargetType().getIdProp().getGetterName());
            } else {
                builder.addStatement("return $L().$L()", prop.getGetterName(), prop.getTargetType().getIdProp().getGetterName());
            }
        }
        typeBuilder.addMethod(builder.build());
    }

    void setter(ImmutableProp prop) {
        if (skip(prop)) {
            return;
        }
        TypeName idTypeName = prop.getTargetType().getIdProp().getTypeName();
        if (prop.isNullable()) {
            idTypeName = idTypeName.box();
        }
        String parameterName = StringUtil.identifier(prop.getName(), "Id");
        ParameterSpec.Builder parameterBuilder = ParameterSpec
                .builder(idTypeName, parameterName);
        if (!idTypeName.isPrimitive()) {
            parameterBuilder.addAnnotation(prop.isNullable() ? Nullable.class : NonNull.class);
        }
        MethodSpec.Builder builder = MethodSpec
                .methodBuilder(StringUtil.identifier(prop.getSetterName(), "Id"))
                .addModifiers(Modifier.PUBLIC)
                .addParameter(parameterBuilder.build())
                .returns(prop.getDeclaringType().getDraftClassName())
                .addAnnotation(OldChain.class);
        if (!withImplementation) {
            builder.addModifiers(Modifier.ABSTRACT);
        } else {
            builder.addAnnotation(Override.class);
            if (prop.isNullable()) {
                builder.beginControlFlow("if ($L == null)", parameterName);
                builder.addStatement("$L(null)", prop.getSetterName());
                builder.addStatement("return this");
                builder.endControlFlow();
                builder.addStatement(
                        "$L(true).$L($L)",
                        prop.getGetterName(),
                        prop.getTargetType().getIdProp().getSetterName(),
                        parameterName
                );
            } else if (prop.getTypeName().isPrimitive()) {
                builder.addStatement(
                        "$L(true).$L($L)",
                        prop.getGetterName(),
                        prop.getTargetType().getIdProp().getSetterName(),
                        parameterName
                );
            } else {
                builder.addStatement(
                        "$L(true).$L($T.requireNonNull($L, $S))",
                        prop.getGetterName(),
                        prop.getTargetType().getIdProp().getSetterName(),
                        Objects.class,
                        parameterName,
                        "\"" + prop.getName() + "\" cannot be null"
                );
            }
            builder.addStatement("return this");
        }
        typeBuilder.addMethod(builder.build());
    }

    private static boolean skip(ImmutableProp prop) {
        return !prop.isAssociation(true) ||
                prop.isList() ||
                prop.getIdViewProp() != null ||
                prop.getDeclaringType().getProps().containsKey(StringUtil.identifier(prop.getName(), "Id"));
    }
}
