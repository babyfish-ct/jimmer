package org.babyfish.jimmer.apt;

import org.babyfish.jimmer.apt.generator.DraftGenerator;
import org.babyfish.jimmer.apt.generator.FetcherGenerator;
import org.babyfish.jimmer.apt.generator.PropsGenerator;
import org.babyfish.jimmer.apt.generator.TableGenerator;
import org.babyfish.jimmer.apt.meta.ImmutableType;
import org.babyfish.jimmer.apt.meta.MetaException;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Types;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@SupportedAnnotationTypes({
        "org.babyfish.jimmer.Immutable",
        "org.babyfish.jimmer.sql.Entity",
        "org.babyfish.jimmer.sql.MappedSuperclass"
})
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class ImmutableProcessor extends AbstractProcessor {

    private TypeUtils typeUtils;

    private Filer filer;

    private String[] includes = null;

    private String[] excludes = null;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        String includes = processingEnv.getOptions().get("jimmer.source.includes");
        String excludes = processingEnv.getOptions().get("jimmer.source.excludes");
        if (includes != null && !includes.isEmpty()) {
            this.includes = includes.trim().split("\\s*,\\s*");
        }
        if (excludes != null && !excludes.isEmpty()) {
            this.excludes = excludes.trim().split("\\s*,\\s*");
        }
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
                String qualifiedName = typeElement.getQualifiedName().toString();
                if (includes != null) {
                    boolean matched = false;
                    for (String include : includes) {
                        if (qualifiedName.startsWith(include)) {
                            matched = true;
                            break;
                        }
                    }
                    if (!matched) {
                        continue;
                    }
                }
                if (excludes != null) {
                    boolean matched = false;
                    for (String exclude : excludes) {
                        if (qualifiedName.startsWith(exclude)) {
                            matched = true;
                            break;
                        }
                    }
                    if (matched) {
                        continue;
                    }
                }
                if (typeUtils.isImmutable(typeElement)) {
                    if (typeElement.getKind() != ElementKind.INTERFACE) {
                        throw new MetaException(
                                "Illegal class \"" +
                                        qualifiedName +
                                        "\", immutable type must be interface"
                        );
                    }
                    ImmutableType immutableType = typeUtils.getImmutableType(typeElement);
                    new DraftGenerator(
                            typeUtils,
                            immutableType,
                            filer
                    ).generate();
                    new PropsGenerator(
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
                        new FetcherGenerator(
                                typeUtils,
                                immutableType,
                                filer
                        ).generate();
                    }
                }
            }
        }
        return true;
    }
}
