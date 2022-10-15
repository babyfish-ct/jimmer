package org.babyfish.jimmer.apt.generator;

import com.squareup.javapoet.*;
import org.babyfish.jimmer.apt.GeneratorException;
import org.babyfish.jimmer.apt.TypeUtils;
import org.babyfish.jimmer.apt.meta.ImmutableProp;
import org.babyfish.jimmer.apt.meta.ImmutableType;
import org.babyfish.jimmer.meta.TargetLevel;
import org.babyfish.jimmer.sql.JoinType;

import javax.annotation.processing.Filer;
import javax.lang.model.element.Modifier;

import java.io.IOException;

import static org.babyfish.jimmer.apt.generator.Constants.PROPS_FOR_CLASS_NAME;
import static org.babyfish.jimmer.apt.generator.Constants.PROP_EXPRESSION_CLASS_NAME;

public class PropsGenerator {

    private final TypeUtils typeUtils;

    private final ImmutableType type;

    private final Filer filer;

    private TypeSpec.Builder typeBuilder;

    public PropsGenerator(
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
                .interfaceBuilder(type.getName() + "Props")
                .addModifiers(Modifier.PUBLIC);
        if (type.isEntity() || type.isMappedSuperClass()) {
            typeBuilder.addAnnotation(
                    AnnotationSpec
                            .builder(PROPS_FOR_CLASS_NAME)
                            .addMember("value", "$T.class", type.getClassName())
                            .build()
            );
        }
        if (type.getSuperType() != null) {
            typeBuilder.addSuperinterface(
                    type.getSuperType().getPropsClassName()
            );
        } else if (type.isEntity() || type.isMappedSuperClass()) {
            typeBuilder.addSuperinterface(Constants.PROPS_CLASS_NAME);
        }
        try {
            for (ImmutableProp prop : type.getDeclaredProps().values()) {
                addStaticProp(prop, false);
            }
            ImmutableType superType = type.getSuperType();
            if (type.isEntity() && superType != null && superType.isMappedSuperClass()) {
                for (ImmutableProp prop : superType.getProps().values()) {
                    if (prop.isAssociation()) {
                        addStaticProp(prop, true);
                    }
                }
            }
            if (type.isEntity() || type.isMappedSuperClass()) {
                for (ImmutableProp prop : type.getDeclaredProps().values()) {
                    if (!prop.isList()) {
                        addProp(prop, false);
                        addProp(prop, true);
                    }
                }
            }
            return typeBuilder.build();
        } finally {
            typeBuilder = null;
        }
    }

    private void addStaticProp(ImmutableProp prop, boolean override) {
        ClassName rawClassName;
        String action;
        if (prop.isList()) {
            rawClassName = prop.isAssociation() ?
                    Constants.REFERENCE_LIST_CLASS_NAME :
                    Constants.SCALAR_LIST_CLASS_NAME;
            action = prop.isAssociation() ?
                    "referenceList" :
                    "scalarList";
        } else {
            rawClassName = prop.isAssociation() ?
                    Constants.REFERENCE_CLASS_NAME :
                    Constants.SCALAR_CLASS_NAME;
            action = prop.isAssociation() ?
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
                );
        if (override) {
            builder.initializer(
                    "\n    $T.$L($T.source($T.$L.unwrap(), $T.class))",
                    Constants.TYPED_PROP_CLASS_NAME,
                    action,
                    Constants.REDIRECTED_PROP_CLASS_NAME,
                    type.getSuperType().getPropsClassName(),
                    fieldName,
                    type.getClassName()
            );
        } else {
            builder.initializer(
                    "\n    $T.$L($T.get($T.class).getProp($L))",
                    Constants.TYPED_PROP_CLASS_NAME,
                    action,
                    Constants.RUNTIME_TYPE_CLASS_NAME,
                    type.getClassName(),
                    Integer.toString(prop.getId())
            );
        }
        typeBuilder.addField(builder.build());
    }

    private void addProp(
            ImmutableProp prop,
            boolean withJoinType
    ) {
        MethodSpec method = PropsGenerator.property(
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
