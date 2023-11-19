package org.babyfish.jimmer.apt.util;

import com.squareup.javapoet.TypeName;
import org.babyfish.jimmer.apt.MetaException;
import org.babyfish.jimmer.jackson.Converter;

import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
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
        return new ParseContext(converterElement).get();
    }

    private ConverterMetadata(TypeName sourceTypeName, TypeName targetTypeName) {
        this.sourceTypeName = sourceTypeName;
        this.targetTypeName = targetTypeName;
    }

    public TypeName getSourceTypeName() {
        return sourceTypeName;
    }

    public TypeName getTargetTypeName() {
        return targetTypeName;
    }

    private static class ParseContext {

        private static final String CONVERTER_NAME = Converter.class.getName();

        public final TypeElement typeElement;

        private Map<TypeVariable, TypeMirror> replaceMap = new HashMap<>();

        private TypeName sourceTypeName;

        private TypeName targetTypeName;

        private ParseContext(TypeElement typeElement) {
            this.typeElement = typeElement;
        }

        private void parse(DeclaredType type) throws Finished {
            TypeElement typeElement = (TypeElement) type.asElement();
            if (typeElement.getQualifiedName().toString().equals(CONVERTER_NAME)) {
                List<? extends TypeMirror> arguments = type.getTypeArguments();
                TypeMirror sourceType = null;
                TypeMirror targetType = null;
                if (arguments.size() == 2) {
                    sourceType = resolve(arguments.get(0));
                    targetType = resolve(arguments.get(1));
                }
                if (sourceType != null && targetType != null) {
                    this.sourceTypeName = TypeName.get(sourceType);
                    this.targetTypeName = TypeName.get(targetType);
                    throw new Finished();
                }
            } else {
                List<? extends TypeMirror> arguments = type.getTypeArguments();
                if (!arguments.isEmpty()) {
                    List<? extends TypeParameterElement> parameters = typeElement.getTypeParameters();
                    int size = arguments.size();
                    for (int i = 0; i < size; i++) {
                        replaceMap.put((TypeVariable) parameters.get(i).asType(), arguments.get(i));
                    }
                }
                TypeMirror superType = typeElement.getSuperclass();
                if (superType instanceof DeclaredType) {
                    parse((DeclaredType) superType);
                }
                for (TypeMirror superItf : typeElement.getInterfaces()) {
                    parse((DeclaredType) superItf);
                }
            }
        }

        public ConverterMetadata get() {
            try {
                parse((DeclaredType) typeElement.asType());
            } catch (Finished ex) {
                return new ConverterMetadata(sourceTypeName, targetTypeName);
            }
            throw new MetaException(
                    typeElement,
                    "it does not specify the arguments for \"" +
                            Converter.class.getName() +
                            "\""
            );
        }

        private TypeMirror resolve(TypeMirror type) {
            while (type instanceof TypeVariable) {
                type = replaceMap.get((TypeVariable) type);
            }
            return type;
        }
    }

    private static class Finished extends Exception {}
}
