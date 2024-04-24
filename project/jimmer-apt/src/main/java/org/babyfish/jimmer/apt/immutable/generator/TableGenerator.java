package org.babyfish.jimmer.apt.immutable.generator;

import com.squareup.javapoet.*;
import org.babyfish.jimmer.apt.GeneratorException;
import org.babyfish.jimmer.apt.Context;
import org.babyfish.jimmer.apt.immutable.meta.ImmutableProp;
import org.babyfish.jimmer.apt.immutable.meta.ImmutableType;
import org.babyfish.jimmer.impl.util.StringUtil;

import javax.annotation.processing.Filer;
import javax.lang.model.element.Modifier;
import java.io.IOException;

import static org.babyfish.jimmer.apt.util.GeneratedAnnotation.generatedAnnotation;

public class TableGenerator {

    private final Context context;

    private final ImmutableType type;
    
    private final boolean isTableEx;

    private final Filer filer;

    private TypeSpec.Builder typeBuilder;

    public TableGenerator(
            Context context,
            ImmutableType type,
            boolean isTableEx,
            Filer filer
    ) {
        this.context = context;
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
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(generatedAnnotation(type))
        ;
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
                if (prop.isDsl(isTableEx)) {
                    addProperty(prop, false);
                    addProperty(prop, true);
                }
                addExists(prop);
                addIdProperty(prop, type.getIdPropName(prop.getName()));
            }
            addAsTableEx();
            addDisableJoin();
            addWeakJoin(false);
            addWeakJoin(true);
            addRemote();
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
                                Constants.DELAYED_OPERATION_CLASS_NAME,
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
                Constants.TABLE_IMPLEMENTOR_CLASS_NAME,
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
        MethodSpec propertyMethod = PropsGenerator.property(
                context,
                isTableEx,
                prop,
                withJoinType,
                true
        );
        if (propertyMethod != null) {
            typeBuilder.addMethod(propertyMethod);
        }
    }

    private void addExists(ImmutableProp prop) {
        MethodSpec existsMethod = PropsGenerator.exists(prop, true);
        if (existsMethod != null) {
            typeBuilder.addMethod(existsMethod);
        }
    }

    private void addIdProperty(ImmutableProp prop, String idPropName) {
        MethodSpec method = PropsGenerator.associatedIdProperty(
                context,
                isTableEx,
                prop,
                idPropName,
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
                                        Constants.TABLE_CLASS_NAME,
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
                                                Constants.CLASS_CLASS_NAME,
                                                TypeVariableName.get("WJ")
                                        ),
                                        "weakJoinType"
                                )
                                .build()
                );
        if (withJoinType) {
            builder.addParameter(
                    ParameterSpec
                            .builder(Constants.JOIN_TYPE_CLASS_NAME, "joinType")
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
                            Constants.TABLE_PROXIES_CLASS_NAME
                    )
                    .endControlFlow()
                    .addStatement(
                            "return (TT)$T.fluent(joinOperation(weakJoinType, joinType))",
                            Constants.TABLE_PROXIES_CLASS_NAME
                    );
        } else {
            builder.addStatement("return weakJoin(weakJoinType, JoinType.INNER)");
        }
        typeBuilder.addMethod(builder.build());
    }

    private void addRemote() {
        if (isTableEx) {
            return;
        }
        TypeSpec.Builder tmpTypeBuilder = typeBuilder;
        typeBuilder = TypeSpec
                .classBuilder("Remote")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addAnnotation(generatedAnnotation(type))
                .superclass(
                        ParameterizedTypeName.get(
                                Constants.ABSTRACT_TYPED_TABLE_CLASS_NAME,
                                type.getClassName()
                        )
                );
        addRemoteConstructor();
        addRemoteIdProp();
        addRemoteAsTableEx();
        addRemoteDisableJoin();
        tmpTypeBuilder.addType(typeBuilder.build());
        typeBuilder = tmpTypeBuilder;
    }

    private void addRemoteConstructor() {
        typeBuilder.addMethod(
                MethodSpec
                        .constructorBuilder()
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(
                                Constants.DELAYED_OPERATION_CLASS_NAME,
                                "delayedOperation"
                        )
                        .addStatement("super($T.class, delayedOperation)", type.getClassName())
                        .build()
        );
        typeBuilder.addMethod(
                MethodSpec
                        .constructorBuilder()
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(
                                ParameterizedTypeName.get(
                                        Constants.TABLE_IMPLEMENTOR_CLASS_NAME,
                                        type.getClassName()
                                ),
                                "table"
                        )
                        .addStatement("super(table)", type.getClassName())
                        .build()
        );
    }

    private void addRemoteIdProp() {
        MethodSpec.Builder builder = MethodSpec
                .methodBuilder(type.getIdProp().getName())
                .addModifiers(Modifier.PUBLIC)
                .returns(PropsGenerator.returnTypeName(context, false, type.getIdProp()))
                .addStatement(
                        "return __get($T.$L.unwrap())",
                        type.getPropsClassName(),
                        StringUtil.snake(type.getIdProp().getName(), StringUtil.SnakeCase.UPPER)
                );
        typeBuilder.addMethod(builder.build());
    }

    private void addRemoteAsTableEx() {
        MethodSpec.Builder builder = MethodSpec
                .methodBuilder("asTableEx")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .addAnnotation(Deprecated.class)
                .returns(
                        ParameterizedTypeName.get(
                                Constants.TABLE_EX_CLASS_NAME,
                                type.getClassName()
                        )
                )
                .addStatement("throw new UnsupportedOperationException()");
        typeBuilder.addMethod(builder.build());
    }

    private void addRemoteDisableJoin() {
        MethodSpec.Builder builder = MethodSpec
                .methodBuilder("__disableJoin")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .addParameter(Constants.STRING_CLASS_NAME, "reason")
                .returns(type.getRemoteTableClassName())
                .addStatement("return this");
        typeBuilder.addMethod(builder.build());
    }
}
