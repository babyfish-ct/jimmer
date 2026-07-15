package org.babyfish.jimmer.meta.impl;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ModelException;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

class AnnotationUtils {
    static <A extends Annotation> A getAnnotation(ImmutableProp prop, Class<A> annotationType) {
        AnnotationSearchContext<A> ctx = new AnnotationSearchContext<>(prop, annotationType);
        for (Annotation annotation : prop.getAnnotations()) {
            collectAnnotationType(annotation, ctx);
        }
        return ctx.getAnnotation();
    }

    @SuppressWarnings("unchecked")
    private static <A extends Annotation> void collectAnnotationType(
            Annotation annotation,
            AnnotationSearchContext<A> ctx
    ) {
        Class<? extends Annotation> curAnnotationType = annotation.annotationType();
        if (curAnnotationType == ctx.annotationType) {
            ctx.set((A) annotation);
            return;
        }
        if (!ctx.push(curAnnotationType)) {
            return;
        }
        for (Annotation deeperAnnotation : curAnnotationType.getAnnotations()) {
            collectAnnotationType(deeperAnnotation, ctx);
        }
        ctx.pop();
    }

    private static class AnnotationSearchContext<A extends Annotation> {

        private final ImmutableProp prop;

        final Class<A> annotationType;

        private LinkedList<Class<? extends Annotation>> pathStack = new LinkedList<>();

        private ArrayList<Class<? extends Annotation>> path;

        private A annotation;

        private AnnotationSearchContext(ImmutableProp prop, Class<A> annotationType) {
            this.prop = prop;
            this.annotationType = annotationType;
        }

        public boolean push(Class<? extends Annotation> annotationType) {
            if (pathStack.contains(annotationType)) {
                return false;
            }
            pathStack.push(annotationType);
            return true;
        }

        public void pop() {
            pathStack.pop();
        }

        public A getAnnotation() {
            return annotation;
        }

        public void set(A annotation) {
            if (this.annotation != null && !this.annotation.equals(annotation)) {
                throw new ModelException(
                        "Illegal property \"" +
                                prop +
                                "\", conflict annotation \"@" +
                                annotation.annotationType().getName() +
                                "\", one " +
                                declaredPath(path) +
                                " and the other one " +
                                declaredPath(pathStack)
                );
            }
            if (this.annotation == null) {
                path = new ArrayList<>(pathStack);
                this.annotation = annotation;
            }
        }

        private static String declaredPath(List<Class<? extends Annotation>> path) {
            if (path.isEmpty()) {
                return "is declared directly";
            }
            return "is declared as nest annotation: " + path;
        }
    }
}
