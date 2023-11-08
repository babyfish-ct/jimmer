package org.babyfish.jimmer.apt;

import org.babyfish.jimmer.apt.dto.AptDtoCompiler;
import org.babyfish.jimmer.apt.dto.DtoContext;
import org.babyfish.jimmer.apt.dto.DtoException;
import org.babyfish.jimmer.apt.generator.*;
import org.babyfish.jimmer.apt.meta.ImmutableProp;
import org.babyfish.jimmer.apt.meta.ImmutableType;
import org.babyfish.jimmer.dto.compiler.DtoAstException;
import org.babyfish.jimmer.dto.compiler.DtoFile;
import org.babyfish.jimmer.dto.compiler.DtoType;
import org.babyfish.jimmer.dto.compiler.DtoUtils;
import org.babyfish.jimmer.error.ErrorFamily;
import org.babyfish.jimmer.sql.Entity;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;
import java.util.*;
import java.util.stream.Collectors;

@SupportedAnnotationTypes({
        "org.babyfish.jimmer.Immutable",
        "org.babyfish.jimmer.sql.Entity",
        "org.babyfish.jimmer.sql.MappedSuperclass",
        "org.babyfish.jimmer.sql.Embeddable",
        "org.babyfish.jimmer.error.ErrorFamily"
})
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class JimmerProcessor extends AbstractProcessor {

    private Context context;

    private Filer filer;

    private String[] includes = null;

    private String[] excludes = null;

    private Messager messager;

    private final Set<TypeElement> processedTypeElements =
            new TreeSet<>(Comparator.comparing(it -> it.getQualifiedName().toString()));

    private boolean jimmerModuleGenerated;

    private Collection<String> dtoDirs;

    private Elements elements;

    private final Set<String> processedDtoPaths = new HashSet<>();

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        messager = processingEnv.getMessager();
        String includes = processingEnv.getOptions().get("jimmer.source.includes");
        String excludes = processingEnv.getOptions().get("jimmer.source.excludes");
        String dtoDirs = processingEnv.getOptions().get("jimmer.dto.dirs");
        if (includes != null && !includes.isEmpty()) {
            this.includes = includes.trim().split("\\s*,\\s*");
        }
        if (excludes != null && !excludes.isEmpty()) {
            this.excludes = excludes.trim().split("\\s*,\\s*");
        }
        if (dtoDirs != null && !dtoDirs.isEmpty()) {
            Set<String> dirs = new LinkedHashSet<>();
            for (String path : dtoDirs.trim().split("\\*[,:;]\\s*")) {
                if (path.isEmpty() || path.equals("/")) {
                    continue;
                }
                if (path.startsWith("/")) {
                    path = path.substring(1);
                }
                if (path.endsWith("/")) {
                    path = path.substring(0, path.length() - 1);
                }
                if (!path.isEmpty()) {
                    dirs.add(path);
                }
            }
            this.dtoDirs = DtoUtils.standardDtoDirs(dirs);
        } else {
            this.dtoDirs = Collections.singletonList("src/main/dto");
        }
        context = new Context(
                processingEnv.getElementUtils(),
                processingEnv.getTypeUtils(),
                "true".equals(processingEnv.getOptions().get("jimmer.keepIsPrefix"))
        );
        filer = processingEnv.getFiler();
        elements = processingEnv.getElementUtils();
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
            Map<TypeElement, ImmutableType> immutableTypeMap =
                    parseImmutableTypes(roundEnv);
            generateJimmerTypes(
                    roundEnv.getRootElements()
                            .stream()
                            .filter(it -> it instanceof TypeElement)
                            .map(immutableTypeMap::get)
                            .filter(Objects::nonNull)
                            .collect(Collectors.toList()),
                    roundEnv
            );

            Map<ImmutableType, List<DtoType<ImmutableType, ImmutableProp>>> dtoTypeMap =
                    parseDtoTypes(immutableTypeMap);
            generateDtoTypes(dtoTypeMap);

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
        return map;
    }
    
    private Map<ImmutableType, List<DtoType<ImmutableType, ImmutableProp>>> parseDtoTypes(
            Map<TypeElement, ImmutableType> immutableTypeMap
    ) {
        Map<TypeElement, ImmutableType> typeMap = new HashMap<>(immutableTypeMap);
        Map<ImmutableType, List<DtoType<ImmutableType, ImmutableProp>>> dtoTypeMap = new LinkedHashMap<>();
        DtoContext dtoContext = new DtoContext(filer, dtoDirs);
        AptDtoCompiler compiler;

        for (DtoFile dtoFile : dtoContext.getDtoFiles()) {
            if (!processedDtoPaths.add(dtoFile.getPath())) {
                continue;
            }
            try {
                compiler = new AptDtoCompiler(dtoFile);
            } catch (DtoAstException ex) {
                throw new DtoException(
                        "Failed to parse \"" +
                                dtoFile.getPath() +
                                "\": " +
                                ex.getMessage(),
                        ex
                );
            } catch (Throwable ex) {
                throw new DtoException(
                        "Failed to read \"" +
                                dtoFile.getPath() +
                                "\": " +
                                ex.getMessage(),
                        ex
                );
            }
            TypeElement typeElement = elements.getTypeElement(compiler.getSourceTypeName());
            if (typeElement == null) {
                throw new DtoException(
                        "Failed to parse \"" +
                                dtoFile.getPath() +
                                "\": No entity type \"" +
                                compiler.getSourceTypeName() +
                                "\""
                );
            }
            if (typeElement.getAnnotation(Entity.class) == null) {
                throw new DtoException(
                        "Failed to parse \"" +
                                dtoFile.getPath() +
                                "\": the \"" +
                                compiler.getSourceTypeName() +
                                "\" is not decorated by \"@" +
                                Entity.class.getName() +
                                "\""
                );
            }
            ImmutableType immutableType = typeMap.get(typeElement);
            if (immutableType == null) {
                immutableType = context.getImmutableType(typeElement);
                typeMap.put(typeElement, immutableType);
            }
            dtoTypeMap
                    .computeIfAbsent(immutableType, it -> new ArrayList<>())
                    .addAll(compiler.compile(immutableType));
        }
        return dtoTypeMap;
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

    private void generateDtoTypes(Map<?, List<DtoType<ImmutableType, ImmutableProp>>> dtoTypeMap) {
        for (List<DtoType<ImmutableType, ImmutableProp>> dtoTypes : dtoTypeMap.values()) {
            for (DtoType<ImmutableType, ImmutableProp> dtoType : dtoTypes) {
                new DtoGenerator(dtoType, filer).generate();
            }
        }
    }
}
