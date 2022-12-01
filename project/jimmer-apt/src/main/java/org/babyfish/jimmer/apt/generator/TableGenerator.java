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
        addCopyConstructor();
        try {
            for (ImmutableProp prop : type.getProps().values()) {
                if (prop.isList() == isTableEx) {
                    addProperty(prop, false);
                    addProperty(prop, true);
                }
            }
            addAsTableEx();
            addDisableJoin();
            addWeakJoin(false);
            addWeakJoin(true);
            return typeBuilder.build();
        } finally {
            typeBuilder = oldTypeBuilder;
        }
    }

    private void addInstanceField() {
        ClassName className = isTableEx ? type.getTableExClassName() : type.getTableClassName();
        FieldSpec.Builder builder = FieldSpec
                .builder(className, "$", Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL);
        if (isTableEx) {
            builder.initializer("new $T($T.$L, null)", className, type.getTableClassName(), "$");
        } else {
            builder.initializer("new $T()", className);
        }
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
                .addStatement("super(table)");
        typeBuilder.addMethod(builder.build());
    }

    private void addCopyConstructor() {
        MethodSpec.Builder builder = MethodSpec
                .constructorBuilder()
                .addModifiers(Modifier.PROTECTED)
                .addParameter(type.getTableClassName(), "base")
                .addParameter(String.class, "joinDisabledReason")
                .addStatement("super(base, joinDisabledReason)");
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
        ClassName tableExClassName = type.getTableExClassName();
        MethodSpec.Builder builder = MethodSpec
                .methodBuilder("asTableEx")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .returns(tableExClassName);
        if (isTableEx) {
            builder.addStatement("return this");
        } else {
            builder.addStatement("return new $T(this, null)", tableExClassName);
        }
        typeBuilder.addMethod(builder.build());
    }

    private void addDisableJoin() {
        ClassName selfClassName = isTableEx ? type.getTableExClassName() : type.getTableClassName();
        MethodSpec.Builder builder = MethodSpec
                .methodBuilder("__disableJoin")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .returns(selfClassName)
                .addParameter(String.class, "reason")
                .addStatement("return new $T(this, reason)", selfClassName);
        typeBuilder.addMethod(builder.build());
    }

    private void addWeakJoin(boolean withJoinType) {
        if (!isTableEx) {
            return;
        }
        MethodSpec.Builder builder = MethodSpec
                .methodBuilder("weakJoin")
                .addModifiers(Modifier.PUBLIC)
                .addTypeVariable(
                        TypeVariableName.get(
                                "TT",
                                ParameterizedTypeName.get(
                                        TABLE_CLASS_NAME,
                                        WildcardTypeName.subtypeOf(TypeName.OBJECT)
                                )
                        )
                )
                .addTypeVariable(
                        TypeVariableName.get(
                                "WJ",
                                ParameterizedTypeName.get(
                                        Constants.WEAK_JOIN_CLASS_NAME,
                                        type.getTableClassName(),
                                        TypeVariableName.get("TT")
                                )
                        )
                )
                .returns(TypeVariableName.get("TT"))
                .addParameter(
                        ParameterSpec
                                .builder(
                                        ParameterizedTypeName.get(
                                                CLASS_CLASS_NAME,
                                                TypeVariableName.get("WJ")
                                        ),
                                        "weakJoinType"
                                )
                                .build()
                );
        if (withJoinType) {
            builder.addParameter(
                    ParameterSpec
                            .builder(JOIN_TYPE_CLASS_NAME, "joinType")
                            .build()
            );
            builder.addAnnotation(
                    AnnotationSpec
                            .builder(SuppressWarnings.class)
                            .addMember("value", "\"unchecked\"")
                            .build()
            );
        }
        if (withJoinType) {
            builder
                    .addStatement("__beforeJoin()")
                    .beginControlFlow("if (raw != null)")
                    .addStatement(
                            "return (TT)$T.wrap(raw.weakJoinImplementor(weakJoinType, joinType))",
                            TABLE_PROXIES_CLASS_NAME
                    )
                    .endControlFlow()
                    .addStatement(
                            "return (TT)$T.fluent(joinOperation(weakJoinType, joinType))",
                            TABLE_PROXIES_CLASS_NAME
                    );
        } else {
            builder.addStatement("return weakJoin(weakJoinType, JoinType.INNER)");
        }
        typeBuilder.addMethod(builder.build());
    }
}
