package org.babyfish.jimmer.apt.meta;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;

public class MetaException extends RuntimeException {

    private final Element element;

    public MetaException(Element element, String reason) {
        super(message(element, reason));
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
                    ((TypeElement) element).getQualifiedName().toString() +
                    "\", " +
                    lowerFirstChar(reason);
        }
        if (element instanceof ExecutableElement) {
            return "Illegal property \"" +
                    ((TypeElement)element.getEnclosingElement()).getQualifiedName() +
                    '.' +
                    element.getSimpleName().toString() +
                    "\", " +
                    lowerFirstChar(reason);
        }
        return reason;
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
