package org.babyfish.jimmer.apt.generator;

import com.squareup.javapoet.*;
import org.babyfish.jimmer.apt.GeneratorException;
import org.babyfish.jimmer.apt.TypeUtils;
import org.babyfish.jimmer.apt.meta.ImmutableProp;
import org.babyfish.jimmer.apt.meta.ImmutableType;
import org.babyfish.jimmer.lang.NewChain;
import org.babyfish.jimmer.sql.Id;

import javax.annotation.processing.Filer;
import javax.lang.model.element.Modifier;
import java.io.IOException;

public class FetcherGenerator {

    private final TypeUtils typeUtils;

    private final ImmutableType type;

    private final Filer filer;

    private TypeSpec.Builder typeBuilder;

    public FetcherGenerator(
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
                            generateFetcher()
                    )
                    .indent("    ")
                    .build()
                    .writeTo(filer);
        } catch (IOException ex) {
            throw new GeneratorException(
                    String.format(
                            "Cannot generate fetcher class for '%s'",
                            type.getName()
                    ),
                    ex
            );
        }
    }

    private TypeSpec generateFetcher() {

        TypeSpec.Builder builder = TypeSpec
                .classBuilder(type.getFetcherClassName().simpleName())
                .addModifiers(Modifier.PUBLIC)
                .superclass(
                        ParameterizedTypeName.get(
                                Constants.ABSTRACT_TYPE_FETCHER_CLASS_NAME,
                                type.getClassName(),
                                type.getFetcherClassName()
                        )
                );

        TypeSpec.Builder oldBuilder = typeBuilder;
        typeBuilder = builder;
        try {
            add$();
            addConstructor();
            for (ImmutableProp prop : type.getProps().values()) {
                if (prop.getAnnotation(Id.class) == null) {
                    addProp(prop);
                    addPropByBoolean(prop);
                    if (prop.isAssociation(true)) {
                        addAssociationProp(prop);
                        if (!prop.isTransient()) {
                            addAssociationPropByFieldConfig(prop);
                        }
                    }
                }
            }
            addConstructorByBoolean();
            addConstructorByFieldConfig();
            addCreatorByBoolean();
            addCreatorByFieldConfig();
        } finally {
            typeBuilder = oldBuilder;
        }
        return builder.build();
    }

    public void add$() {
        FieldSpec.Builder builder = FieldSpec
                .builder(type.getFetcherClassName(), "$")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.STATIC)
                .initializer("new $T()", type.getFetcherClassName());
        typeBuilder.addField(builder.build());
    }

    private void addConstructor() {
        MethodSpec.Builder builder = MethodSpec
                .constructorBuilder()
                .addModifiers(Modifier.PRIVATE)
                .addStatement(
                        "super($T.class)",
                        type.getClassName()
                );
        typeBuilder.addMethod(builder.build());
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
        typeBuilder.addMethod(builder.build());
    }

    private void addAssociationProp(ImmutableProp prop) {
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
        boolean recursive = typeUtils.isSubType(
                prop.getElementType(),
                type.getTypeElement().asType()
        );
        ClassName fieldConfigClassName;
        if (recursive && prop.isList()) {
            fieldConfigClassName = Constants.RECURSIVE_LIST_FIELD_CONFIG_CLASS_NAME;
        } else if (recursive) {
            fieldConfigClassName = Constants.RECURSIVE_FIELD_CONFIG_CLASS_NAME;
        } else if (prop.isList()) {
            fieldConfigClassName = Constants.LIST_FIELD_CONFIG_CLASS_NAME;
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
                                        typeUtils.getImmutableType(prop.getElementType()).getTableClassName()
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

    private void addConstructorByBoolean() {
        MethodSpec.Builder builder = MethodSpec
                .constructorBuilder()
                .addModifiers(Modifier.PRIVATE)
                .addParameter(type.getFetcherClassName(), "prev")
                .addParameter(org.babyfish.jimmer.meta.ImmutableProp.class, "prop")
                .addParameter(boolean.class, "negative")
                .addStatement("super(prev, prop, negative)");
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
                .methodBuilder("createChildFetcher")
                .addModifiers(Modifier.PROTECTED)
                .addParameter(
                        org.babyfish.jimmer.meta.ImmutableProp.class,
                        "prop"
                )
                .addParameter(boolean.class, "negative")
                .returns(type.getFetcherClassName())
                .addAnnotation(Override.class)
                .addStatement("return new $T(this, prop, negative)", type.getFetcherClassName());
        typeBuilder.addMethod(builder.build());
    }

    private void addCreatorByFieldConfig() {
        MethodSpec.Builder builder = MethodSpec
                .methodBuilder("createChildFetcher")
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
