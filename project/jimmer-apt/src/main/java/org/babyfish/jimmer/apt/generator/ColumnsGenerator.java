package org.babyfish.jimmer.apt.generator;

import com.squareup.javapoet.*;
import org.babyfish.jimmer.apt.GeneratorException;
import org.babyfish.jimmer.apt.TypeUtils;
import org.babyfish.jimmer.apt.meta.ImmutableProp;
import org.babyfish.jimmer.apt.meta.ImmutableType;
import org.babyfish.jimmer.sql.JoinType;

import javax.annotation.processing.Filer;
import javax.lang.model.element.Modifier;

import java.io.IOException;

import static org.babyfish.jimmer.apt.generator.Constants.COLUMNS_FOR_CLASS_NAME;
import static org.babyfish.jimmer.apt.generator.Constants.PROP_EXPRESSION_CLASS_NAME;

public class ColumnsGenerator {

    private final TypeUtils typeUtils;

    private final ImmutableType type;

    private final boolean isColumnsEx;

    private final Filer filer;

    private TypeSpec.Builder typeBuilder;

    public ColumnsGenerator(
            TypeUtils typeUtils,
            ImmutableType type,
            boolean isColumnsEx,
            Filer filer
    ) {
        this.typeUtils = typeUtils;
        this.type = type;
        this.isColumnsEx = isColumnsEx;
        this.filer = filer;
    }

    public void generate() {
        try {
            JavaFile
                    .builder(
                            type.getPackageName(),
                            generateImpl()
                    )
                    .indent("    ")
                    .build()
                    .writeTo(filer);
        } catch (IOException ex) {
            throw new GeneratorException(
                    String.format(
                            "Cannot generate props class for '%s'",
                            type.getName()
                    ),
                    ex
            );
        }
    }

    private TypeSpec generateImpl() {
        String suffix = isColumnsEx ? "ColumnsEx" : "Columns";
        TypeVariableName typeVariable = TypeVariableName.get("E", type.getClassName());
        typeBuilder = TypeSpec.interfaceBuilder(type.getName() + suffix);
        typeBuilder.addTypeVariable(typeVariable);
        typeBuilder.addAnnotation(
                AnnotationSpec
                        .builder(COLUMNS_FOR_CLASS_NAME)
                        .addMember("value", "$T.class", type.getClassName())
                        .build()
        );
        if (isColumnsEx) {
            typeBuilder.addSuperinterface(
                    type.getColumnsClassName(typeVariable)
            );
            if (type.getSuperType() != null) {
                typeBuilder.addSuperinterface(
                        type.getSuperType().getColumnsExClassName(typeVariable)
                );
            } else {
                typeBuilder.addSuperinterface(
                        ParameterizedTypeName.get(
                                Constants.COLUMNS_EX_CLASS_NAME,
                                typeVariable
                        )
                );
            }
        } else {
            if (type.getSuperType() != null) {
                typeBuilder.addSuperinterface(
                        type.getSuperType().getColumnsClassName(typeVariable)
                );
            } else {
                typeBuilder.addSuperinterface(
                        ParameterizedTypeName.get(
                                Constants.COLUMNS_CLASS_NAME,
                                typeVariable
                        )
                );
            }
        }
        try {
            for (ImmutableProp prop : type.getDeclaredProps().values()) {
                if (prop.isList() == isColumnsEx) {
                    addProperty(prop, false);
                    addProperty(prop, true);
                }
            }
            return typeBuilder.build();
        } finally {
            typeBuilder = null;
        }
    }

    private void addProperty(
            ImmutableProp prop,
            boolean withJoinType
    ) {
        MethodSpec method = ColumnsGenerator.property(
                typeUtils,
                isColumnsEx,
                prop,
                withJoinType,
                false
        );
        if (method != null) {
            typeBuilder.addMethod(method);
        }
    }

    static MethodSpec property(
            TypeUtils typeUtils,
            boolean isColumnsEx,
            ImmutableProp prop,
            boolean withJoinType,
            boolean withImplementation
    ) {
        if (prop.isTransient()) {
            return null;
        }
        if (withJoinType && !prop.isAssociation()) {
            return null;
        }

        TypeName returnType;
        if (prop.isAssociation()) {
            if (isColumnsEx) {
                returnType = typeUtils
                        .getImmutableType(prop.getElementType())
                        .getTableExClassName();
            } else {
                returnType = typeUtils
                        .getImmutableType(prop.getElementType())
                        .getTableClassName();
            }
        } else {
            if (prop.getTypeName().isPrimitive()) {
                returnType = ParameterizedTypeName.get(
                        Constants.PROP_NUMERIC_EXPRESSION_CLASS_NAME,
                        prop.getTypeName().box()
                );
            } else if (typeUtils.isString(prop.getReturnType())) {
                returnType = Constants.PROP_STRING_EXPRESSION_CLASS_NAME;
            } else if (typeUtils.isNumber(prop.getReturnType())) {
                returnType = ParameterizedTypeName.get(
                        Constants.PROP_NUMERIC_EXPRESSION_CLASS_NAME,
                        prop.getTypeName()
                );
            } else if (typeUtils.isComparable(prop.getReturnType())) {
                returnType = ParameterizedTypeName.get(
                        Constants.PROP_COMPARABLE_EXPRESSION_CLASS_NAME,
                        prop.getTypeName()
                );
            } else {
                returnType = ParameterizedTypeName.get(
                        PROP_EXPRESSION_CLASS_NAME,
                        prop.getTypeName()
                );
            }
        }

        MethodSpec.Builder builder = MethodSpec
                .methodBuilder(prop.getName())
                .addModifiers(Modifier.PUBLIC)
                .returns(returnType);
        if (withImplementation) {
            builder.addAnnotation(Override.class);
        } else {
            builder.addModifiers(Modifier.ABSTRACT);
        }
        if (withJoinType) {
            builder.addParameter(JoinType.class, "joinType");
        }
        if (withImplementation) {
            if (prop.isAssociation()) {
                if (withJoinType) {
                    builder.addStatement("return join($S, joinType)", prop.getName());
                } else {
                    builder.addStatement("return join($S)", prop.getName());
                }
            } else {
                builder.addStatement("return get($S)", prop.getName());
            }
        }
        return builder.build();
    }
}
