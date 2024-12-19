package org.babyfish.jimmer.apt.util;

import com.squareup.javapoet.AnnotationSpec;

public class SuppressAnnotation {
    private SuppressAnnotation() {
    }

    public static AnnotationSpec suppressAllAnnotation() {
        return AnnotationSpec
                .builder(SuppressWarnings.class)
                .addMember("value", "$S", "all")
                .build();
    }
}
