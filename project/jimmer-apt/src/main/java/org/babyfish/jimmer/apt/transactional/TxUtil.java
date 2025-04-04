package org.babyfish.jimmer.apt.transactional;

import org.babyfish.jimmer.apt.Context;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;

public class TxUtil {

    private TxUtil() {}

    static final String TX = "org.babyfish.jimmer.sql.transaction.Tx";

    static final String TARGET_ANNOTATION = "org.babyfish.jimmer.sql.transaction.TargetAnnotation";

    public static AnnotationMirror tx(Context ctx, Element element) {
        return annotation(ctx, element, TX);
    }

    public static AnnotationMirror targetAnnotation(Context ctx, Element element) {
        return annotation(ctx, element, TARGET_ANNOTATION);
    }

    private static AnnotationMirror annotation(Context ctx, Element element, String className) {
        for (AnnotationMirror annotationMirror : element.getAnnotationMirrors()) {
            TypeElement annotationElement =
                    (TypeElement) ctx.getTypes().asElement(annotationMirror.getAnnotationType());
            if (annotationElement.getQualifiedName().toString().equals(className)) {
                return annotationMirror;
            }
        }
        return null;
    }
}
