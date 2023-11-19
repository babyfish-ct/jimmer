package org.babyfish.jimmer.apt.util;

import org.babyfish.jimmer.apt.MetaException;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class RecursiveAnnotations {

    private RecursiveAnnotations() {}

    public static AnnotationMirror of(ExecutableElement element, Class<?> annotationType) {
        VisitContext ctx = new VisitContext(element, annotationType.getName());
        for (AnnotationMirror mirror : element.getAnnotationMirrors()) {
            collectAnnotationTypes(mirror, ctx);
        }
        return ctx.annotation;
    }

    private static void collectAnnotationTypes(AnnotationMirror annotation, VisitContext ctx) {
        TypeElement element = (TypeElement) annotation.getAnnotationType().asElement();
        String qualifiedName = element.getQualifiedName().toString();
        if (qualifiedName.equals(ctx.annotationName)) {
            ctx.set(annotation);
            return;
        }
        if (!ctx.push(qualifiedName)) {
            return;
        }
        for (AnnotationMirror subMirror : element.getAnnotationMirrors()) {
            collectAnnotationTypes(subMirror, ctx);
        }
        ctx.pop();
    }

    private static class VisitContext {

        private final ExecutableElement element;

        final String annotationName;

        private final LinkedList<String> stack = new LinkedList<>();

        private ArrayList<String> path;

        AnnotationMirror annotation;

        private VisitContext(ExecutableElement element, String annotationName) {
            this.element = element;
            this.annotationName = annotationName;
        }

        public boolean push(String qualifiedName) {
            if (stack.contains(qualifiedName)) {
                return false;
            }
            stack.push(qualifiedName);
            return true;
        }

        public void pop() {
            stack.pop();
        }

        public void set(AnnotationMirror annotation) {
            if (this.annotation != null && !this.annotation.equals(annotation)) {
                throw new MetaException(
                        element,
                        "Conflict annotation \"@" +
                                annotationName +
                                "\" one " +
                                declared(path) +
                                " and the other one " +
                                declared(stack)
                );
            }
            this.annotation = annotation;
            this.path = new ArrayList<>(stack);
        }

        private static String declared(List<String> path) {
            if (path.isEmpty()) {
                return "is declared directly";
            }
            return "is declared as nest annotation of " + path;
        }
    }
}
