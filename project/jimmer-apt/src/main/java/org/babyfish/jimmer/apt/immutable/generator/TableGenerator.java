package org.babyfish.jimmer.apt.immutable.generator;

import com.squareup.javapoet.*;
import org.babyfish.jimmer.apt.GeneratorException;
import org.babyfish.jimmer.apt.Context;
import org.babyfish.jimmer.apt.immutable.meta.ImmutableProp;
import org.babyfish.jimmer.apt.immutable.meta.ImmutableType;
import org.babyfish.jimmer.impl.util.StringUtil;

import javax.lang.model.element.Modifier;
import java.io.IOException;

import static org.babyfish.jimmer.apt.util.GeneratedAnnotation.generatedAnnotation;
import static org.babyfish.jimmer.apt.util.SuppressAnnotation.suppressAllAnnotation;

public class TableGenerator {

    private final Context context;

    private final ImmutableType type;
    
    private final boolean isTableEx;

    private TypeSpec.Builder typeBuilder;

    public TableGenerator(
            Context context,
            ImmutableType type,
            boolean isTableEx
    ) {
        this.context = context;
        this.type = type;
        this.isTableEx = isTableEx;
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
                    .writeTo(context.getFiler());
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
                            Constants.TABLE_EX_PROXY_CLASS_NAME,
                            type.getClassName(),
                            type.getTableClassName()
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
        addDisableJoinConstructor();
        addBaseTableOwnerConstructor();
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
            addBaseTableOwner();
            addWeakJoin(false);
            addWeakJoin(true);
            addLambdaWeakJoin(false);
            addLambdaWeakJoin(true);
            addBaseTableLambdaWeakJoin(false);
            addBaseTableLambdaWeakJoin(true);
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
            builder.initializer("new $T($T.$L, (String)null)", className, type.getTableClassName(), "$");
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

    private void addDisableJoinConstructor() {
        MethodSpec.Builder builder = MethodSpec
                .constructorBuilder()
                .addModifiers(Modifier.PROTECTED)
                .addParameter(type.getTableClassName(), "base")
                .addParameter(String.class, "joinDisabledReason")
                .addStatement("super(base, joinDisabledReason)");
        typeBuilder.addMethod(builder.build());
    }

    private void addBaseTableOwnerConstructor() {
        MethodSpec.Builder builder = MethodSpec
                .constructorBuilder()
                .addModifiers(Modifier.PROTECTED)
                .addParameter(type.getTableClassName(), "base")
                .addParameter(
                        Constants.BASE_TABLE_OWNER_CLASS_NAME,
                        "baseTableOwner"
                )
                .addStatement("super(base, baseTableOwner)");
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
            builder.addStatement("return new $T(this, (String)null)", tableExClassName);
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

    private void addBaseTableOwner() {
        ClassName selfClassName = isTableEx ? type.getTableExClassName() : type.getTableClassName();
        MethodSpec.Builder builder = MethodSpec
                .methodBuilder("__baseTableOwner")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .returns(selfClassName)
                .addParameter(
                        Constants.BASE_TABLE_OWNER_CLASS_NAME,
                        "baseTableOwner"
                )
                .addStatement("return new $T(this, baseTableOwner)", selfClassName);
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
            builder.addAnnotation(suppressAllAnnotation());
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

    private void addLambdaWeakJoin(boolean withJoinType) {
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
                .returns(TypeVariableName.get("TT"))
                .addParameter(
                        ParameterSpec
                                .builder(
                                        ParameterizedTypeName.get(
                                                Constants.CLASS_CLASS_NAME,
                                                TypeVariableName.get("TT")
                                        ),
                                        "targetTableType"
                                )
                                .build()
                );
        if (withJoinType) {
            builder.addParameter(
                    ParameterSpec
                            .builder(Constants.JOIN_TYPE_CLASS_NAME, "joinType")
                            .build()
            );
            builder.addAnnotation(suppressAllAnnotation());
        }
        builder.addParameter(
                ParameterSpec
                        .builder(
                                ParameterizedTypeName.get(
                                        Constants.WEAK_JOIN_CLASS_NAME,
                                        type.getTableClassName(),
                                        TypeVariableName.get("TT")
                                ),
                                "weakJoinLambda"
                        )
                        .build()
        );
        if (withJoinType) {
            builder
                    .addStatement("__beforeJoin()")
                    .beginControlFlow("if (raw != null)")
                    .addStatement(
                            "return (TT)$T.wrap(raw.weakJoinImplementor(targetTableType, joinType, weakJoinLambda))",
                            Constants.TABLE_PROXIES_CLASS_NAME
                    )
                    .endControlFlow()
                    .addStatement(
                            "return (TT)$T.fluent(joinOperation(targetTableType, joinType, weakJoinLambda))",
                            Constants.TABLE_PROXIES_CLASS_NAME
                    );
        } else {
            builder.addStatement("return weakJoin(targetTableType, JoinType.INNER, weakJoinLambda)");
        }
        typeBuilder.addMethod(builder.build());
    }

    private void addBaseTableLambdaWeakJoin(boolean withJoinType) {
        if (!isTableEx) {
            return;
        }
        MethodSpec.Builder builder = MethodSpec.methodBuilder("weakJoin")
                .addModifiers(Modifier.PUBLIC)
                .addTypeVariable(TypeVariableName.get("TT", Constants.BASE_TABLE_CLASS_NAME))
                .returns(TypeVariableName.get("TT"))
                .addParameter(TypeVariableName.get("TT"), "targetBaseTable");
        if (withJoinType) {
            builder.addParameter(Constants.JOIN_TYPE_CLASS_NAME, "joinType");
        }
        builder.addParameter(
                ParameterizedTypeName.get(
                        Constants.WEAK_JOIN_CLASS_NAME,
                        type.getTableClassName(),
                        TypeVariableName.get("TT")
                ),
                "weakJoinLambda"
        );
        if (!withJoinType) {
            builder.addStatement(
                    "return weakJoin(targetBaseTable, $T.INNER, weakJoinLambda)",
                    Constants.JOIN_TYPE_CLASS_NAME
            );
        } else {
            CodeBlock.Builder cb = CodeBlock.builder();
            cb.addStatement(
                    "$T lambda = $T.get(weakJoinLambda)",
                    Constants.WEAK_JOIN_LAMBDA_CLASS_NAME,
                    Constants.J_WEAK_JOIN_LAMBDA_FACTORY_CLASS_NAME
            );
            cb.add(
                    "$T handle = $T.of($>\n",
                    Constants.WEAK_JOIN_HANDLE_CLASS_NAME,
                    Constants.WEAK_JOIN_HANDLE_CLASS_NAME
            );
            cb.add("lambda,\n");
            cb.add("true,\n");
            cb.add("true,\n");
            cb.add(
                    "($T)($T) weakJoinLambda\n$<",
                    ParameterizedTypeName.get(
                            Constants.WEAK_JOIN_CLASS_NAME,
                            ParameterizedTypeName.get(
                                    Constants.TABLE_LIKE_NAME,
                                    WildcardTypeName.subtypeOf(TypeName.OBJECT)
                            ),
                            ParameterizedTypeName.get(
                                    Constants.TABLE_LIKE_NAME,
                                    WildcardTypeName.subtypeOf(TypeName.OBJECT)
                            )
                    ),
                    ParameterizedTypeName.get(
                            Constants.WEAK_JOIN_CLASS_NAME,
                            WildcardTypeName.subtypeOf(TypeName.OBJECT),
                            WildcardTypeName.subtypeOf(TypeName.OBJECT)
                    )
            );
            cb.addStatement(")");
            cb.addStatement(
                    "return ($T) $T.of(($T) targetBaseTable, this, handle, joinType)",
                    TypeVariableName.get("TT"),
                    Constants.BASE_TABLE_SYMBOLS_CLASS_NAME,
                    Constants.BASE_TABLE_SYMBOL_CLASS_NAME
            );
            builder.addCode(cb.build());
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
        addRemoteConstructors();
        addRemoteIdProp();
        addRemoteAsTableEx();
        addRemoteDisableJoin();
        addRemoteBaseTableOwner();
        tmpTypeBuilder.addType(typeBuilder.build());
        typeBuilder = tmpTypeBuilder;
    }

    private void addRemoteConstructors() {
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
        typeBuilder.addMethod(
                MethodSpec
                        .constructorBuilder()
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(
                                type.getRemoteTableClassName(),
                                "base"
                        )
                        .addParameter(
                                Constants.BASE_TABLE_OWNER_CLASS_NAME,
                                "baseTableOwner"
                        )
                        .addStatement("super(base, baseTableOwner)", type.getClassName())
                        .build()
        );
    }

    private void addRemoteIdProp() {
        TypeName returnType = PropsGenerator.returnTypeName(context, false, type.getIdProp());
        MethodSpec.Builder builder = MethodSpec
                .methodBuilder(type.getIdProp().getName())
                .addModifiers(Modifier.PUBLIC)
                .returns(returnType)
                .addStatement(
                        "return ($L)this.<$T>get($T.$L.unwrap())",
                        returnType,
                        type.getIdProp().getTypeName().box(),
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

    private void addRemoteBaseTableOwner() {
        MethodSpec.Builder builder = MethodSpec
                .methodBuilder("__baseTableOwner")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .addParameter(
                        Constants.BASE_TABLE_OWNER_CLASS_NAME,
                        "baseTableOwner"
                )
                .returns(type.getRemoteTableClassName())
                .addStatement("return new Remote(this, baseTableOwner)");
        typeBuilder.addMethod(builder.build());
    }
}
