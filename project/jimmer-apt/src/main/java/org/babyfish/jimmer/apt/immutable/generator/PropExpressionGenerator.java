package org.babyfish.jimmer.apt.immutable.generator;

import com.squareup.javapoet.*;
import org.babyfish.jimmer.apt.GeneratorException;
import org.babyfish.jimmer.apt.Context;
import org.babyfish.jimmer.apt.immutable.meta.ImmutableProp;
import org.babyfish.jimmer.apt.immutable.meta.ImmutableType;

import javax.lang.model.element.Modifier;
import java.io.IOException;

import static org.babyfish.jimmer.apt.util.GeneratedAnnotation.generatedAnnotation;

public class PropExpressionGenerator {

    private final Context context;

    private final ImmutableType type;

    private TypeSpec.Builder typeBuilder;

    public PropExpressionGenerator(Context context, ImmutableType type) {
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
                .addAnnotation(generatedAnnotation(type))
                .superclass(
                        ParameterizedTypeName.get(
                                Constants.ABSTRACT_TYPED_EMBEDDED_PROP_EXPRESSION_CLASS_NAME,
                                type.getClassName()
                        )
                );
        typeBuilder = builder;
        try {
            addConstructor();
            addBaseTableConstructor();
            addProps();
            addBaseTableOwner();
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

    private void addBaseTableConstructor() {
        MethodSpec.Builder builder = MethodSpec
                .constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addParameter(
                        type.getPropExpressionClassName(),
                        "base"
                )
                .addParameter(
                        Constants.BASE_TABLE_OWNER_CLASS_NAME,
                        "baseTableOwner"
                )
                .addStatement("super(base, baseTableOwner)");
        typeBuilder.addMethod(builder.build());
    }

    private void addProps() {
        for (ImmutableProp prop : type.getProps().values()) {
            typeBuilder.addMethod(
                    PropsGenerator.property(
                            context,
                            false,
                            prop,
                            false,
                            true,
                            true
                    )
            );
        }
    }

    private void addBaseTableOwner() {
        MethodSpec.Builder builder = MethodSpec
                .methodBuilder("__baseTableOwner")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .addParameter(
                        Constants.BASE_TABLE_OWNER_CLASS_NAME,
                        "baseTableOwner"
                )
                .returns(type.getPropExpressionClassName())
                .addStatement("return new $T(this, baseTableOwner)", type.getPropExpressionClassName());
        typeBuilder.addMethod(builder.build());
    }
}
