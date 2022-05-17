package org.babyfish.jimmer.apt.generator;

import com.squareup.javapoet.*;
import org.babyfish.jimmer.apt.GeneratorException;
import org.babyfish.jimmer.apt.TypeUtils;
import org.babyfish.jimmer.apt.meta.ImmutableProp;
import org.babyfish.jimmer.apt.meta.ImmutableType;

import javax.annotation.processing.Filer;
import javax.lang.model.element.Modifier;
import javax.persistence.Id;
import java.io.IOException;

public class FetcherGenerator {

    private TypeUtils typeUtils;

    private ImmutableType type;

    private Filer filer;

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
                                type.getClassName()
                        )
                );

        TypeSpec.Builder oldBuilder = typeBuilder;
        typeBuilder = builder;
        try {
            add$();
            addConstructor();
            addWrapConstructor();
            for (ImmutableProp prop : type.getProps().values()) {
                if (prop.getAnnotation(Id.class) == null) {
                    addProp(prop);
                    addBooleanProp(prop);
                    if (prop.isAssociation()) {
                        addAssociationProp(prop);
                        addAssociationPropWithLoader(prop);
                    }
                }
            }
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

    private void addWrapConstructor() {
        MethodSpec.Builder builder = MethodSpec
                .constructorBuilder()
                .addModifiers(Modifier.PRIVATE)
                .addParameter(
                        ParameterizedTypeName.get(
                                Constants.FETCHER_CLASS_NAME,
                                type.getClassName()
                        ),
                        "raw"
                )
                .addStatement("super(raw)");
        typeBuilder.addMethod(builder.build());
    }

    private void addProp(ImmutableProp prop) {
        MethodSpec.Builder builder = MethodSpec
                .methodBuilder(prop.getName())
                .returns(type.getFetcherClassName())
                .addStatement(
                        "return new $T(raw.add($S))",
                        type.getFetcherClassName(),
                        prop.getName()
                );
        typeBuilder.addMethod(builder.build());
    }

    private void addBooleanProp(ImmutableProp prop) {
        MethodSpec.Builder builder = MethodSpec
                .methodBuilder(prop.getName())
                .addParameter(boolean.class, "enabled")
                .returns(type.getFetcherClassName())
                .addStatement(
                        "return new $T(enabled ? raw.add($S) : raw.remove($S))",
                        type.getFetcherClassName(),
                        prop.getName(),
                        prop.getName()
                );
        typeBuilder.addMethod(builder.build());
    }

    private void addAssociationProp(ImmutableProp prop) {
        MethodSpec.Builder builder = MethodSpec
                .methodBuilder(prop.getName())
                .addParameter(
                        ParameterizedTypeName.get(
                                Constants.FETCHER_CLASS_NAME,
                                prop.getElementTypeName()
                        ),
                        "childFetcher"
                )
                .returns(type.getFetcherClassName())
                .addStatement(
                        "return new $T(raw.add($S, childFetcher))",
                        type.getFetcherClassName(),
                        prop.getName()
                );
        typeBuilder.addMethod(builder.build());
    }

    private void addAssociationPropWithLoader(ImmutableProp prop) {
        boolean recursive = typeUtils.isSubType(
                prop.getElementType(),
                type.getTypeElement().asType()
        );
        TypeName loaderClassName;
        if (recursive && prop.isList()) {
            loaderClassName = Constants.RECURSIVE_LIST_LOADER_CLASS_NAME;
        } else if (recursive) {
            loaderClassName = Constants.RECURSIVE_LOADER_CLASS_NAME;
        } else if (prop.isList()) {
            loaderClassName = Constants.LIST_LOADER_CLASS_NAME;
        } else {
            loaderClassName = Constants.LOADER_CLASS_NAME;
        }
        MethodSpec.Builder builder = MethodSpec
                .methodBuilder(prop.getName())
                .addParameter(
                        ParameterizedTypeName.get(
                                Constants.CONSUMER_CLASS_NAME,
                                loaderClassName
                        ),
                        "loader"
                )
                .addParameter(
                        ParameterizedTypeName.get(
                                Constants.FETCHER_CLASS_NAME,
                                prop.getElementTypeName()
                        ),
                        "childFetcher"
                )
                .returns(type.getFetcherClassName())
                .addStatement(
                        "return new $T(raw.add($S, loader, childFetcher))",
                        type.getFetcherClassName(),
                        prop.getName()
                );
        typeBuilder.addMethod(builder.build());
    }
}
