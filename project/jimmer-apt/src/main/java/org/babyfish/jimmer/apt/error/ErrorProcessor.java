package org.babyfish.jimmer.apt.error;

import org.babyfish.jimmer.apt.Context;
import org.babyfish.jimmer.apt.MetaException;
import org.babyfish.jimmer.error.ErrorFamily;

import javax.annotation.processing.Filer;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import java.util.ArrayList;
import java.util.List;

public class ErrorProcessor {

    private final Context context;

    private final Filer filer;

    private final boolean checkedException;

    public ErrorProcessor(Context context, boolean checkedException, Filer filer) {
        this.context = context;
        this.filer = filer;
        this.checkedException = checkedException;
    }

    public boolean process(RoundEnvironment roundEnv) {
        List<TypeElement> errorElements = getErrorFamilies(roundEnv);
        generateErrorType(errorElements);
        return !errorElements.isEmpty();
    }

    private List<TypeElement> getErrorFamilies(RoundEnvironment roundEnv) {
        List<TypeElement> typeElements = new ArrayList<>();
        for (Element element : roundEnv.getRootElements()) {
            if (element instanceof TypeElement && context.include((TypeElement) element)) {
                if (element.getAnnotation(ErrorFamily.class) != null) {
                    if (element.getKind() != ElementKind.ENUM) {
                        throw new MetaException(
                                element,
                                "only enum can be decorated by @" +
                                        ErrorFamily.class.getName()
                        );
                    }
                    typeElements.add((TypeElement) element);
                }
            }
        }
        return typeElements;
    }

    private void generateErrorType(List<TypeElement> typeElements) {
        for (TypeElement typeElement : typeElements) {
            new ErrorGenerator(context, typeElement, checkedException, filer).generate();
        }
    }
}
