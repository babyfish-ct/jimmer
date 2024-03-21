package org.babyfish.jimmer.apt.dto;

import org.babyfish.jimmer.apt.Context;
import org.babyfish.jimmer.apt.MetaException;
import org.babyfish.jimmer.apt.immutable.meta.ImmutableProp;
import org.babyfish.jimmer.apt.immutable.meta.ImmutableType;
import org.babyfish.jimmer.dto.compiler.DtoAstException;
import org.babyfish.jimmer.dto.compiler.DtoType;
import org.babyfish.jimmer.dto.compiler.TypeRef;
import org.babyfish.jimmer.impl.util.StringUtil;

import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

public class DtoInterfaces {

    public static Set<String> abstractMethodNames(Context ctx, DtoType<ImmutableType, ImmutableProp> dtoType) {
        if (dtoType.getSuperInterfaces().isEmpty()) {
            return Collections.emptySet();
        }
        Set<String> methodNames = new LinkedHashSet<>();
        Set<String> handledTypeNames = new HashSet<>();
        for (TypeRef typeRef : dtoType.getSuperInterfaces()) {
            TypeElement typeElement = ctx.getElements().getTypeElement(typeRef.getTypeName());
            if (typeElement.getKind() != ElementKind.INTERFACE) {
                throw new DtoAstException(
                        dtoType.getDtoFile(),
                        typeRef.getLine(),
                        typeRef.getCol(),
                        "The super type \"" +
                                typeRef.getTypeName() +
                                "\" is not interface"
                );
            }
            collectMembers(typeElement, ctx, handledTypeNames, methodNames);
        }
        return methodNames;
    }

    private static void collectMembers(TypeElement typeElement, Context ctx, Set<String> handledTypeNames, Set<String> methodNames) {
        String qualifiedName = typeElement.getQualifiedName().toString();
        if (!handledTypeNames.add(qualifiedName)) {
            return;
        }
        for (ExecutableElement executableElement : ElementFilter.methodsIn(typeElement.getEnclosedElements())) {
            if (executableElement.getModifiers().contains(Modifier.STATIC) || executableElement.getModifiers().contains(Modifier.DEFAULT)) {
                continue;
            }
            if (!executableElement.getTypeParameters().isEmpty()) {
                throw new MetaException(
                        executableElement,
                        "Illegal abstract method, the declaring interface \"" +
                                qualifiedName +
                                "\" or its derived interface is used as the super interface of generated DTO type " +
                                "so that this abstract method cannot have generic parameters"
                );
            }
            String methodName = executableElement.getSimpleName().toString();
            if (methodName.equals("hashCode") && executableElement.getParameters().isEmpty()) {
                continue;
            }
            if (methodName.equals("equals") && executableElement.getParameters().size() == 1) {
                TypeMirror typeMirror = executableElement.getParameters().get(0).asType();
                if (typeMirror.getKind() == TypeKind.DECLARED) {
                    TypeElement paramTypeElement = (TypeElement) ((DeclaredType) typeMirror).asElement();
                    if ("java.lang.Object".equals(paramTypeElement.getQualifiedName().toString())) {
                        continue;
                    }
                }
            }
            if (methodName.equals("toString") && executableElement.getParameters().isEmpty()) {
                continue;
            }
            String propName = StringUtil.propName(
                    methodName,
                    executableElement.getReturnType().getKind() == TypeKind.BOOLEAN
            );
            if (propName != null) {
                if (!executableElement.getParameters().isEmpty() || executableElement.getReturnType().getKind() == TypeKind.VOID) {
                    propName = null;
                }
            } else {
                if (executableElement.getParameters().size() == 1 &&
                        executableElement.getReturnType().getKind() == TypeKind.VOID &&
                        methodName.startsWith("set") &&
                        methodName.length() > 3 &&
                        Character.isUpperCase(methodName.charAt(3))
                ) {
                    propName = StringUtil.identifier(methodName.substring(3));
                }
            }
            if (propName == null) {
                throw new MetaException(
                        executableElement,
                        "Illegal abstract method, the declaring interface \"" +
                                qualifiedName +
                                "\" or its derived interface is used as the super interface of generated DTO type " +
                                "but this abstract method can be consider as neither getter and setter"
                );
            }
            methodNames.add(methodName);
        }
        for (TypeMirror typeMirror : typeElement.getInterfaces()) {
            collectMembers((TypeElement) ctx.getTypes().asElement(typeMirror), ctx, handledTypeNames, methodNames);
        }
    }
}
