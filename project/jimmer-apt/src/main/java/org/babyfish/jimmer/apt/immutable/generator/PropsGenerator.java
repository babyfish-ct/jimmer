package org.babyfish.jimmer.apt.immutable.generator;

import com.squareup.javapoet.*;
import org.babyfish.jimmer.apt.GeneratorException;
import org.babyfish.jimmer.apt.Context;
import org.babyfish.jimmer.apt.immutable.meta.ImmutableProp;
import org.babyfish.jimmer.apt.immutable.meta.ImmutableType;
import org.babyfish.jimmer.impl.util.StringUtil;

import javax.lang.model.element.Modifier;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

import java.io.IOException;

import static org.babyfish.jimmer.apt.util.GeneratedAnnotation.generatedAnnotation;

public class PropsGenerator {

    private final Context context;

    private final ImmutableType type;

    private TypeSpec.Builder typeBuilder;

    public PropsGenerator(
            Context context,
            ImmutableType type
    ) {
        this.context = context;
        this.type = type;
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
                    .writeTo(context.getFiler());
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
                .interfaceBuilder(type.getName() + "Props")
                .addAnnotation(generatedAnnotation(type))
                .addModifiers(Modifier.PUBLIC);
        if (type.isEntity() || type.isMappedSuperClass()) {
            typeBuilder.addAnnotation(
                    AnnotationSpec
                            .builder(Constants.PROPS_FOR_CLASS_NAME)
                            .addMember("value", "$T.class", type.getClassName())
                            .build()
            );
        }
        if (type.getSuperTypes().isEmpty()) {
            if (type.isEntity() || type.isMappedSuperClass()) {
                typeBuilder.addSuperinterface(Constants.PROPS_CLASS_NAME);
            }
        } else {
            for (ImmutableType superType : type.getSuperTypes()) {
                typeBuilder.addSuperinterface(
                        superType.getPropsClassName()
                );
            }
        }
        if (type.isEntity()) {
            typeBuilder.addSuperinterface(
                    ParameterizedTypeName.get(
                            Constants.SELECTION_CLASS_NAME,
                            type.getClassName()
                    )
            );
        }
        try {
            for (ImmutableProp prop : type.getProps().values()) {
                addStaticProp(prop);
            }
            if (type.isEntity() || type.isMappedSuperClass()) {
                for (ImmutableProp prop : type.getDeclaredProps().values()) {
                    if (prop.isDsl(false)) {
                        addProp(prop, false);
                        addProp(prop, true);
                    }
                    addExists(prop);
                    addIdProp(prop, type.getIdPropName(prop.getName()));
                }
            }
            return typeBuilder.build();
        } finally {
            typeBuilder = null;
        }
    }

    private void addStaticProp(ImmutableProp prop) {
        ClassName rawClassName;
        String action;
        if (prop.isList()) {
            rawClassName = prop.isAssociation(false) ?
                    Constants.REFERENCE_LIST_CLASS_NAME :
                    Constants.SCALAR_LIST_CLASS_NAME;
            action = prop.isAssociation(false) ?
                    "referenceList" :
                    "scalarList";
        } else {
            rawClassName = prop.isAssociation(false) ?
                    Constants.REFERENCE_CLASS_NAME :
                    Constants.SCALAR_CLASS_NAME;
            action = prop.isAssociation(false) ?
                    "reference" :
                    "scalar";
        }
        String fieldName = Strings.upper(prop.getName());
        FieldSpec.Builder builder = FieldSpec
                .builder(
                        ParameterizedTypeName.get(
                                rawClassName,
                                type.getClassName(),
                                prop.getElementTypeName().box()
                        ),
                        fieldName,
                        Modifier.PUBLIC,
                        Modifier.STATIC,
                        Modifier.FINAL
                )
                .initializer(
                        "\n    $T.$L($T.get($T.class).getProp($S))",
                        Constants.TYPED_PROP_CLASS_NAME,
                        action,
                        Constants.RUNTIME_TYPE_CLASS_NAME,
                        type.getClassName(),
                        prop.getName()
                );
        typeBuilder.addField(builder.build());
    }

    private void addProp(
            ImmutableProp prop,
            boolean withJoinType
    ) {
        MethodSpec propertyMethod = property(
                context,
                false,
                prop,
                withJoinType,
                false
        );
        if (propertyMethod != null) {
            typeBuilder.addMethod(propertyMethod);
        }
    }

    private void addExists(ImmutableProp prop) {
        MethodSpec existsMethod = exists(prop, false);
        if (existsMethod != null) {
            typeBuilder.addMethod(existsMethod);
        }
    }

    private void addIdProp(ImmutableProp prop, String idPropName) {
        MethodSpec method = associatedIdProperty(
                context,
                false,
                prop,
                idPropName,
                false
        );
        if (method != null) {
            typeBuilder.addMethod(method);
        }
    }

    static MethodSpec property(
            Context context,
            boolean isTableEx,
            ImmutableProp prop,
            boolean withJoinType,
            boolean withImplementation
    ) {
        return property(context, isTableEx, prop, withJoinType, withImplementation, false);
    }

    static MethodSpec property(
            Context context,
            boolean isTableEx,
            ImmutableProp prop,
            boolean withJoinType,
            boolean withImplementation,
            boolean ignoreOverride
    ) {
        if (withJoinType && !prop.isAssociation(true)) {
            return null;
        }
        TypeName returnType = returnTypeName(context, isTableEx, prop);
        MethodSpec.Builder builder = MethodSpec
                .methodBuilder(prop.getName())
                .addModifiers(Modifier.PUBLIC)
                .returns(returnType);
        if (withImplementation) {
            if (!isTableEx && !ignoreOverride) {
                builder.addAnnotation(Override.class);
            }
        } else {
            builder.addModifiers(Modifier.ABSTRACT);
        }
        if (withJoinType) {
            builder.addParameter(Constants.JOIN_TYPE_CLASS_NAME, "joinType");
        }
        if (withImplementation) {
            if (prop.isAssociation(true)) {
                builder.addStatement("__beforeJoin()");
                if (withJoinType) {
                    builder
                            .beginControlFlow("if (raw != null)")
                            .addStatement(
                                    "return new $T(raw.joinImplementor($T.$L.unwrap(), joinType))",
                                    returnType,
                                    prop.getDeclaringType().getPropsClassName(),
                                    Strings.upper(prop.getName())
                            )
                            .endControlFlow()
                            .addStatement(
                                    "return new $T(joinOperation($T.$L.unwrap(), joinType))",
                                    returnType,
                                    prop.getDeclaringType().getPropsClassName(),
                                    Strings.upper(prop.getName())
                            );
                } else {
                    builder
                            .beginControlFlow("if (raw != null)")
                            .addStatement(
                                    "return new $T(raw.joinImplementor($T.$L.unwrap()))",
                                    returnType,
                                    prop.getDeclaringType().getPropsClassName(),
                                    Strings.upper(prop.getName())
                            )
                            .endControlFlow()
                            .addStatement(
                                    "return new $T(joinOperation($T.$L.unwrap()))",
                                    returnType,
                                    prop.getDeclaringType().getPropsClassName(),
                                    Strings.upper(prop.getName())
                            );
                }
            } else if (prop.isAssociation(false)) {
                builder.addStatement(
                        "return new $T(__get($T.$L.unwrap()))",
                        returnType,
                        prop.getDeclaringType().getPropsClassName(),
                        Strings.upper(prop.getName())
                );
            } else {
                builder.addStatement(
                        "return __get($T.$L.unwrap())",
                        prop.getDeclaringType().getPropsClassName(),
                        Strings.upper(prop.getName())
                );
            }
        }
        return builder.build();
    }

    static TypeName returnTypeName(
            Context context,
            boolean isTableEx,
            ImmutableProp prop
    ) {
        TypeName returnType;
        if (prop.isAssociation(true)) {
            if (prop.isRemote()) {
                returnType = context
                        .getImmutableType(prop.getElementType())
                        .getRemoteTableClassName();
            } else if (isTableEx) {
                returnType = context
                        .getImmutableType(prop.getElementType())
                        .getTableExClassName();
            } else {
                returnType = context
                        .getImmutableType(prop.getElementType())
                        .getTableClassName();
            }
        } else if (prop.isAssociation(false)) {
            ClassName className = (ClassName)prop.getTypeName();
            returnType = ClassName.get(
                    className.packageName(),
                    className.simpleName() + ImmutableType.PROP_EXPRESSION_SUFFIX
            );
        } else {
            returnType = propExpressionTypeName(prop.getReturnType(), context);
        }
        return returnType;
    }

    static MethodSpec associatedIdProperty(
            Context context,
            boolean isTableEx,
            ImmutableProp prop,
            String idPropName,
            boolean withImplementation
    ) {
        if (idPropName == null) {
            return null;
        }
        if (prop.isTransient() || !prop.isAssociation(true) || prop.isList() != isTableEx) {
            return null;
        }
        MethodSpec.Builder builder = MethodSpec
                .methodBuilder(idPropName)
                .addModifiers(Modifier.PUBLIC)
                .returns(
                        propExpressionTypeName(prop.getTargetType().getIdProp().getReturnType(), context)
                );
        if (withImplementation) {
            if (!isTableEx) {
                builder.addAnnotation(Override.class);
            }
        } else {
            builder.addModifiers(Modifier.ABSTRACT);
        }
        if (withImplementation) {
            builder.addStatement(
                    "return __getAssociatedId($T.$L.unwrap())",
                    prop.getDeclaringType().getPropsClassName(),
                    Strings.upper(prop.getName())
            );
        }
        return builder.build();
    }

    private static TypeName propExpressionTypeName(TypeMirror typeMirror, Context context) {
        TypeName typeName = TypeName.get(typeMirror);
        if (typeMirror.getKind().isPrimitive() && typeMirror.getKind() != TypeKind.BOOLEAN) {
            return ParameterizedTypeName.get(
                    Constants.PROP_NUMERIC_EXPRESSION_CLASS_NAME,
                    typeName.box()
            );
        }
        if (typeName.equals(Constants.STRING_CLASS_NAME)) {
            return Constants.PROP_STRING_EXPRESSION_CLASS_NAME;
        } else if (context.isNumber(typeMirror)) {
            return ParameterizedTypeName.get(
                    Constants.PROP_NUMERIC_EXPRESSION_CLASS_NAME,
                    typeName.box()
            );
        } else if (context.isDate(typeMirror)) {
            return ParameterizedTypeName.get(
                    Constants.PROP_DATE_EXPRESSION_CLASS_NAME,
                    typeName.box()
            );
        } else if (context.isTemporal(typeMirror)) {
            return ParameterizedTypeName.get(
                    Constants.PROP_TEMPORAL_EXPRESSION_CLASS_NAME,
                    typeName.box()
            );
        } else if (context.isComparable(typeMirror)) {
            return ParameterizedTypeName.get(
                    Constants.PROP_COMPARABLE_EXPRESSION_CLASS_NAME,
                    typeName.box()
            );
        } else {
            return ParameterizedTypeName.get(
                    Constants.PROP_EXPRESSION_CLASS_NAME,
                    typeName.box()
            );
        }
    }

    static MethodSpec exists(
            ImmutableProp prop,
            boolean withImplementation
    ) {
        if (!prop.isAssociation(true) || !prop.isList()) {
            return null;
        }
        MethodSpec.Builder builder = MethodSpec
                .methodBuilder(prop.getName())
                .addModifiers(Modifier.PUBLIC)
                .addParameter(
                        ParameterizedTypeName.get(
                                Constants.FUNCTION_CLASS_NAME,
                                prop.getTargetType().getTableExClassName(),
                                Constants.PREDICATE_CLASS_NAME
                        ),
                        "block"
                )
                .returns(Constants.PREDICATE_CLASS_NAME);
        if (withImplementation) {
            builder.addAnnotation(Override.class);
        } else {
            builder.addModifiers(Modifier.ABSTRACT);
        }
        if (withImplementation) {
            builder.addStatement(
                    "return exists($T.$L.unwrap(), block)",
                    prop.getDeclaringType().getPropsClassName(),
                    StringUtil.snake(prop.getName(), StringUtil.SnakeCase.UPPER)
            );
        }
        return builder.build();
    }
}
