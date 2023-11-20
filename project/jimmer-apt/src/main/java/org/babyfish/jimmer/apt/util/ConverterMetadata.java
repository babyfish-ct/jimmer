package org.babyfish.jimmer.apt.util;

import com.squareup.javapoet.*;
import org.babyfish.jimmer.apt.MetaException;
import org.babyfish.jimmer.apt.generator.Constants;
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
        return new ParseContext(converterElement).get();
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

    private static class ParseContext {

        private static final String CONVERTER_NAME = Converter.class.getName();

        public final TypeElement converterTypeElement;

        private final Map<TypeVariable, TypeMirror> replaceMap = new HashMap<>();

        private ParseContext(TypeElement converterTypeElement) {
            this.converterTypeElement = converterTypeElement;
        }

        private void parse(DeclaredType type) throws Finished {
            TypeElement typeElement = (TypeElement) type.asElement();
            if (typeElement.getQualifiedName().toString().equals(CONVERTER_NAME)) {
                List<? extends TypeMirror> arguments = type.getTypeArguments();
                if (arguments.size() != 2) {
                    throw new MetaException(
                            this.converterTypeElement,
                            "The converter type \"" +
                                    typeElement +
                                    "\" does not specify type arguments for \"" +
                                    Converter.class.getName() +
                                    "\""
                    );
                }
                throw new Finished(
                        resolve(arguments.get(0)),
                        resolve(arguments.get(1))
                );
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
                parse((DeclaredType) converterTypeElement.asType());
            } catch (Finished ex) {
                return new ConverterMetadata(ex.sourceTypeName, ex.targetTypeName);
            }
            throw new MetaException(
                    converterTypeElement,
                    "it does not specify the arguments for \"" +
                            Converter.class.getName() +
                            "\""
            );
        }

        private TypeName resolve(TypeMirror type) {
            if (type instanceof DeclaredType) {
                DeclaredType declaredType = (DeclaredType) type;
                TypeElement typeElement = (TypeElement) declaredType.asElement();
                ClassName className = ClassName.bestGuess(typeElement.getQualifiedName().toString());
                List<? extends TypeMirror> arguments = declaredType.getTypeArguments();
                if (!arguments.isEmpty()) {
                    TypeName[] argTypeNames = new TypeName[arguments.size()];
                    for (int i = arguments.size() - 1; i >= 0; --i) {
                        argTypeNames[i] = resolve(arguments.get(i));
                    }
                    return ParameterizedTypeName.get(className, argTypeNames);
                }
                return className;
            }
            if (type instanceof TypeVariable) {
                return resolve((TypeVariable) type);
            }
            if (type instanceof WildcardType) {
                WildcardType wildcardType = (WildcardType) type;
                if (wildcardType.getSuperBound() != null) {
                    return WildcardTypeName.supertypeOf(
                            resolve(wildcardType.getSuperBound())
                    );
                }
                return WildcardTypeName.subtypeOf(
                        wildcardType.getExtendsBound() != null ?
                                resolve(wildcardType.getExtendsBound()) :
                                TypeName.OBJECT
                );
            }
            if (type instanceof ArrayType) {
                return ArrayTypeName.of(
                        resolve(((ArrayType)type).getComponentType())
                );
            }
            throw new MetaException(
                    converterTypeElement,
                    "cannot resolve \"" + type + "\""
            );
        }

        private TypeName resolve(TypeVariable typeVariable) {
            TypeMirror type = replaceMap.get(typeVariable);
            if (type == null) {
                throw new MetaException(
                        converterTypeElement,
                        "cannot resolve \"" + typeVariable + "\""
                );
            }
            return resolve(type);
        }
    }

    private static class Finished extends Exception {

        final TypeName sourceTypeName;

        final TypeName targetTypeName;

        private Finished(TypeName sourceTypeName, TypeName targetTypeName) {
            this.sourceTypeName = sourceTypeName;
            this.targetTypeName = targetTypeName;
        }
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
