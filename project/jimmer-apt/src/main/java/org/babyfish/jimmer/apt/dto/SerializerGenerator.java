package org.babyfish.jimmer.apt.dto;

import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import org.babyfish.jimmer.apt.immutable.meta.ImmutableProp;
import org.babyfish.jimmer.apt.immutable.meta.ImmutableType;
import org.babyfish.jimmer.dto.compiler.DtoModifier;
import org.babyfish.jimmer.dto.compiler.DtoProp;
import org.babyfish.jimmer.dto.compiler.DtoType;
import org.babyfish.jimmer.impl.util.StringUtil;

import javax.lang.model.element.Modifier;
import java.io.IOException;

public class SerializerGenerator {

    private final DtoGenerator parentGenerator;

    private final DtoType<ImmutableType, ImmutableProp> dtoType;

    public SerializerGenerator(
            DtoGenerator parentGenerator
    ) {
        this.parentGenerator = parentGenerator;
        this.dtoType = parentGenerator.dtoType;
    }

    public void generate() {
        TypeSpec.Builder builder = TypeSpec
                .classBuilder("Serializer")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .superclass(
                        ParameterizedTypeName.get(
                                parentGenerator.ctx.getJacksonTypes().jsonSerializer,
                                parentGenerator.getDtoClassName(null)
                        )
                );
        builder.addMethod(newSerialize());
        parentGenerator.getTypeBuilder().addType(builder.build());
    }

    private MethodSpec newSerialize() {
        MethodSpec.Builder builder = MethodSpec
                .methodBuilder("serialize")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .addParameter(
                        parentGenerator.getDtoClassName(null),
                        "input"
                )
                .addParameter(
                        parentGenerator.ctx.getJacksonTypes().jsonGenerator,
                        "gen"
                )
                .addParameter(
                        parentGenerator.ctx.getJacksonTypes().serializerProvider,
                        "provider"
                )
                .addException(IOException.class);
        builder.addStatement("gen.writeStartObject()");
        for (DtoProp<?, ?> prop : dtoType.getDtoProps()) {
            DtoModifier inputModifier = prop.getInputModifier();
            if (inputModifier == DtoModifier.DYNAMIC) {
                builder.beginControlFlow(
                        "if (input.$L())",
                        StringUtil.identifier("is", prop.getName(), "Loaded")
                );
                builder.addStatement(
                        "provider.defaultSerializeField($S, input.$L(), gen)",
                        prop.getName(),
                        parentGenerator.getterName(prop)
                );
                builder.endControlFlow();
            } else {
                builder.addStatement(
                        "provider.defaultSerializeField($S, input.$L(), gen)",
                        prop.getName(),
                        parentGenerator.getterName(prop)
                );
            }
        }
        builder.addStatement("gen.writeEndObject()");
        return builder.build();
    }
}
