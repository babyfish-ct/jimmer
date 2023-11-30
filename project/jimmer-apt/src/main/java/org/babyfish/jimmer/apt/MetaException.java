package org.babyfish.jimmer.apt;

import org.babyfish.jimmer.Immutable;
import org.babyfish.jimmer.sql.Embeddable;
import org.babyfish.jimmer.sql.Entity;
import org.babyfish.jimmer.sql.MappedSuperclass;

import javax.lang.model.element.*;
import javax.lang.model.type.TypeKind;

public class MetaException extends RuntimeException {

    private final Element element;

    public MetaException(Element element, String reason) {
        super(message(element, reason));
        this.element = element;
    }

    public MetaException(Element element, Element childElement, String reason) {
        super(message(element, childElement == element ? reason : message(childElement, reason)));
        this.element = element;
    }

    public MetaException(Element element, String reason, Throwable cause) {
        super(message(element, reason), cause);
        this.element = element;
    }

    public Element getElement() {
        return element;
    }

    private static String message(Element element, String reason) {
        if (element instanceof TypeElement) {
            return "Illegal type \"" +
                    longName(element) +
                    "\", " +
                    lowerFirstChar(reason);
        }
        if (element instanceof ExecutableElement) {
            TypeElement type = (TypeElement) element.getEnclosingElement();
            return "Illegal " +
                    (isProperty((ExecutableElement) element) ? "property" : "method") +
                    " \"" +
                    longName(element) +
                    "\", " +
                    lowerFirstChar(reason);
        }
        if (element instanceof VariableElement) {
            return "Illegal parameter \"" +
                    longName(element) +
                    "\", " +
                    lowerFirstChar(reason);
        }
        return reason;
    }

    private static String longName(Element element) {
        if (element instanceof TypeElement) {
            return ((TypeElement) element).getQualifiedName().toString();
        }
        return longName(element.getEnclosingElement()) +
                (element.getKind() == ElementKind.PARAMETER ? ':' : '.') +
                element.getSimpleName().toString();
    }

    private static boolean isProperty(ExecutableElement element) {
        TypeElement typeElement = (TypeElement) element.getEnclosingElement();
        if (typeElement.getAnnotation(Immutable.class) != null ||
                typeElement.getAnnotation(Entity.class) != null ||
                typeElement.getAnnotation(MappedSuperclass.class) != null ||
                typeElement.getAnnotation(Embeddable.class) != null) {
            return true;
        }
        if (element.getParameters().isEmpty() && element.getTypeParameters().isEmpty()) {
            String simpleName = element.getSimpleName().toString();
            if (simpleName.startsWith("get") && !Character.isLowerCase(simpleName.charAt(3))) {
                return true;
            }
            if (simpleName.startsWith("is") &&
                    !Character.isLowerCase(simpleName.charAt(2)) &&
                    element.getReturnType().getKind() == TypeKind.BOOLEAN) {
                return true;
            }
        }
        return false;
    }

    private static String lowerFirstChar(String reason) {
        int whitespaceCount = 0;
        while (whitespaceCount < reason.length()) {
            if (!Character.isWhitespace(reason.charAt(whitespaceCount))) {
                break;
            }
            whitespaceCount++;
        }
        if (whitespaceCount != 0) {
            reason = reason.substring(whitespaceCount);
        }
        if (reason.isEmpty()) {
            return reason;
        }
        if (Character.isUpperCase(reason.charAt(0))) {
            return Character.toLowerCase(reason.charAt(0)) + reason.substring(1);
        }
        return reason;
    }
}
