package org.babyfish.jimmer.apt.generator;

import com.squareup.javapoet.*;
import org.babyfish.jimmer.apt.GeneratorException;
import org.babyfish.jimmer.apt.TypeUtils;
import org.babyfish.jimmer.apt.meta.ImmutableProp;
import org.babyfish.jimmer.apt.meta.ImmutableType;

import javax.annotation.processing.Filer;
import javax.lang.model.element.Modifier;
import java.io.IOException;

import static org.babyfish.jimmer.apt.generator.Constants.*;

public class TableGenerator {

    private final TypeUtils typeUtils;

    private final ImmutableType type;
    
    private final boolean isTableEx;

    private final Filer filer;

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
        if (!isTableEx) {
            typeBuilder.addSuperinterface(type.getPropsClassName());
        }
        if (isTableEx) {
            typeBuilder.superclass(type.getTableClassName());
            typeBuilder.addSuperinterface(
                    ParameterizedTypeName.get(
                            Constants.TABLE_EX_CLASS_NAME,
                            type.getClassName()
                    )
            );
        } else {
            typeBuilder.superclass(
                    ParameterizedTypeName.get(
                            Constants.ABSTRACT_TYPED_TABLE_CLASS_NAME,
                            type.getClassName()
                    )
            );
        }
        addInstanceField();
        addDefaultConstructor();
        addDelayedConstructor();
        addWrapperConstructor();
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

    private void addInstanceField() {
        ClassName className = isTableEx ? type.getTableExClassName() : type.getTableClassName();
        FieldSpec.Builder builder = FieldSpec
                .builder(className, "$", Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                .initializer("new $T()", className);
        typeBuilder.addField(builder.build());
    }

    private void addDefaultConstructor() {
        MethodSpec.Builder builder = MethodSpec
                .constructorBuilder()
                .addModifiers(Modifier.PUBLIC);
        if (isTableEx) {
            builder.addStatement("super()");
        } else {
            builder.addStatement("super($T.class)", type.getClassName());
        }
        typeBuilder.addMethod(builder.build());
    }

    private void addDelayedConstructor() {
        MethodSpec.Builder builder = MethodSpec
                .constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addParameter(
                        ParameterizedTypeName.get(
                                DELAYED_OPERATION_CLASS_NAME,
                                type.getClassName()
                        ),
                        "delayedOperation"
                );
        if (isTableEx) {
            builder.addStatement("super(delayedOperation)");
        } else {
            builder.addStatement("super($T.class, delayedOperation)", type.getClassName());
        }
        typeBuilder.addMethod(builder.build());
    }

    private void addWrapperConstructor() {
        MethodSpec.Builder builder = MethodSpec
                .constructorBuilder()
                .addModifiers(Modifier.PUBLIC);
        TypeName tableTypeName = ParameterizedTypeName.get(
                TABLE_IMPLEMENTOR_CLASS_NAME,
                type.getClassName()
        );
        builder
                .addParameter(tableTypeName, "table")
                .addParameter(String.class, "joinDisabledReason")
                .addStatement("super(table, joinDisabledReason)");
        typeBuilder.addMethod(builder.build());
    }

    private void addProperty(
            ImmutableProp prop,
            boolean withJoinType
    ) {
        MethodSpec method = PropsGenerator.property(
                typeUtils,
                isTableEx,
                prop,
                withJoinType,
                true
        );
        if (method != null) {
            typeBuilder.addMethod(method);
        }
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
