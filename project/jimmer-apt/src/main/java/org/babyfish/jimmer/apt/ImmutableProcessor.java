package org.babyfish.jimmer.apt;

import org.babyfish.jimmer.apt.generator.DraftGenerator;
import org.babyfish.jimmer.apt.generator.TableGenerator;
import org.babyfish.jimmer.apt.meta.ImmutableType;
import org.babyfish.jimmer.apt.meta.MetaException;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Types;
import java.util.Set;

@SupportedAnnotationTypes({"org.babyfish.jimmer.Immutable", "javax.persistence.Entity"})
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class ImmutableProcessor extends AbstractProcessor {

    private TypeUtils typeUtils;

    private Filer filer;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        typeUtils = new TypeUtils(
                processingEnv.getElementUtils(),
                processingEnv.getTypeUtils()
        );
        filer = processingEnv.getFiler();
    }

    @Override
    public boolean process(
            Set<? extends TypeElement> annotations,
            RoundEnvironment roundEnv
    ) {
        for (Element element : roundEnv.getRootElements()) {
            if (element instanceof TypeElement) {
                TypeElement typeElement = (TypeElement)element;
                if (typeUtils.isImmutable(typeElement)) {
                    if (typeElement.getKind() != ElementKind.INTERFACE) {
                        throw new MetaException(
                                "Illegal class \"" +
                                        typeElement.getQualifiedName().toString() +
                                        "\", immutable type must be interface"
                        );
                    }
                    if (typeUtils.isImmutable(typeElement)) {
                        ImmutableType immutableType = typeUtils.getImmutableType(typeElement);
                        new DraftGenerator(
                                typeUtils,
                                immutableType,
                                filer
                        ).generate();
                        if (immutableType.isEntity()) {
                            new TableGenerator(
                                    typeUtils,
                                    immutableType,
                                    false,
                                    filer
                            ).generate();
                            new TableGenerator(
                                    typeUtils,
                                    immutableType,
                                    true,
                                    filer
                            ).generate();
                        }
                    }
                }
            }
        }
        return true;
    }
}
