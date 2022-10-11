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

    private final Filer filer;

    private TypeSpec.Builder typeBuilder;

    public ColumnsGenerator(
            TypeUtils typeUtils,
            ImmutableType type,
            Filer filer
    ) {
        this.typeUtils = typeUtils;
        this.type = type;
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
        typeBuilder = TypeSpec
                .interfaceBuilder(type.getName() + "Columns")
                .addModifiers(Modifier.PUBLIC);
        typeBuilder.addAnnotation(
                AnnotationSpec
                        .builder(COLUMNS_FOR_CLASS_NAME)
                        .addMember("value", "$T.class", type.getClassName())
                        .build()
        );
        if (type.getSuperType() != null) {
            typeBuilder.addSuperinterface(
                    type.getSuperType().getColumnsClassName()
            );
        } else {
            typeBuilder.addSuperinterface(Constants.COLUMNS_CLASS_NAME);
        }
        try {
            for (ImmutableProp prop : type.getDeclaredProps().values()) {
                if (!prop.isList()) {
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
                false,
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
            boolean isTableEx,
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
            if (isTableEx) {
                returnType = typeUtils
                        .getImmutableType(prop.getElementType())
                        .getTableExClassName();
            } else {
                returnType = typeUtils
                        .getImmutableType(prop.getElementType())
                        .getTableClassName();
            }
        } else {
            if (prop.getTypeName().isPrimitive() && !prop.getTypeName().equals(TypeName.BOOLEAN)) {
                returnType = ParameterizedTypeName.get(
                        Constants.PROP_NUMERIC_EXPRESSION_CLASS_NAME,
                        prop.getTypeName().box()
                );
            } else if (typeUtils.isString(prop.getReturnType())) {
                returnType = Constants.PROP_STRING_EXPRESSION_CLASS_NAME;
            } else if (typeUtils.isNumber(prop.getReturnType())) {
                returnType = ParameterizedTypeName.get(
                        Constants.PROP_NUMERIC_EXPRESSION_CLASS_NAME,
                        prop.getTypeName().box()
                );
            } else if (typeUtils.isComparable(prop.getReturnType())) {
                returnType = ParameterizedTypeName.get(
                        Constants.PROP_COMPARABLE_EXPRESSION_CLASS_NAME,
                        prop.getTypeName().box()
                );
            } else {
                returnType = ParameterizedTypeName.get(
                        PROP_EXPRESSION_CLASS_NAME,
                        prop.getTypeName().box()
                );
            }
        }

        MethodSpec.Builder builder = MethodSpec
                .methodBuilder(prop.getName())
                .addModifiers(Modifier.PUBLIC)
                .returns(returnType);
        if (withImplementation) {
            if (!isTableEx) {
                builder.addAnnotation(Override.class);
            }
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
