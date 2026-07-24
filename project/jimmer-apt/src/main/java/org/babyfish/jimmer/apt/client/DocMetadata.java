package org.babyfish.jimmer.apt.client;

import org.babyfish.jimmer.apt.Context;
import org.babyfish.jimmer.client.Description;
import org.babyfish.jimmer.client.meta.Doc;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import java.util.HashMap;
import java.util.Map;

public class DocMetadata {

    private final Context ctx;

    private final Map<Element, String> docMap = new HashMap<>();

    private final Map<TypeElement, Map<String, String>> draftDocMap = new HashMap<>();

    public DocMetadata(Context ctx) {
        this.ctx = ctx;
    }

    private static String decapitalizeFirst(String value) {
        return Character.toLowerCase(value.charAt(0)) + value.substring(1);
    }

    public Doc getDoc(Element element) {
        String docString = getString(element);
        if (docString != null) {
            return Doc.parse(docString);
        }
        return null;
    }

    public String getString(Element element) {
        String docString = getStringImpl(element);
        if (docString.isEmpty()) {
            return null;
        }
        return docString;
    }

    private String getStringImpl(Element element) {
        String docString = docMap.get(element);
        if (docString != null) {
            return docString;
        }

        docString = ctx.getElements().getDocComment(element);
        if (docString != null && !docString.isEmpty()) {
            docMap.put(element, docString);
            return docString;
        }

        Description description = element.getAnnotation(Description.class);
        if (description != null) {
            String value = description.value();
            if (!value.isEmpty()) {
                docMap.put(element, value);
                return value;
            }
        }

        TypeElement typeElement;
        String key;
        if (element instanceof ExecutableElement) {
            typeElement = (TypeElement) (element).getEnclosingElement();
            ExecutableElement executableElement = (ExecutableElement) element;
            key = propName(executableElement);
        } else if (element instanceof TypeElement) {
            typeElement = (TypeElement) element;
            key = "";
        } else {
            typeElement = null;
            key = null;
        }
        if (typeElement != null) {
            String value = draftDocStringMap(typeElement).get(key);
            if (value != null && !value.isEmpty()) {
                docMap.put(element, value);
                return value;
            }
        }
        docMap.put(element, "");
        return "";
    }

    private String propName(ExecutableElement executableElement) {
        String methodName = executableElement.getSimpleName().toString();
        if (!ctx.keepIsPrefix() &&
                executableElement.getReturnType().getKind() == TypeKind.BOOLEAN &&
                methodName.startsWith("is") &&
                methodName.length() > 2 &&
                Character.isUpperCase(methodName.charAt(2))) {
            return decapitalizeFirst(methodName.substring(2));
        }
        if (methodName.startsWith("get") &&
                methodName.length() > 3 &&
                Character.isUpperCase(methodName.charAt(3))) {
            return decapitalizeFirst(methodName.substring(3));
        }
        return methodName;
    }

    private Map<String, String> draftDocStringMap(TypeElement typeElement) {
        Map<String, String> cachedMap = draftDocMap.get(typeElement);
        if (cachedMap != null) {
            return cachedMap;
        }
        TypeElement draftElement = ctx.getElements().getTypeElement(
                typeElement.getQualifiedName() + "Draft"
        );
        Map<String, String> map = new HashMap<>();
        if (draftElement != null) {
            Description description = draftElement.getAnnotation(Description.class);
            if (description != null && !description.value().isEmpty()) {
                map.put("", description.value());
            }
            for (Element element : draftElement.getEnclosedElements()) {
                if (!(element instanceof ExecutableElement)) {
                    continue;
                }
                ExecutableElement executableElement = (ExecutableElement) element;
                String methodName = executableElement.getSimpleName().toString();
                if (!methodName.startsWith("set") ||
                        methodName.length() == 3 ||
                        executableElement.getParameters().size() != 1) {
                    continue;
                }
                description = executableElement.getAnnotation(Description.class);
                if (description != null && !description.value().isEmpty()) {
                    String suffix = methodName.substring(3);
                    map.put(suffix, description.value());
                    map.put(decapitalizeFirst(suffix), description.value());
                }
            }
        }
        draftDocMap.put(typeElement, map);
        return map;
    }
}
