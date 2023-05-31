package org.babyfish.jimmer.apt;

import org.babyfish.jimmer.apt.generator.*;
import org.babyfish.jimmer.apt.meta.ImmutableType;
import org.babyfish.jimmer.apt.meta.MetaException;
import org.babyfish.jimmer.error.ErrorFamily;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import java.util.*;
import java.util.stream.Collectors;

@SupportedAnnotationTypes({
        "org.babyfish.jimmer.Immutable",
        "org.babyfish.jimmer.sql.Entity",
        "org.babyfish.jimmer.sql.MappedSuperclass",
        "org.babyfish.jimmer.error.ErrorFamily"
})
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class ImmutableProcessor extends AbstractProcessor {

    private Context context;

    private Filer filer;

    private String[] includes = null;

    private String[] excludes = null;

    private Messager messager;

    private final Set<TypeElement> processedTypeElements =
            new TreeSet<>(Comparator.comparing(it -> it.getQualifiedName().toString()));

    private boolean jimmerModuleGenerated;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        messager = processingEnv.getMessager();
        String includes = processingEnv.getOptions().get("jimmer.source.includes");
        String excludes = processingEnv.getOptions().get("jimmer.source.excludes");
        if (includes != null && !includes.isEmpty()) {
            this.includes = includes.trim().split("\\s*,\\s*");
        }
        if (excludes != null && !excludes.isEmpty()) {
            this.excludes = excludes.trim().split("\\s*,\\s*");
        }
        context = new Context(
                processingEnv.getElementUtils(),
                processingEnv.getTypeUtils(),
                "true".equals(processingEnv.getOptions().get("jimmer.keepIsPrefix"))
        );
        filer = processingEnv.getFiler();
    }

    @Override
    public boolean process(
            Set<? extends TypeElement> annotations,
            RoundEnvironment roundEnv
    ) {
        boolean go = false;
        for (Element element : roundEnv.getRootElements()) {
            if (element instanceof TypeElement) {
                if (processedTypeElements.add((TypeElement) element)) {
                    go = true;
                }
            }
        }
        if (!go) {
            return true;
        }

        try {
            Map<TypeElement, ImmutableType> immutableTypeMap = parseImmutableTypes(roundEnv);
            generateJimmerTypes(
                    roundEnv.getRootElements()
                            .stream()
                            .filter(it -> it instanceof TypeElement)
                            .map(immutableTypeMap::get)
                            .filter(Objects::nonNull)
                            .collect(Collectors.toList()),
                    roundEnv
            );

            List<TypeElement> errorElements = getErrorFamilies(roundEnv);
            generateErrorType(errorElements);
        } catch (MetaException ex) {
            messager.printMessage(Diagnostic.Kind.ERROR, ex.getMessage(), ex.getElement());
        }

        return true;
    }

    private Map<TypeElement, ImmutableType> parseImmutableTypes(RoundEnvironment roundEnv) {
        Map<TypeElement, ImmutableType> map = new HashMap<>();
        for (Element element : roundEnv.getRootElements()) {
            if (element instanceof TypeElement) {
                TypeElement typeElement = (TypeElement) element;
                if (context.isImmutable(typeElement) && include(typeElement)) {
                    if (typeElement.getKind() != ElementKind.INTERFACE) {
                        throw new MetaException(
                                typeElement,
                                "immutable type must be interface"
                        );
                    }
                    ImmutableType immutableType = context.getImmutableType(typeElement);
                    map.put(typeElement, immutableType);
                }
            }
        }
        for (int step = 0; step < 4; step++) {
            int ctxSize = context.getImmutableTypes().size();
            do {
                for (ImmutableType type : context.getImmutableTypes()) {
                    type.resolve(context, step);
                }
            } while (ctxSize < context.getImmutableTypes().size());
        }
        return map;
    }

    private List<TypeElement> getErrorFamilies(RoundEnvironment roundEnv) {
        List<TypeElement> typeElements = new ArrayList<>();
        for (Element element : roundEnv.getRootElements()) {
            if (element instanceof TypeElement && include((TypeElement) element)) {
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

    private boolean include(TypeElement typeElement) {
        String qualifiedName = typeElement.getQualifiedName().toString();
        if (includes != null) {
            boolean matched = false;
            for (String include : includes) {
                if (qualifiedName.startsWith(include)) {
                    return true;
                }
            }
        }
        if (excludes != null) {
            for (String exclude : excludes) {
                if (qualifiedName.startsWith(exclude)) {
                    return false;
                }
            }
        }
        return true;
    }

    private void generateJimmerTypes(Collection<ImmutableType> immutableTypes, RoundEnvironment roundEnv) {
        for (ImmutableType immutableType : immutableTypes) {
            new DraftGenerator(
                    immutableType,
                    filer
            ).generate();
            new PropsGenerator(
                    context,
                    immutableType,
                    filer
            ).generate();
            messager.printMessage(Diagnostic.Kind.NOTE, "Immutable: " + immutableType.getQualifiedName());
            if (immutableType.isEntity()) {
                messager.printMessage(Diagnostic.Kind.NOTE, "Entity: " + immutableType.getQualifiedName());
                new TableGenerator(
                        context,
                        immutableType,
                        false,
                        filer
                ).generate();
                new TableGenerator(
                        context,
                        immutableType,
                        true,
                        filer
                ).generate();
                new FetcherGenerator(
                        context,
                        immutableType,
                        filer
                ).generate();
            } else if (immutableType.isEmbeddable()) {
                new PropExpressionGenerator(
                        context,
                        immutableType,
                        filer
                ).generate();
            }
        }
        if (!jimmerModuleGenerated) {
            new JimmerModuleGenerator(
                    processedTypeElements,
                    filer
            ).generate();
            jimmerModuleGenerated = true;
        }
    }

    private void generateErrorType(List<TypeElement> typeElements) {
        for (TypeElement typeElement : typeElements) {
            new ErrorGenerator(typeElement, filer).generate();
        }
    }
}
