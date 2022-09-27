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

import static org.babyfish.jimmer.apt.generator.Constants.*;

public class TableGenerator {

    private TypeUtils typeUtils;

    private ImmutableType type;
    
    private boolean isTableEx;

    private Filer filer;

    private TypeSpec.Builder typeBuilder;

    public TableGenerator(
            TypeUtils typeUtils,
            ImmutableType type,
            boolean isTableEx,
            Filer filer
    ) {
        this.typeUtils = typeUtils;
        this.type = type;
        this.isTableEx = isTableEx;
        this.filer = filer;
    }

    public void generate() {
        try {
            JavaFile
                    .builder(
                            type.getPackageName(),
                            generateTable()
                    )
                    .indent("    ")
                    .build()
                    .writeTo(filer);
        } catch (IOException ex) {
            throw new GeneratorException(
                    String.format(
                            "Cannot generate " +
                                    (isTableEx ? "tableEx" : "table") +
                                    "class for '%s'",
                            type.getName()
                    ),
                    ex
            );
        }
    }

    private TypeSpec generateTable() {
        TypeSpec.Builder oldTypeBuilder = typeBuilder;
        typeBuilder = TypeSpec
                .classBuilder(
                        isTableEx ?
                                type.getTableExClassName().simpleName() :
                                type.getTableClassName().simpleName()
                )
                .addModifiers(Modifier.PUBLIC);
        if (isTableEx) {
            typeBuilder.superclass(type.getTableClassName());
            typeBuilder.addSuperinterface(
                    ParameterizedTypeName.get(
                            Constants.QUERY_TABLE_EX_CLASS_NAME,
                            type.getClassName()
                    )
            );
        } else {
            typeBuilder.superclass(
                    ParameterizedTypeName.get(
                            Constants.ABSTRACT_TABLE_WRAPPER_CLASS_NAME,
                            type.getClassName()
                    )
            );
        }
        addDefaultConstructor();
        addParameterizedConstructor();
        try {
            for (ImmutableProp prop : type.getProps().values()) {
                if (prop.isList() == isTableEx) {
                    addProperty(prop, false);
                    addProperty(prop, true);
                }
            }
            addAsTableEx();
            return typeBuilder.build();
        } finally {
            typeBuilder = oldTypeBuilder;
        }
    }

    private void addDefaultConstructor() {
        MethodSpec.Builder builder = MethodSpec
                .constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addComment("For fluent-API")
                .addStatement("super(null)");
        typeBuilder.addMethod(builder.build());
    }

    private void addParameterizedConstructor() {
        MethodSpec.Builder builder = MethodSpec
                .constructorBuilder()
                .addModifiers(Modifier.PUBLIC);
        TypeName tableTypeName;
        if (isTableEx) {
            tableTypeName = ParameterizedTypeName.get(
                    QUERY_TABLE_EX_CLASS_NAME,
                    type.getClassName()
            );
        } else {
            tableTypeName = ParameterizedTypeName.get(
                    TABLE_CLASS_NAME,
                    type.getClassName()
            );
        }
        builder
                .addParameter(tableTypeName, "table")
                .addStatement("super(table)");
        typeBuilder.addMethod(builder.build());
    }

    private void addProperty(
            ImmutableProp prop,
            boolean withJoinType
    ) {
        if (prop.isTransient()) {
            return;
        }
        if (withJoinType && !prop.isAssociation()) {
            return;
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
        if (isTableEx && !prop.isList()) {
            builder.addAnnotation(Override.class);
        }
        if (withJoinType) {
            builder.addParameter(JoinType.class, "joinType");
        }
        if (prop.isAssociation()) {
            if (withJoinType) {
                builder.addStatement("return join($S, joinType)", prop.getName());
            } else {
                builder.addStatement("return join($S)", prop.getName());
            }
        } else {
            builder.addStatement("return get($S)", prop.getName());
        }
        typeBuilder.addMethod(builder.build());
    }

    private void addAsTableEx() {
        if (isTableEx) {
            return;
        }
        MethodSpec.Builder builder = MethodSpec
                .methodBuilder("asTableEx")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .returns(type.getTableExClassName())
                .addStatement(
                        "return ($T)super.asTableEx()",
                        type.getTableExClassName()
                );
        typeBuilder.addMethod(builder.build());
    }
}
