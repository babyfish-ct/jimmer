package org.babyfish.jimmer.apt.immutable.generator;

import com.squareup.javapoet.*;
import org.babyfish.jimmer.apt.GeneratorException;
import org.babyfish.jimmer.apt.Context;
import org.babyfish.jimmer.apt.immutable.meta.ImmutableProp;
import org.babyfish.jimmer.apt.immutable.meta.ImmutableType;
import org.babyfish.jimmer.client.meta.Doc;
import org.babyfish.jimmer.impl.util.StringUtil;
import org.babyfish.jimmer.lang.NewChain;
import org.babyfish.jimmer.sql.*;

import javax.lang.model.element.Modifier;
import java.io.IOException;

import static org.babyfish.jimmer.apt.util.GeneratedAnnotation.generatedAnnotation;

public class FetcherGenerator {

    private final Context context;

    private final ImmutableType type;

    private TypeSpec.Builder typeBuilder;

    public FetcherGenerator(
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
                            generateFetcher()
                    )
                    .indent("    ")
                    .build()
                    .writeTo(context.getFiler());
        } catch (IOException ex) {
            throw new GeneratorException(
                    String.format(
                            "Cannot generate fetcher class for '%s'",
                            type.getQualifiedName()
                    ),
                    ex
            );
        }
    }

    private TypeSpec generateFetcher() {

        TypeSpec.Builder builder = TypeSpec
                .classBuilder(type.getFetcherClassName().simpleName())
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(generatedAnnotation(type))
                .superclass(
                        ParameterizedTypeName.get(
                                Constants.ABSTRACT_TYPED_FETCHER_CLASS_NAME,
                                type.getClassName(),
                                type.getFetcherClassName()
                        )
                );

        TypeSpec.Builder oldBuilder = typeBuilder;
        typeBuilder = builder;
        try {
            add$();
            add$from();
            addConstructor();
            for (ImmutableProp prop : type.getProps().values()) {
                if (prop.isId()) {
                    continue;
                }
                if (isFetchProp(prop)) {
                    addProp(prop);
                    addPropByBoolean(prop);
                    if (prop.isAssociation(true)) {
                        addPropWithChild(prop);
                        if (!prop.isList()) {
                            addPropByIdOnlyFetchType(prop);
                        }
                        if (!prop.isRemote()) {
                            addAssociationPropByFieldConfig(prop);
                            if (!prop.isList()) {
                                addPropWithReferenceFetchType(prop);
                            }
                            addRecursiveProp(prop, false);
                            addRecursiveProp(prop, true);
                        }
                    } else if (prop.getTargetType() != null && prop.getTargetType().isEmbeddable()) {
                        addPropWithChild(prop);
                    }
                }
            }
            addConstructorByNegativeAndReferenceType();
            addConstructorByFieldConfig();
            addCreatorByBoolean();
            addCreatorByFieldConfig();
        } finally {
            typeBuilder = oldBuilder;
        }
        return builder.build();
    }

    private void add$() {
        FieldSpec.Builder builder = FieldSpec
                .builder(type.getFetcherClassName(), "$")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                .initializer("new $T(null)", type.getFetcherClassName());
        typeBuilder.addField(builder.build());
    }

    private void add$from() {
        MethodSpec.Builder builder = MethodSpec
                .methodBuilder("$from")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addParameter(
                        ParameterizedTypeName.get(
                                Constants.FETCHER_CLASS_NAME,
                                type.getClassName()
                        ),
                        "base"
                )
                .returns(type.getFetcherClassName())
                .addCode("return base instanceof $T ? \n", type.getFetcherClassName())
                .addCode("\t($T)base : \n", type.getFetcherClassName())
                .addCode(
                        "\tnew $T(($T)base);\n",
                        type.getFetcherClassName(),
                        ParameterizedTypeName.get(
                                Constants.FETCHER_IMPL_CLASS_NAME,
                                type.getClassName()
                        )
                );
        typeBuilder.addMethod(builder.build());
    }

    private void addConstructor() {
        MethodSpec.Builder builder = MethodSpec
                .constructorBuilder()
                .addModifiers(Modifier.PRIVATE)
                .addParameter(
                        ParameterizedTypeName.get(
                                Constants.FETCHER_IMPL_CLASS_NAME,
                                type.getClassName()
                        ),
                        "base"
                )
                .addStatement(
                        "super($T.class, base)",
                        type.getClassName()
                );
        typeBuilder.addMethod(builder.build());
    }

    private boolean isFetchProp(ImmutableProp prop) {
        if (prop.isTransient()) {
            return prop.hasTransientResolver();
        }
        return true;
    }

    private void addProp(ImmutableProp prop) {
        MethodSpec.Builder builder = MethodSpec
                .methodBuilder(prop.getName())
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(NewChain.class)
                .returns(type.getFetcherClassName())
                .addStatement(
                        "return add($S)",
                        prop.getName()
                );
        Doc doc = context.getDocMetadata().getDoc(prop.toElement());
        if (doc != null) {
            builder.addJavadoc("$L", doc.getValue());
        }
        typeBuilder.addMethod(builder.build());
    }

    private void addPropByBoolean(ImmutableProp prop) {
        MethodSpec.Builder builder = MethodSpec
                .methodBuilder(prop.getName())
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(NewChain.class)
                .addParameter(boolean.class, "enabled")
                .returns(type.getFetcherClassName())
                .addStatement(
                        "return enabled ? add($S) : remove($S)",
                        prop.getName(),
                        prop.getName()
                );
        Doc doc = context.getDocMetadata().getDoc(prop.toElement());
        if (doc != null) {
            builder.addJavadoc("$L", doc.getValue());
        }
        typeBuilder.addMethod(builder.build());
    }

    private void addPropWithChild(ImmutableProp prop) {
        MethodSpec.Builder builder = MethodSpec
                .methodBuilder(prop.getName())
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(NewChain.class)
                .addParameter(
                        ParameterizedTypeName.get(
                                Constants.FETCHER_CLASS_NAME,
                                prop.getElementTypeName()
                        ),
                        "childFetcher"
                )
                .returns(type.getFetcherClassName())
                .addStatement(
                        "return add($S, childFetcher)",
                        prop.getName()
                );
        typeBuilder.addMethod(builder.build());
    }

    private void addAssociationPropByFieldConfig(ImmutableProp prop) {
        ClassName fieldConfigClassName;
        if (prop.isList()) {
            fieldConfigClassName = Constants.LIST_FIELD_CONFIG_CLASS_NAME;
        } else if (prop.isAssociation(true)) {
            fieldConfigClassName = Constants.REFERENCE_FIELD_CONFIG_CLASS_NAME;
        } else {
            fieldConfigClassName = Constants.FIELD_CONFIG_CLASS_NAME;
        }
        MethodSpec.Builder builder = MethodSpec
                .methodBuilder(prop.getName())
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(NewChain.class)
                .addParameter(
                        ParameterizedTypeName.get(
                                Constants.FETCHER_CLASS_NAME,
                                prop.getElementTypeName()
                        ),
                        "childFetcher"
                )
                .addParameter(
                        ParameterizedTypeName.get(
                                Constants.CONSUMER_CLASS_NAME,
                                ParameterizedTypeName.get(
                                        fieldConfigClassName,
                                        prop.getElementTypeName(),
                                        context.getImmutableType(prop.getElementType()).getTableClassName()
                                )
                        ),
                        "fieldConfig"
                )
                .returns(type.getFetcherClassName())
                .addStatement(
                        "return add($S, childFetcher, fieldConfig)",
                        prop.getName()
                );
        typeBuilder.addMethod(builder.build());
    }

    private void addRecursiveProp(ImmutableProp prop, boolean withLambda) {
        if (!prop.isRecursive()) {
            return;
        }
        ClassName fieldConfigClassName;
        if (prop.isList()) {
            fieldConfigClassName = Constants.RECURSIVE_LIST_FIELD_CONFIG_CLASS_NAME;
        } else {
            fieldConfigClassName = Constants.RECURSIVE_REFERENCE_FIELD_CONFIG_CLASS_NAME;
        }
        MethodSpec.Builder builder = MethodSpec
                .methodBuilder(StringUtil.identifier("recursive", prop.getName()))
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(NewChain.class);
        if (withLambda) {
            builder.addParameter(
                    ParameterizedTypeName.get(
                            Constants.CONSUMER_CLASS_NAME,
                            ParameterizedTypeName.get(
                                    fieldConfigClassName,
                                    prop.getElementTypeName(),
                                    context.getImmutableType(prop.getElementType()).getTableClassName()
                            )
                    ),
                    "fieldConfig"
            );
        }
        builder
                .returns(type.getFetcherClassName())
                .addStatement(
                        "return addRecursion($S, $L)",
                        prop.getName(),
                        withLambda ? "fieldConfig" : "null"
                );
        typeBuilder.addMethod(builder.build());
    }

    private void addPropByIdOnlyFetchType(ImmutableProp prop) {
        ImmutableProp associationProp = prop.getIdViewBaseProp();
        if (associationProp == null) {
            associationProp = prop;
        }
        if (associationProp.isTransient() || !associationProp.isAssociation(true)) {
            return;
        }
        if (prop.isReverse() || prop.isList() || prop.getAnnotation(JoinTable.class) != null) {
            return;
        }
        MethodSpec.Builder builder = MethodSpec
                .methodBuilder(prop.getName())
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(NewChain.class)
                .addParameter(
                        Constants.ID_ONLY_FETCH_TYPE,
                        "idOnlyFetchType"
                )
                .returns(type.getFetcherClassName())
                .addStatement(
                        "return add($S, idOnlyFetchType)",
                        prop.getName()
                );
        typeBuilder.addMethod(builder.build());
    }

    private void addPropWithReferenceFetchType(ImmutableProp prop) {

        MethodSpec.Builder builder = MethodSpec
                .methodBuilder(prop.getName())
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(NewChain.class)
                .addParameter(Constants.REFERENCE_FETCH_TYPE_CLASS_NAME, "fetchType")
                .addParameter(
                        ParameterizedTypeName.get(
                                Constants.FETCHER_CLASS_NAME,
                                prop.getElementTypeName()
                        ),
                        "childFetcher"
                )
                .returns(type.getFetcherClassName())
                .addStatement(
                        "return $L(childFetcher, cfg -> cfg.fetchType(fetchType))",
                        prop.getName()
                );

        typeBuilder.addMethod(builder.build());
    }

    private void addConstructorByNegativeAndReferenceType() {
        MethodSpec.Builder builder = MethodSpec
                .constructorBuilder()
                .addModifiers(Modifier.PRIVATE)
                .addParameter(type.getFetcherClassName(), "prev")
                .addParameter(org.babyfish.jimmer.meta.ImmutableProp.class, "prop")
                .addParameter(boolean.class, "negative")
                .addParameter(Constants.ID_ONLY_FETCH_TYPE, "idOnlyFetchType")
                .addStatement("super(prev, prop, negative, idOnlyFetchType)");
        typeBuilder.addMethod(builder.build());
    }

    private void addConstructorByFieldConfig() {
        MethodSpec.Builder builder = MethodSpec
                .constructorBuilder()
                .addModifiers(Modifier.PRIVATE)
                .addParameter(type.getFetcherClassName(), "prev")
                .addParameter(org.babyfish.jimmer.meta.ImmutableProp.class, "prop")
                .addParameter(
                        ParameterizedTypeName.get(
                                Constants.FIELD_CONFIG_CLASS_NAME,
                                WildcardTypeName.subtypeOf(Object.class),
                                WildcardTypeName.subtypeOf(
                                        ParameterizedTypeName.get(
                                                Constants.TABLE_CLASS_NAME,
                                                WildcardTypeName.subtypeOf(Object.class)
                                        )
                                )
                        ),
                        "fieldConfig"
                )
                .addStatement("super(prev, prop, fieldConfig)");
        typeBuilder.addMethod(builder.build());
    }

    private void addCreatorByBoolean() {
        MethodSpec.Builder builder = MethodSpec
                .methodBuilder("createFetcher")
                .addModifiers(Modifier.PROTECTED)
                .addParameter(
                        org.babyfish.jimmer.meta.ImmutableProp.class,
                        "prop"
                )
                .addParameter(boolean.class, "negative")
                .addParameter(Constants.ID_ONLY_FETCH_TYPE, "idOnlyFetchType")
                .returns(type.getFetcherClassName())
                .addAnnotation(Override.class)
                .addStatement("return new $T(this, prop, negative, idOnlyFetchType)", type.getFetcherClassName());
        typeBuilder.addMethod(builder.build());
    }

    private void addCreatorByFieldConfig() {
        MethodSpec.Builder builder = MethodSpec
                .methodBuilder("createFetcher")
                .addModifiers(Modifier.PROTECTED)
                .addParameter(
                        org.babyfish.jimmer.meta.ImmutableProp.class,
                        "prop"
                )
                .addParameter(
                        ParameterizedTypeName.get(
                                Constants.FIELD_CONFIG_CLASS_NAME,
                                WildcardTypeName.subtypeOf(Object.class),
                                WildcardTypeName.subtypeOf(
                                        ParameterizedTypeName.get(
                                                Constants.TABLE_CLASS_NAME,
                                                WildcardTypeName.subtypeOf(Object.class)
                                        )
                                )
                        ),
                        "fieldConfig"
                )
                .returns(type.getFetcherClassName())
                .addAnnotation(Override.class)
                .addStatement("return new $T(this, prop, fieldConfig)", type.getFetcherClassName());
        typeBuilder.addMethod(builder.build());
    }
}
