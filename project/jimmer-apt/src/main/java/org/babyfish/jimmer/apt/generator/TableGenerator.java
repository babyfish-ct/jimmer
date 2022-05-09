package org.babyfish.jimmer.apt.generator;

import com.squareup.javapoet.*;
import org.babyfish.jimmer.apt.GeneratorException;
import org.babyfish.jimmer.apt.TypeUtils;
import org.babyfish.jimmer.apt.meta.ImmutableProp;
import org.babyfish.jimmer.apt.meta.ImmutableType;

import javax.annotation.processing.Filer;
import javax.lang.model.element.Modifier;
import javax.persistence.criteria.JoinType;
import java.io.IOException;

import static org.babyfish.jimmer.apt.generator.Constants.*;

public class TableGenerator {

    private TypeUtils typeUtils;

    private ImmutableType type;

    private Filer filer;

    private TypeSpec.Builder typeBuilder;

    public TableGenerator(
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
                            generateTable(false)
                    )
                    .indent("    ")
                    .build()
                    .writeTo(filer);
        } catch (IOException ex) {
            throw new GeneratorException(
                    String.format(
                            "Cannot generate table class for '%s'",
                            type.getName()
                    ),
                    ex
            );
        }
    }

    private TypeSpec generateTable(boolean subQueryTable) {
        TypeSpec.Builder oldTypeBuilder = typeBuilder;
        typeBuilder = TypeSpec
                .classBuilder(
                        subQueryTable ?
                                type.getSubQueryTableClassName().simpleName() :
                                type.getTableClassName().simpleName()
                )
                .addModifiers(Modifier.PUBLIC);
        if (subQueryTable) {
            typeBuilder.addModifiers(Modifier.STATIC);
            typeBuilder.superclass(type.getTableClassName());
            typeBuilder.addSuperinterface(
                    ParameterizedTypeName.get(
                            Constants.SUB_QUERY_TABLE_CLASS_NAME,
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
            addCreateQuery();
            addCreateSubQuery();
            addCreateWildSubQuery();
        }
        addConstructor(subQueryTable);
        try {
            for (ImmutableProp prop : type.getProps().values()) {
                if (subQueryTable || !prop.isList()){
                    addProperty(prop, subQueryTable, false);
                    addProperty(prop, subQueryTable, true);
                }
            }
            if (!subQueryTable) {
                typeBuilder.addType(generateTable(true));
            }
            return typeBuilder.build();
        } finally {
            typeBuilder = oldTypeBuilder;
        }
    }

    private void addCreateQuery() {
        TypeVariableName typeVariable = TypeVariableName.get("R");
        MethodSpec.Builder builder = MethodSpec
                .methodBuilder("createQuery")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addTypeVariable(typeVariable)
                .addParameter(SQL_CLIENT_CLASS_NAME, "sqlClient")
                .addParameter(
                        ParameterizedTypeName.get(
                                BI_FUNCTION_CLASS_NAME,
                                ParameterizedTypeName.get(
                                        MUTABLE_ROOT_QUERY_CLASS_NAME,
                                        type.getTableClassName()
                                ),
                                type.getTableClassName(),
                                ParameterizedTypeName.get(
                                        CONFIGURABLE_TYPED_ROOT_QUERY_CLASS_NAME,
                                        type.getTableClassName(),
                                        typeVariable
                                )
                        ),
                        "block"
                )
                .returns(
                        ParameterizedTypeName.get(
                                CONFIGURABLE_TYPED_ROOT_QUERY_CLASS_NAME,
                                type.getTableClassName(),
                                typeVariable
                        )
                )
                .addStatement(
                        "return $T.createQuery($T.class, sqlClient, block)",
                        QUERIES_CLASS_NAME,
                        type.getTableClassName()
                );
        typeBuilder.addMethod(builder.build());
    }

    private void addCreateSubQuery() {
        TypeVariableName typeVariable = TypeVariableName.get("R");
        MethodSpec.Builder builder = MethodSpec
                .methodBuilder("createSubQuery")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addTypeVariable(typeVariable)
                .addParameter(Constants.FILTERABLE_CLASS_NAME, "parent")
                .addParameter(
                        ParameterizedTypeName.get(
                                BI_FUNCTION_CLASS_NAME,
                                MUTABLE_SUB_QUERY_CLASS_NAME,
                                type.getSubQueryTableClassName(),
                                ParameterizedTypeName.get(CONFIGURABLE_TYPED_SUB_QUERY_CLASS_NAME, typeVariable)
                        ),
                        "block"
                )
                .returns(ParameterizedTypeName.get(CONFIGURABLE_TYPED_SUB_QUERY_CLASS_NAME, typeVariable))
                .addStatement(
                        "return $T.createSubQuery($T.class, parent, block)",
                        QUERIES_CLASS_NAME,
                        type.getSubQueryTableClassName()
                );
        typeBuilder.addMethod(builder.build());
    }

    private void addCreateWildSubQuery() {
        MethodSpec.Builder builder = MethodSpec
                .methodBuilder("createWildSubQuery")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addParameter(Constants.FILTERABLE_CLASS_NAME, "parent")
                .addParameter(
                        ParameterizedTypeName.get(
                                BI_CONSUMER_CLASS_NAME,
                                MUTABLE_SUB_QUERY_CLASS_NAME,
                                type.getSubQueryTableClassName()
                        ),
                        "block"
                )
                .returns(MUTABLE_SUB_QUERY_CLASS_NAME)
                .addStatement(
                        "return $T.createWildSubQuery($T.class, parent, block)",
                        QUERIES_CLASS_NAME,
                        type.getSubQueryTableClassName()
                );
        typeBuilder.addMethod(builder.build());
    }

    private void addConstructor(boolean subQueryTable) {
        MethodSpec.Builder builder = MethodSpec
                .constructorBuilder()
                .addModifiers(Modifier.PUBLIC);
        TypeName tableTypeName;
        if (subQueryTable) {
            tableTypeName = ParameterizedTypeName.get(
                    SUB_QUERY_TABLE_CLASS_NAME,
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
            boolean subQueryTable,
            boolean withJoinType
    ) {

        if (withJoinType && !prop.isAssociation()) {
            return;
        }

        TypeName returnType;
        if (prop.isAssociation()) {
            if (subQueryTable) {
                returnType = typeUtils
                        .getImmutableType(prop.getElementType())
                        .getSubQueryTableClassName();
            } else {
                returnType = typeUtils
                        .getImmutableType(prop.getElementType())
                        .getTableClassName();
            }
        } else {
            if (prop.getTypeName().isPrimitive()) {
                returnType = ParameterizedTypeName.get(
                        Constants.NUMERIC_EXPRESSION_CLASS_NAME,
                        prop.getTypeName().box()
                );
            } else if (typeUtils.isString(prop.getReturnType())) {
                returnType = Constants.STRING_EXPRESSION_CLASS_NAME;
            } else if (typeUtils.isNumber(prop.getReturnType())) {
                returnType = ParameterizedTypeName.get(
                        Constants.NUMERIC_EXPRESSION_CLASS_NAME,
                        prop.getTypeName()
                );
            } else if (typeUtils.isComparable(prop.getReturnType())) {
                returnType = ParameterizedTypeName.get(
                        Constants.COMPARABLE_EXPRESSION_CLASS_NAME,
                        prop.getTypeName()
                );
            } else {
                returnType = ParameterizedTypeName.get(
                        Constants.EXPRESSION_CLASS_NAME,
                        prop.getTypeName()
                );
            }
        }

        MethodSpec.Builder builder = MethodSpec
                .methodBuilder(prop.getName())
                .addModifiers(Modifier.PUBLIC)
                .returns(returnType);
        if (subQueryTable && !prop.isList()) {
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
}
