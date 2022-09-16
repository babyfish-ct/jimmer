package org.babyfish.jimmer.apt.generator;

import com.squareup.javapoet.*;
import org.babyfish.jimmer.apt.GeneratorException;
import org.babyfish.jimmer.apt.TypeUtils;
import org.babyfish.jimmer.apt.meta.ImmutableProp;
import org.babyfish.jimmer.apt.meta.ImmutableType;

import javax.annotation.processing.Filer;
import javax.lang.model.element.Modifier;
import java.io.IOException;

public class PropsGenerator {

    private final TypeUtils typeUtils;

    private final ImmutableType type;

    private final Filer filer;

    private TypeSpec.Builder typeBuilder;

    public PropsGenerator(TypeUtils typeUtils, ImmutableType type, Filer filer) {
        this.typeUtils = typeUtils;
        this.type = type;
        this.filer = filer;
    }

    public void generate() {
        try {
            JavaFile
                    .builder(
                            type.getPackageName(),
                            generateProps()
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

    private TypeSpec generateProps() {

        TypeSpec.Builder builder = TypeSpec
                .classBuilder(type.getPropsClassName())
                .addModifiers(Modifier.PUBLIC);
        typeBuilder = builder;
        try {
            addConstructor();
            for (ImmutableProp prop : type.getProps().values()) {
                addProp(prop);
            }
        } finally {
            typeBuilder = null;
        }
        return builder.build();
    }

    private void addConstructor() {
        typeBuilder.addMethod(
                MethodSpec
                        .constructorBuilder()
                        .addModifiers(Modifier.PRIVATE)
                        .build()
        );
    }

    private void addProp(ImmutableProp prop) {
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
        FieldSpec.Builder builder = FieldSpec
                .builder(
                        ParameterizedTypeName.get(
                                rawClassName,
                                type.getClassName(),
                                prop.getElementTypeName().box()
                        ),
                        Strings.upper(prop.getName()),
                        Modifier.PUBLIC,
                        Modifier.STATIC,
                        Modifier.FINAL
                )
                .initializer(
                        "\n    $T.$L($T.get($T.class).getProp($L))",
                        Constants.TYPED_PROP_CLASS_NAME,
                        action,
                        Constants.RUNTIME_TYPE_CLASS_NAME,
                        type.getClassName(),
                        Integer.toString(prop.getId())
                );
        typeBuilder.addField(builder.build());
    }
}
