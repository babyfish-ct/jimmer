package org.babyfish.jimmer.apt.util;

import com.squareup.javapoet.*;
import org.babyfish.jimmer.apt.MetaException;
import org.babyfish.jimmer.apt.immutable.generator.Constants;
import org.babyfish.jimmer.jackson.Converter;

import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.type.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConverterMetadata {

    private final TypeName sourceTypeName;

    private final TypeName targetTypeName;

    public static ConverterMetadata of(TypeElement converterElement) {
        if (!converterElement.getTypeParameters().isEmpty()) {
            throw new MetaException(
                    converterElement,
                    "It should not have type parameters"
            );
        }
        List<TypeName> arguments = new GenericParser("converter", converterElement, Converter.class.getName()).parse();
        return new ConverterMetadata(arguments.get(0), arguments.get(1));
    }

    public ConverterMetadata(TypeName sourceTypeName, TypeName targetTypeName) {
        this.sourceTypeName = sourceTypeName;
        this.targetTypeName = targetTypeName;
    }

    public TypeName getSourceTypeName() {
        return sourceTypeName;
    }

    public TypeName getTargetTypeName() {
        return targetTypeName;
    }

    public ConverterMetadata toListMetadata() {
        return new ListMetadata();
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

    private class ListMetadata extends ConverterMetadata {

        public ListMetadata() {
            super(
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
        public ConverterMetadata toListMetadata() {
            throw new UnsupportedOperationException("The current object is already list metadata");
        }
    }
}
