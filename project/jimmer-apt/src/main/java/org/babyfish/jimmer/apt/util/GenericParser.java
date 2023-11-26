package org.babyfish.jimmer.apt.util;

import com.squareup.javapoet.*;
import org.babyfish.jimmer.apt.MetaException;

import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.type.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class GenericParser {

    private final String name;

    private final TypeElement element;

    private final String superName;

    private final Map<TypeVariable, TypeMirror> replaceMap = new HashMap<>();

    public GenericParser(String name, TypeElement element, String superName) {
        this.name = name.toLowerCase();
        this.element = element;
        this.superName = superName;
    }

    public List<TypeName> parse() {
        try {
            parse((DeclaredType) element.asType());
        } catch (Finished ex) {
            return ex.arguments;
        }
        throw new MetaException(
                element,
                "it does not specify the arguments for \"" +
                        superName +
                        "\""
        );
    }

    private void parse(DeclaredType type) throws Finished {
        TypeElement typeElement = (TypeElement) type.asElement();
        List<? extends TypeMirror> arguments = type.getTypeArguments();
        if (typeElement.getQualifiedName().toString().equals(superName)) {
            if (arguments.isEmpty()) {
                throw new MetaException(
                        this.element,
                        "The " +
                                name +
                                " type \"" +
                                typeElement +
                                "\" does not specify type arguments for \"" +
                                superName +
                                "\""
                );
            }
            throw new Finished(
                    arguments.stream().map(this::resolve).collect(Collectors.toList())
            );
        } else {
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
                element,
                "cannot resolve \"" + type + "\""
        );
    }

    private TypeName resolve(TypeVariable typeVariable) {
        TypeMirror type = replaceMap.get(typeVariable);
        if (type == null) {
            throw new MetaException(
                    element,
                    "cannot resolve \"" + typeVariable + "\""
            );
        }
        return resolve(type);
    }

    private static class Finished extends Exception {

        final List<TypeName> arguments;

        private Finished(List<TypeName> arguments) {
            this.arguments = arguments;
        }
    }
}
