package org.babyfish.jimmer.apt.generator;

import org.babyfish.jimmer.apt.meta.ImmutableProp;

import javax.lang.model.element.*;
import java.util.*;

public class Annotations {

    private static final String JAVAX_PREFIX = "javax.validation.constraints.";

    private static final String JAKARTA_PREFIX = "jakarta.validation.constraints.";

    private Annotations() {}

    public static Map<String, List<AnnotationMirror>> validateAnnotationMirrorMultiMap(ImmutableProp prop) {
        Map<String, List<AnnotationMirror>> mirrorMultiMap = new HashMap<>();
        for (AnnotationMirror mirror : prop.getAnnotations()) {
            Element element = mirror.getAnnotationType().asElement();
            if (element instanceof TypeElement) {
                String qualifiedName = ((TypeElement)element).getQualifiedName().toString();
                if (qualifiedName.startsWith(JAVAX_PREFIX)) {
                    String name = qualifiedName.substring(JAVAX_PREFIX.length());
                    mirrorMultiMap.computeIfAbsent(name, it -> new ArrayList<>()).add(mirror);
                } else if (qualifiedName.startsWith(JAKARTA_PREFIX)) {
                    String name = qualifiedName.substring(JAKARTA_PREFIX.length());
                    mirrorMultiMap.computeIfAbsent(name, it -> new ArrayList<>()).add(mirror);
                }
            }
        }
        return mirrorMultiMap;
    }

    public static String qualifiedName(AnnotationMirror mirror) {
        return ((TypeElement)mirror.getAnnotationType().asElement()).getQualifiedName().toString();
    }

    @SuppressWarnings("unchecked")
    public static <T> T annotationValue(AnnotationMirror mirror, String name, T defaultValue) {
        for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> e : mirror.getElementValues().entrySet()) {
            if (e.getKey().getSimpleName().toString().equals(name)) {
                return (T)e.getValue().getValue();
            }
        }
        return defaultValue;
    }

    public static <T> List<T> nonNullList(List<T> list) {
        if (list == null) {
            return Collections.emptyList();
        }
        return list;
    }
}
