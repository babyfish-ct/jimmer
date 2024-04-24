package org.babyfish.jimmer.apt.util;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import org.babyfish.jimmer.apt.immutable.generator.Constants;
import org.babyfish.jimmer.apt.immutable.meta.ImmutableType;
import org.babyfish.jimmer.dto.compiler.DtoFile;

public class GeneratedAnnotation {
    private GeneratedAnnotation() {
    }

    public static AnnotationSpec generatedAnnotation() {
        return AnnotationSpec
                .builder(Constants.GENERATED_BY_CLASS_NAME)
                .build();
    }

    public static AnnotationSpec generatedAnnotation(ClassName className) {
        return AnnotationSpec
                .builder(Constants.GENERATED_BY_CLASS_NAME)
                .addMember("type", "$T.class", className)
                .build();
    }

    public static AnnotationSpec generatedAnnotation(ImmutableType type) {
        return generatedAnnotation(type.getClassName());
    }

    public static AnnotationSpec generatedAnnotation(DtoFile dtoFile) {
        return AnnotationSpec
                .builder(Constants.GENERATED_BY_CLASS_NAME)
                .addMember(
                        "file",
                        "$S",
                        dtoFile.getPath()
                )
                .build();
    }
}
