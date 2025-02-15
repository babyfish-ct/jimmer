package org.babyfish.jimmer.apt.client;

import org.babyfish.jimmer.apt.Context;
import org.babyfish.jimmer.client.Description;
import org.babyfish.jimmer.client.meta.Doc;
import org.babyfish.jimmer.impl.util.StringUtil;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class DocMetadata {

    private final Context ctx;

    private final Map<Element, String> docMap = new HashMap<>();

    public DocMetadata(Context ctx) {
        this.ctx = ctx;
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
        if (element instanceof ExecutableElement) {
            typeElement = (TypeElement) (element).getEnclosingElement();
        } else if (element instanceof TypeElement) {
            typeElement = (TypeElement) element;
        } else {
            typeElement = null;
        }
        if (typeElement != null) {
            Map<String, String> map = implDocStringMap(typeElement);
            if (!map.isEmpty()) {
                docMap.put(typeElement, map.get(""));
                addPropDocs(typeElement, map);
            }
        }
        String value = docMap.get(element);
        if (value == null) {
            docMap.put(element, "");
            return "";
        }
        return value;
    }

    private void addPropDocs(TypeElement typeElement, Map<String, String> map) {
        for (Element element : typeElement.getEnclosedElements()) {
            if (!(element instanceof ExecutableElement)) {
                continue;
            }
            ExecutableElement executableElement = (ExecutableElement) element;
            if (executableElement.getReturnType().getKind() != TypeKind.VOID &&
            executableElement.getTypeParameters().isEmpty() &&
            executableElement.getParameters().isEmpty()) {
                docMap.put(
                        element,
                        map.getOrDefault(executableElement.getSimpleName().toString(), "")
                );
            }
        }
        for (TypeMirror superType : typeElement.getInterfaces()) {
            TypeElement superElement = (TypeElement) ctx.getTypes().asElement(superType);
            addPropDocs(superElement, map);
        }
    }

    private Map<String, String> implDocStringMap(TypeElement typeElement) {
        TypeElement implElement = ctx.getElements().getTypeElement(
                typeElement.getQualifiedName() + "Draft"
        );
        if (implElement == null) {
            return Collections.emptyMap();
        }
        implElement = (TypeElement) implElement
                .getEnclosedElements()
                .stream()
                .filter(it -> it instanceof TypeElement && "Producer".equals(it.getSimpleName().toString()))
                .findFirst()
                .orElse(null);
        if (implElement == null) {
            return Collections.emptyMap();
        }
        implElement = (TypeElement) implElement
                .getEnclosedElements()
                .stream()
                .filter(it -> it instanceof TypeElement && "Impl".equals(it.getSimpleName().toString()))
                .findFirst()
                .orElse(null);
        if (implElement == null) {
            return Collections.emptyMap();
        }
        Map<String, String> map = new HashMap<>();
        Description description = implElement.getAnnotation(Description.class);
        if (description != null && !description.value().isEmpty()) {
            map.put("", description.value());
        }
        for (Element element : implElement.getEnclosedElements()) {
            if (!(element instanceof ExecutableElement)) {
                continue;
            }
            ExecutableElement executableElement = (ExecutableElement) element;
            if (executableElement.getReturnType().getKind() == TypeKind.VOID) {
                continue;
            }
            if (!executableElement.getParameters().isEmpty() || !executableElement.getTypeParameters().isEmpty()) {
                continue;
            }
            description = executableElement.getAnnotation(Description.class);
            if (description == null || description.value().isEmpty()) {
                continue;
            }
            String propName = StringUtil.propName(
                    executableElement.getSimpleName().toString(),
                    executableElement.getReturnType().getKind() == TypeKind.BOOLEAN
            );
            if (propName == null) {
                propName = executableElement.getSimpleName().toString();
            }
            map.put(propName, description.value());
        }
        return map;
    }
}
