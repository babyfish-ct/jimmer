package org.babyfish.jimmer.apt.util;

import com.squareup.javapoet.*;
import org.babyfish.jimmer.apt.Context;
import org.babyfish.jimmer.apt.MetaException;
import org.babyfish.jimmer.apt.immutable.generator.Constants;
import org.babyfish.jimmer.jackson.Converter;

import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import java.util.List;

public class ConverterMetadata {

    private final TypeMirror sourceType;

    private final TypeMirror targetType;

    private final TypeName sourceTypeName;

    private final TypeName targetTypeName;

    public static ConverterMetadata of(TypeElement converterElement) {
        if (!converterElement.getTypeParameters().isEmpty()) {
            throw new MetaException(
                    converterElement,
                    "It should not have type parameters"
            );
        }
        GenericParser.Result result = new GenericParser("converter", converterElement, Converter.class.getName()).parse();
        return new ConverterMetadata(
                result.arguments.get(0),
                result.arguments.get(1),
                result.argumentTypeNames.get(0),
                result.argumentTypeNames.get(1)
        );
    }

    public ConverterMetadata(TypeMirror sourceType, TypeMirror targetType, TypeName sourceTypeName, TypeName targetTypeName) {
        this.sourceType = sourceType;
        this.targetType = targetType;
        this.sourceTypeName = sourceTypeName;
        this.targetTypeName = targetTypeName;
    }

    public TypeMirror getSourceType() {
        return sourceType;
    }

    public TypeMirror getTargetType() {
        return targetType;
    }

    public TypeName getSourceTypeName() {
        return sourceTypeName;
    }

    public TypeName getTargetTypeName() {
        return targetTypeName;
    }

    public ConverterMetadata toListMetadata(Context ctx) {
        TypeElement listElement = ctx.getElements().getTypeElement(List.class.getName());
        return new ListMetadata(
                ctx.getTypes().getDeclaredType(listElement, sourceType),
                ctx.getTypes().getDeclaredType(listElement, targetType),
                ParameterizedTypeName.get(
                        Constants.LIST_CLASS_NAME,
                        sourceTypeName
                ),
                ParameterizedTypeName.get(
                        Constants.LIST_CLASS_NAME,
                        targetTypeName
                )
        );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ConverterMetadata metadata = (ConverterMetadata) o;

        if (!sourceTypeName.equals(metadata.sourceTypeName)) return false;
        return targetTypeName.equals(metadata.targetTypeName);
    }

    @Override
    public int hashCode() {
        int result = sourceTypeName.hashCode();
        result = 31 * result + targetTypeName.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "ConverterMetadata{" +
                "sourceTypeName=" + sourceTypeName +
                ", targetTypeName=" + targetTypeName +
                '}';
    }

    private static class ListMetadata extends ConverterMetadata {

        public ListMetadata(TypeMirror sourceType, TypeMirror targetType, TypeName sourceTypeName, TypeName targetTypeName) {
            super(sourceType, targetType, sourceTypeName, targetTypeName);
        }

        @Override
        public ConverterMetadata toListMetadata(Context context) {
            throw new UnsupportedOperationException("The current object is already list metadata");
        }
    }
}
