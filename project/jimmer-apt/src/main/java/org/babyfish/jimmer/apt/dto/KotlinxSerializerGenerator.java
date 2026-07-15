package org.babyfish.jimmer.apt.dto;

import com.squareup.javapoet.*;
import org.babyfish.jimmer.apt.immutable.generator.Constants;
import org.babyfish.jimmer.apt.immutable.meta.ImmutableProp;
import org.babyfish.jimmer.apt.immutable.meta.ImmutableType;
import org.babyfish.jimmer.dto.compiler.AbstractProp;
import org.babyfish.jimmer.dto.compiler.DtoModifier;
import org.babyfish.jimmer.dto.compiler.DtoProp;
import org.babyfish.jimmer.dto.compiler.DtoType;

import javax.lang.model.element.Modifier;
import java.util.List;

public class KotlinxSerializerGenerator {

    private static final String SERIALIZER_CLASS_NAME = "$serializer";

    private static final String CHILD_SERIALIZERS_FIELD_NAME = "CHILD_SERIALIZERS";

    private static final String DESCRIPTOR_FIELD_NAME = "DESCRIPTOR";

    private final DtoGenerator parentGenerator;

    private final DtoType<ImmutableType, ImmutableProp> dtoType;

    private final ClassName dtoClassName;

    private final ClassName serializerClassName;

    private final ArrayTypeName serializersArrayTypeName;

    public KotlinxSerializerGenerator(DtoGenerator parentGenerator) {
        this.parentGenerator = parentGenerator;
        this.dtoType = parentGenerator.dtoType;
        this.dtoClassName = parentGenerator.getDtoClassName(null);
        this.serializerClassName = parentGenerator.getDtoClassName(SERIALIZER_CLASS_NAME);
        this.serializersArrayTypeName = ArrayTypeName.of(
                ParameterizedTypeName.get(
                        Constants.KOTLINX_KSERIALIZER_CLASS_NAME,
                        WildcardTypeName.subtypeOf(TypeName.OBJECT)
                )
        );
    }

    public void generate() {
        TypeSpec.Builder builder = TypeSpec
                .classBuilder(SERIALIZER_CLASS_NAME)
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                .addSuperinterface(
                        ParameterizedTypeName.get(
                                Constants.KOTLINX_KSERIALIZER_CLASS_NAME,
                                dtoClassName
                        )
                )
                .addSuperinterface(
                        ParameterizedTypeName.get(
                                Constants.KOTLINX_GENERATED_SERIALIZER_CLASS_NAME,
                                dtoClassName
                        )
                );

        builder.addField(instanceField());
        builder.addField(childSerializersField());
        builder.addField(descriptorField());
        builder.addStaticBlock(descriptorStaticBlock());
        builder.addMethod(serializerMethod());
        builder.addMethod(descriptorGetter());
        builder.addMethod(childSerializersGetter());
        builder.addMethod(typeParameterSerializersGetter());
        builder.addMethod(serializeMethod());
        builder.addMethod(deserializeMethod());

        parentGenerator.getTypeBuilder().addType(builder.build());
    }

    private FieldSpec instanceField() {
        return FieldSpec
                .builder(
                        serializerClassName,
                        "INSTANCE",
                        Modifier.PUBLIC,
                        Modifier.STATIC,
                        Modifier.FINAL
                )
                .initializer("new $T()", serializerClassName)
                .build();
    }

    private FieldSpec childSerializersField() {
        List<AbstractProp> props = dtoType.getProps();
        CodeBlock.Builder block = CodeBlock
                .builder()
                .add("new $T {$>", serializersArrayTypeName);
        for (int i = 0; i < props.size(); i++) {
            AbstractProp prop = props.get(i);
            if (i != 0) {
                block.add(",");
            }
            block.add(
                    "\nserializer($S, $L)",
                    prop.getName(),
                    isNullable(prop)
            );
        }
        if (!props.isEmpty()) {
            block.add("\n");
        }
        block.add("$<}");
        return FieldSpec
                .builder(
                        serializersArrayTypeName,
                        CHILD_SERIALIZERS_FIELD_NAME,
                        Modifier.PRIVATE,
                        Modifier.STATIC,
                        Modifier.FINAL
                )
                .initializer(block.build())
                .build();
    }

    private FieldSpec descriptorField() {
        return FieldSpec
                .builder(
                        Constants.KOTLINX_SERIAL_DESCRIPTOR_CLASS_NAME,
                        DESCRIPTOR_FIELD_NAME,
                        Modifier.PRIVATE,
                        Modifier.STATIC,
                        Modifier.FINAL
                )
                .build();
    }

    private CodeBlock descriptorStaticBlock() {
        CodeBlock.Builder block = CodeBlock
                .builder()
                .addStatement(
                        "$T descriptor = new $T($S, INSTANCE, $L)",
                        Constants.KOTLINX_PLUGIN_GENERATED_SERIAL_DESCRIPTOR_CLASS_NAME,
                        Constants.KOTLINX_PLUGIN_GENERATED_SERIAL_DESCRIPTOR_CLASS_NAME,
                        dtoClassName.canonicalName(),
                        dtoType.getProps().size()
                );
        for (AbstractProp prop : dtoType.getProps()) {
            block.addStatement(
                    "descriptor.addElement($S, $L)",
                    prop.getName(),
                    isElementOptional(prop)
            );
        }
        block.addStatement("$L = descriptor", DESCRIPTOR_FIELD_NAME);
        return block.build();
    }

    private MethodSpec serializerMethod() {
        TypeName objectSerializerTypeName =
                ParameterizedTypeName.get(Constants.KOTLINX_KSERIALIZER_CLASS_NAME, TypeName.OBJECT.box());
        return MethodSpec
                .methodBuilder("serializer")
                .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
                .returns(
                        ParameterizedTypeName.get(
                                Constants.KOTLINX_KSERIALIZER_CLASS_NAME,
                                WildcardTypeName.subtypeOf(TypeName.OBJECT)
                        )
                )
                .addParameter(String.class, "fieldName")
                .addParameter(TypeName.BOOLEAN, "nullable")
                .beginControlFlow("try")
                .addStatement(
                        "$T<?> serializer = $T.serializer($T.class.getDeclaredField(fieldName).getGenericType())",
                        Constants.KOTLINX_KSERIALIZER_CLASS_NAME,
                        Constants.KOTLINX_SERIALIZERS_CLASS_NAME,
                        dtoClassName
                )
                .beginControlFlow("if (nullable)")
                .addStatement(
                        "return $T.getNullable(($T)serializer)",
                        Constants.KOTLINX_BUILTIN_SERIALIZERS_CLASS_NAME,
                        objectSerializerTypeName
                )
                .endControlFlow()
                .addStatement("return serializer")
                .nextControlFlow("catch ($T ex)", NoSuchFieldException.class)
                .addStatement("throw new $T(ex)", AssertionError.class)
                .endControlFlow()
                .build();
    }

    private MethodSpec descriptorGetter() {
        return MethodSpec
                .methodBuilder("getDescriptor")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .returns(Constants.KOTLINX_SERIAL_DESCRIPTOR_CLASS_NAME)
                .addStatement("return $L", DESCRIPTOR_FIELD_NAME)
                .build();
    }

    private MethodSpec childSerializersGetter() {
        return MethodSpec
                .methodBuilder("childSerializers")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .returns(serializersArrayTypeName)
                .addStatement("return $L", CHILD_SERIALIZERS_FIELD_NAME)
                .build();
    }

    private MethodSpec typeParameterSerializersGetter() {
        return MethodSpec
                .methodBuilder("typeParametersSerializers")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .returns(serializersArrayTypeName)
                .addStatement("return new $T[0]", Constants.KOTLINX_KSERIALIZER_CLASS_NAME)
                .build();
    }

    private MethodSpec serializeMethod() {
        MethodSpec.Builder builder = MethodSpec
                .methodBuilder("serialize")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .addParameter(Constants.KOTLINX_ENCODER_CLASS_NAME, "encoder")
                .addParameter(dtoClassName, "input")
                .addStatement(
                        "$T composite = encoder.beginStructure($L)",
                        Constants.KOTLINX_COMPOSITE_ENCODER_CLASS_NAME,
                        DESCRIPTOR_FIELD_NAME
                );
        List<AbstractProp> props = dtoType.getProps();
        for (int i = 0; i < props.size(); i++) {
            AbstractProp prop = props.get(i);
            if (isDynamic(prop)) {
                builder.beginControlFlow(
                        "if (input.$L())",
                        parentGenerator.getterName(loadedProp(prop))
                );
                addEncodeStatement(builder, prop, i);
                builder.endControlFlow();
            } else {
                addEncodeStatement(builder, prop, i);
            }
        }
        builder.addStatement("composite.endStructure($L)", DESCRIPTOR_FIELD_NAME);
        return builder.build();
    }

    private void addEncodeStatement(MethodSpec.Builder builder, AbstractProp prop, int index) {
        TypeName typeName = parentGenerator.getPropTypeName(prop);
        String getterName = parentGenerator.getterName(prop);
        if (!isNullable(prop) && typeName.equals(TypeName.BOOLEAN)) {
            addPrimitiveEncodeStatement(builder, "encodeBooleanElement", index, getterName);
        } else if (!isNullable(prop) && typeName.equals(TypeName.BYTE)) {
            addPrimitiveEncodeStatement(builder, "encodeByteElement", index, getterName);
        } else if (!isNullable(prop) && typeName.equals(TypeName.SHORT)) {
            addPrimitiveEncodeStatement(builder, "encodeShortElement", index, getterName);
        } else if (!isNullable(prop) && typeName.equals(TypeName.CHAR)) {
            addPrimitiveEncodeStatement(builder, "encodeCharElement", index, getterName);
        } else if (!isNullable(prop) && typeName.equals(TypeName.INT)) {
            addPrimitiveEncodeStatement(builder, "encodeIntElement", index, getterName);
        } else if (!isNullable(prop) && typeName.equals(TypeName.LONG)) {
            addPrimitiveEncodeStatement(builder, "encodeLongElement", index, getterName);
        } else if (!isNullable(prop) && typeName.equals(TypeName.FLOAT)) {
            addPrimitiveEncodeStatement(builder, "encodeFloatElement", index, getterName);
        } else if (!isNullable(prop) && typeName.equals(TypeName.DOUBLE)) {
            addPrimitiveEncodeStatement(builder, "encodeDoubleElement", index, getterName);
        } else if (!isNullable(prop) && typeName.equals(Constants.STRING_CLASS_NAME)) {
            addPrimitiveEncodeStatement(builder, "encodeStringElement", index, getterName);
        } else {
            builder.addStatement(
                    "composite.$L($L, $L, ($T)$L[$L], input.$L())",
                    isNullable(prop) ? "encodeNullableSerializableElement" : "encodeSerializableElement",
                    DESCRIPTOR_FIELD_NAME,
                    index,
                    Constants.KOTLINX_SERIALIZATION_STRATEGY_CLASS_NAME,
                    CHILD_SERIALIZERS_FIELD_NAME,
                    index,
                    getterName
            );
        }
    }

    private void addPrimitiveEncodeStatement(
            MethodSpec.Builder builder,
            String methodName,
            int index,
            String getterName
    ) {
        builder.addStatement(
                "composite.$L($L, $L, input.$L())",
                methodName,
                DESCRIPTOR_FIELD_NAME,
                index,
                getterName
        );
    }

    private MethodSpec deserializeMethod() {
        MethodSpec.Builder builder = MethodSpec
                .methodBuilder("deserialize")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .returns(dtoClassName)
                .addParameter(Constants.KOTLINX_DECODER_CLASS_NAME, "decoder")
                .addStatement("$T output = new $T()", dtoClassName, dtoClassName)
                .addStatement(
                        "$T composite = decoder.beginStructure($L)",
                        Constants.KOTLINX_COMPOSITE_DECODER_CLASS_NAME,
                        DESCRIPTOR_FIELD_NAME
                )
                .beginControlFlow("while (true)")
                .addStatement("int index = composite.decodeElementIndex($L)", DESCRIPTOR_FIELD_NAME)
                .beginControlFlow("if (index == $T.DECODE_DONE)", Constants.KOTLINX_COMPOSITE_DECODER_CLASS_NAME)
                .addStatement("break")
                .endControlFlow()
                .beginControlFlow("switch (index)");
        List<AbstractProp> props = dtoType.getProps();
        for (int i = 0; i < props.size(); i++) {
            AbstractProp prop = props.get(i);
            builder.addCode("case $L:\n$>", i);
            addDecodeStatement(builder, prop, i);
            builder.addStatement("break");
            builder.addCode("$<");
        }
        builder.addCode("case $T.UNKNOWN_NAME:\n$>", Constants.KOTLINX_COMPOSITE_DECODER_CLASS_NAME);
        builder.addStatement("break");
        builder.addCode("$<");
        builder.addCode("default:\n$>");
        builder.addStatement(
                "throw new $T($S + index)",
                Constants.KOTLINX_SERIALIZATION_EXCEPTION_CLASS_NAME,
                "Unexpected index: "
        );
        builder.addCode("$<");
        builder.endControlFlow();
        builder.endControlFlow();
        builder.addStatement("composite.endStructure($L)", DESCRIPTOR_FIELD_NAME);
        builder.addStatement("return output");
        return builder.build();
    }

    private void addDecodeStatement(MethodSpec.Builder builder, AbstractProp prop, int index) {
        TypeName typeName = parentGenerator.getPropTypeName(prop);
        String setterName = parentGenerator.setterName(prop);
        if (!isNullable(prop) && typeName.equals(TypeName.BOOLEAN)) {
            addPrimitiveDecodeStatement(builder, setterName, "decodeBooleanElement", index);
        } else if (!isNullable(prop) && typeName.equals(TypeName.BYTE)) {
            addPrimitiveDecodeStatement(builder, setterName, "decodeByteElement", index);
        } else if (!isNullable(prop) && typeName.equals(TypeName.SHORT)) {
            addPrimitiveDecodeStatement(builder, setterName, "decodeShortElement", index);
        } else if (!isNullable(prop) && typeName.equals(TypeName.CHAR)) {
            addPrimitiveDecodeStatement(builder, setterName, "decodeCharElement", index);
        } else if (!isNullable(prop) && typeName.equals(TypeName.INT)) {
            addPrimitiveDecodeStatement(builder, setterName, "decodeIntElement", index);
        } else if (!isNullable(prop) && typeName.equals(TypeName.LONG)) {
            addPrimitiveDecodeStatement(builder, setterName, "decodeLongElement", index);
        } else if (!isNullable(prop) && typeName.equals(TypeName.FLOAT)) {
            addPrimitiveDecodeStatement(builder, setterName, "decodeFloatElement", index);
        } else if (!isNullable(prop) && typeName.equals(TypeName.DOUBLE)) {
            addPrimitiveDecodeStatement(builder, setterName, "decodeDoubleElement", index);
        } else if (!isNullable(prop) && typeName.equals(Constants.STRING_CLASS_NAME)) {
            addPrimitiveDecodeStatement(builder, setterName, "decodeStringElement", index);
        } else {
            builder.addStatement(
                    "output.$L(($T)composite.$L($L, $L, ($T)$L[$L], null))",
                    setterName,
                    typeName,
                    isNullable(prop) ? "decodeNullableSerializableElement" : "decodeSerializableElement",
                    DESCRIPTOR_FIELD_NAME,
                    index,
                    Constants.KOTLINX_DESERIALIZATION_STRATEGY_CLASS_NAME,
                    CHILD_SERIALIZERS_FIELD_NAME,
                    index
            );
        }
    }

    private void addPrimitiveDecodeStatement(
            MethodSpec.Builder builder,
            String setterName,
            String methodName,
            int index
    ) {
        builder.addStatement(
                "output.$L(composite.$L($L, $L))",
                setterName,
                methodName,
                DESCRIPTOR_FIELD_NAME,
                index
        );
    }

    private boolean isNullable(AbstractProp prop) {
        return prop.isNullable();
    }

    private boolean isElementOptional(AbstractProp prop) {
        if (isNullable(prop)) {
            return true;
        }
        if (prop instanceof DtoProp<?, ?>) {
            return parentGenerator.stateFieldName(loadedProp(prop), false) != null;
        }
        return false;
    }

    private boolean isDynamic(AbstractProp prop) {
        return prop instanceof DtoProp<?, ?> &&
                prop.getInputModifier() == DtoModifier.DYNAMIC;
    }

    @SuppressWarnings("unchecked")
    private DtoProp<ImmutableType, ImmutableProp> loadedProp(AbstractProp prop) {
        return (DtoProp<ImmutableType, ImmutableProp>) prop;
    }
}
