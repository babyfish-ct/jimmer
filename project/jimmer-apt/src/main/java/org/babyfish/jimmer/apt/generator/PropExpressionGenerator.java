package org.babyfish.jimmer.apt.generator;

import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import org.babyfish.jimmer.apt.GeneratorException;
import org.babyfish.jimmer.apt.TypeUtils;
import org.babyfish.jimmer.apt.meta.ImmutableProp;
import org.babyfish.jimmer.apt.meta.ImmutableType;

import javax.annotation.processing.Filer;
import javax.lang.model.element.Modifier;
import java.io.IOException;

public class PropExpressionGenerator {

    private final TypeUtils typeUtils;

    private final ImmutableType type;

    private final Filer filer;

    private TypeSpec.Builder typeBuilder;

    public PropExpressionGenerator(TypeUtils typeUtils, ImmutableType type, Filer filer) {
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
                            "Cannot generate embedded prop expression class for '%s'",
                            type.getName()
                    ),
                    ex
            );
        }
    }

    private TypeSpec generateImpl() {
        TypeSpec.Builder builder = TypeSpec
                .classBuilder(type.getPropExpressionClassName())
                .addModifiers(Modifier.PUBLIC)
                .superclass(
                        ParameterizedTypeName.get(
                                Constants.ABSTRACT_TYPED_EMBEDDED_PROP_EXPRESSION_CLASS_NAME,
                                type.getClassName()
                        )
                );
        typeBuilder = builder;
        try {
            addConstructor();
            addProps();
        } finally {
            typeBuilder = null;
        }
        return builder.build();
    }

    private void addConstructor() {
        MethodSpec.Builder builder = MethodSpec
                .constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addParameter(
                        ParameterizedTypeName.get(
                                Constants.EMBEDDED_PROP_EXPRESSION_CLASS_NAME,
                                type.getClassName()
                        ),
                        "raw"
                )
                .addStatement("super(raw)");
        typeBuilder.addMethod(builder.build());
    }

    private void addProps() {
        for (ImmutableProp prop : type.getProps().values()) {
            typeBuilder.addMethod(
                    PropsGenerator.property(
                            typeUtils,
                            false,
                            prop,
                            false,
                            true,
                            true
                    )
            );
        }
    }
}
