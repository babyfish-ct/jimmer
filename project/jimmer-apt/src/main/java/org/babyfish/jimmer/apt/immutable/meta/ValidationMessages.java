package org.babyfish.jimmer.apt.immutable.meta;

import com.squareup.javapoet.ClassName;
import org.babyfish.jimmer.apt.MetaException;

import javax.lang.model.element.*;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.*;

public class ValidationMessages {

    public static final String JAVAX_CONSTRAINT_FULL_NAME = "javax.validation.Constraint";

    public static final String JAKARTA_CONSTRAINT_FULL_NAME = "jakarta.validation.Constraint";

    private ValidationMessages() {}

    public static Map<ClassName, String> parseMessageMap(Element element) {
        Map<ClassName, String> map = new LinkedHashMap<>();
        for (AnnotationMirror annotationMirror : element.getAnnotationMirrors()) {
            if (hasConstraint((TypeElement) annotationMirror.getAnnotationType().asElement())) {
                TypeElement typeElement = (TypeElement) annotationMirror.getAnnotationType().asElement();
                ClassName className = ClassName.get(
                        ((PackageElement)typeElement.getEnclosingElement()).getQualifiedName().toString(),
                        typeElement.getSimpleName().toString()
                );
                if (map.containsKey(className)) {
                    throw new MetaException(
                            element,
                            "duplicate annotations \"" + className + "\""
                    );
                }
                String message = "";
                for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> e :
                        annotationMirror.getElementValues().entrySet()) {
                    if (e.getKey().getSimpleName().toString().equals("message")) {
                        Object msg = e.getValue().getValue();
                        if (msg instanceof String) {
                            message = (String) msg;
                        }
                        break;
                    }
                }
                map.put(className, message);
            }
        }
        return Collections.unmodifiableMap(map);
    }

    private static boolean hasConstraint(TypeElement element) {
        for (AnnotationMirror mirror : element.getAnnotationMirrors()) {
            TypeElement annoElement = (TypeElement) mirror.getAnnotationType().asElement();
            if (annoElement.getQualifiedName().toString().equals(JAKARTA_CONSTRAINT_FULL_NAME) ||
            annoElement.getQualifiedName().toString().equals(JAVAX_CONSTRAINT_FULL_NAME)) {
                Retention retention = element.getAnnotation(Retention.class);
                if (retention == null || retention.value() != RetentionPolicy.RUNTIME) {
                    throw new MetaException(
                            element,
                            "the annotation @" +
                                    element.getQualifiedName().toString() +
                                    " is decorated by @" +
                                    annoElement.getQualifiedName() +
                                    " but its retention is not runtime"
                    );
                }
                for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> e :
                    mirror.getElementValues().entrySet()) {
                    if (e.getKey().getSimpleName().toString().equals("validatedBy")) {
                        return !((Collection<?>)e.getValue().getValue()).isEmpty();
                    }
                }
                break;
            }
        }
        return false;
    }
}
