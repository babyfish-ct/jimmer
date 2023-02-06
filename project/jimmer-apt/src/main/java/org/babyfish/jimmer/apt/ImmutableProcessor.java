package org.babyfish.jimmer.apt;

import org.babyfish.jimmer.Immutable;
import org.babyfish.jimmer.apt.generator.*;
import org.babyfish.jimmer.apt.meta.ImmutableProp;
import org.babyfish.jimmer.apt.meta.ImmutableType;
import org.babyfish.jimmer.apt.meta.MetaException;
import org.babyfish.jimmer.dto.compiler.DtoAstException;
import org.babyfish.jimmer.dto.compiler.DtoType;
import org.babyfish.jimmer.error.ErrorFamily;
import org.babyfish.jimmer.sql.Embeddable;
import org.babyfish.jimmer.sql.Entity;
import org.babyfish.jimmer.sql.MappedSuperclass;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.tools.StandardLocation;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@SupportedAnnotationTypes({
        "org.babyfish.jimmer.Immutable",
        "org.babyfish.jimmer.sql.Entity",
        "org.babyfish.jimmer.sql.MappedSuperclass",
        "org.babyfish.jimmer.error.ErrorFamily"
})
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class ImmutableProcessor extends AbstractProcessor {

    private TypeUtils typeUtils;

    private Filer filer;

    private String[] includes = null;

    private String[] excludes = null;

    private Messager messager;

    private boolean processed;

    private Set<String> dtoDirs;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        messager = processingEnv.getMessager();
        String includes = processingEnv.getOptions().get("jimmer.source.includes");
        String excludes = processingEnv.getOptions().get("jimmer.source.excludes");
        String dtoDirs = processingEnv.getOptions().get("jimmer.dtoDirs");
        if (includes != null && !includes.isEmpty()) {
            this.includes = includes.trim().split("\\s*,\\s*");
        }
        if (excludes != null && !excludes.isEmpty()) {
            this.excludes = excludes.trim().split("\\s*,\\s*");
        }
        if (dtoDirs != null && !dtoDirs.isEmpty()) {
            Set<String> dirs = new LinkedHashSet<>();
            for (String path : dtoDirs.trim().split("\\*[,:;]\\s*")) {
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
            this.dtoDirs = dirs;
        } else {
            Set<String> dirs = new LinkedHashSet<>();
            dirs.add("src/main/dto");
            this.dtoDirs = dirs;
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
        if (!processed) {
            processed = true;
        } else {
            return true;
        }

        Map<TypeElement, ImmutableType> immutableTypeMap = parseImmutableTypes(roundEnv);
        Map<ImmutableType, List<DtoType<ImmutableType, ImmutableProp>>> dtoTypeMap =
                parseDtoTypes(immutableTypeMap.values());
        generateJimmerTypes(
                roundEnv
                        .getRootElements()
                        .stream()
                        .filter(it -> it instanceof TypeElement)
                        .map(immutableTypeMap::get)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList()),
                roundEnv
        );
        generateDtoTypes(dtoTypeMap);

        List<TypeElement> errorElements = getErrorFamilies(roundEnv);
        generateErrorType(errorElements);

        return true;
    }

    private Map<TypeElement, ImmutableType> parseImmutableTypes(RoundEnvironment roundEnv) {
        Map<TypeElement, ImmutableType> map = new HashMap<>();
        Set<Element> elements = new HashSet<>();
        elements.addAll(roundEnv.getElementsAnnotatedWith(Immutable.class));
        elements.addAll(roundEnv.getElementsAnnotatedWith(Entity.class));
        elements.addAll(roundEnv.getElementsAnnotatedWith(MappedSuperclass.class));
        elements.addAll(roundEnv.getElementsAnnotatedWith(Embeddable.class));
        processingEnv.getMessager().printMessage(Diagnostic.Kind.WARNING, elements.toString());
        for (Element element : elements) {
            if (element instanceof TypeElement) {
                TypeElement typeElement = (TypeElement) element;
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
                                        typeElement.getQualifiedName().toString() +
                                        "\", immutable type must be interface"
                        );
                    }
                    ImmutableType immutableType = typeUtils.getImmutableType(typeElement);
                    map.put(typeElement, immutableType);
                }
            }
        }
        for (TypeElement typeElement : map.keySet()) {
            typeUtils.getImmutableType(typeElement).resolve(typeUtils);
        }
        return map;
    }

    private Map<ImmutableType, List<DtoType<ImmutableType, ImmutableProp>>> parseDtoTypes(
            Collection<ImmutableType> immutableTypes
    ) {
        String path;
        try {
            path = filer.getResource(
                    StandardLocation.CLASS_OUTPUT,
                    "",
                    "dummy.txt"
            ).toUri().toString();
        } catch (IOException ex) {
            throw new MetaException("Failed to guess base project dir", ex);
        }
        if (path.startsWith("file://")) {
            path = path.substring(7);
        } else if (path.startsWith("file:/")) {
            path = path.substring(6);
        }
        path = path.substring(0, path.lastIndexOf('/'));
        File file = new File(path);
        List<String> actualDtoDirs = new ArrayList<>();
        while (file != null) {
            collectActualDtoDir(file, actualDtoDirs);
            file = file.getParentFile();
        }

        Map<ImmutableType, List<DtoType<ImmutableType, ImmutableProp>>> dtoMap = new HashMap<>();
        for (ImmutableType immutableType : immutableTypes) {
            if (immutableType.isEntity()) {
                for (String actualDtoDir : actualDtoDirs) {
                    File dtoFile = new File(
                            actualDtoDir +
                                    '/' +
                                    immutableType.getQualifiedName().replace('.', '/') +
                                    ".dto"
                    );
                    if (dtoFile.exists()) {
                        List<DtoType<ImmutableType, ImmutableProp>> dtoTypes;
                        try (InputStream in = new FileInputStream(dtoFile)) {
                            dtoTypes = new AptDtoCompiler(immutableType).compile(in);
                        } catch (DtoAstException ex) {
                            throw new MetaException(
                                    "Failed to parse \"" +
                                            dtoFile.getAbsolutePath() +
                                            "\": " +
                                            ex.getMessage(),
                                    ex
                            );
                        } catch (IOException ex) {
                            throw new MetaException(
                                    "Failed to read \"" +
                                            dtoFile.getAbsolutePath() +
                                            "\": " +
                                            ex.getMessage(),
                                    ex
                            );
                        }
                        dtoMap.put(immutableType, dtoTypes);
                    }
                }
            }
        }
        for (Map.Entry<ImmutableType, List<DtoType<ImmutableType, ImmutableProp>>> e : dtoMap.entrySet()) {
            ImmutableType type = e.getKey();
            for (DtoType<ImmutableType, ImmutableProp> dtoType : e.getValue()) {
                for (Map.Entry<ImmutableType, List<DtoType<ImmutableType, ImmutableProp>>> otherEntry : dtoMap.entrySet()) {
                    ImmutableType otherType = otherEntry.getKey();
                    for (DtoType<ImmutableType, ImmutableProp> otherDtoType : otherEntry.getValue()) {
                        if (type != otherType && Objects.equals(dtoType.getName(), otherDtoType.getName())) {
                            throw new MetaException(
                                    "Conflict dto type name, the \"" +
                                            type.getQualifiedName() +
                                            "\" and \"" +
                                            otherType.getQualifiedName() +
                                            "\" are belong to same package, " +
                                            "but they have define a dto type named \"" +
                                            dtoType.getName() +
                                            "\""
                            );
                        }
                    }
                }
            }
        }
        return dtoMap;
    }

    private List<TypeElement> getErrorFamilies(RoundEnvironment roundEnv) {
        List<TypeElement> typeElements = new ArrayList<>();
        for (Element element : roundEnv.getRootElements()) {
            if (element.getAnnotation(ErrorFamily.class) != null) {
                if (element.getKind() != ElementKind.ENUM) {
                    throw new MetaException(
                            "Illegal type \"" +
                                    element +
                                    "\", only enum can be decorated by @" +
                                    ErrorFamily.class.getName()
                    );
                }
                typeElements.add((TypeElement) element);
            }
        }
        return typeElements;
    }

    private void generateJimmerTypes(Collection<ImmutableType> immutableTypes, RoundEnvironment roundEnv) {
        for (ImmutableType immutableType : immutableTypes) {
            new DraftGenerator(
                    immutableType,
                    filer
            ).generate();
            new PropsGenerator(
                    typeUtils,
                    immutableType,
                    filer
            ).generate();
            messager.printMessage(Diagnostic.Kind.NOTE, "Immutable: " + immutableType.getQualifiedName());
            if (immutableType.isEntity()) {
                messager.printMessage(Diagnostic.Kind.NOTE, "Entity: " + immutableType.getQualifiedName());
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
            } else if (immutableType.isEmbeddable()) {
                new PropExpressionGenerator(
                        typeUtils,
                        immutableType,
                        filer
                ).generate();
            }
        }
        PackageCollector packageCollector = new PackageCollector();
        for (Element element : roundEnv.getElementsAnnotatedWith(Entity.class)) {
            packageCollector.accept((TypeElement) element);
        }
        new JimmerModuleGenerator(
                packageCollector.toString(),
                packageCollector.getTypeElements(),
                filer
        ).generate();
    }

    private void generateDtoTypes(Map<?, List<DtoType<ImmutableType, ImmutableProp>>> dtoTypeMap) {
        for (List<DtoType<ImmutableType, ImmutableProp>> dtoTypes : dtoTypeMap.values()) {
            for (DtoType<ImmutableType, ImmutableProp> dtoType : dtoTypes) {
                new DtoGenerator(dtoType, filer).generate();
            }
        }
    }

    private void generateErrorType(List<TypeElement> typeElements) {
        for (TypeElement typeElement : typeElements) {
            new ErrorGenerator(typeElement, filer).generate();
        }
    }

    private void collectActualDtoDir(File baseFile, List<String> outputFiles) {
        for (String dtoDir : dtoDirs) {
            File subFile = baseFile;
            for (String part : dtoDir.split("/")) {
                subFile = new File(subFile, part);
                if (!subFile.isDirectory()) {
                    subFile = null;
                    break;
                }
            }
            if (subFile != null) {
                String path = subFile.getAbsolutePath();
                if (path.endsWith("/")) {
                    path = path.substring(0, path.length() - 1);
                }
                outputFiles.add(path);
            }
        }
    }

    private static class PackageCollector {

        private static final Pattern DOT_PATTERN = Pattern.compile("\\.");

        private List<String> paths;

        private String str;

        private final List<TypeElement> typeElements = new ArrayList<>();

        public void accept(TypeElement typeElement) {
            typeElements.add(typeElement);
            if (paths != null && paths.isEmpty()) {
                return;
            }
            str = null;
            List<String> newPaths = Collections.emptyList();
            for (Element parent = typeElement.getEnclosingElement(); parent != null; parent = parent.getEnclosingElement()) {
                if (parent instanceof PackageElement) {
                    String packageName = ((PackageElement) parent).getQualifiedName().toString();
                    newPaths = new ArrayList<>(Arrays.asList(DOT_PATTERN.split(packageName)));
                    break;
                }
            }
            if (paths == null) {
                paths = newPaths;
            } else {
                int len = Math.min(paths.size(), newPaths.size());
                int index = 0;
                while (index < len) {
                    if (!paths.get(index).equals(newPaths.get(index))) {
                        break;
                    }
                    index++;
                }
                if (index < paths.size()) {
                    paths.subList(index, paths.size()).clear();
                }
            }
        }

        public List<TypeElement> getTypeElements() {
            return Collections.unmodifiableList(typeElements);
        }

        @Override
        public String toString() {
            String s = str;
            if (s == null) {
                List<String> ps = paths;
                str = s = ps == null || ps.isEmpty() ? "" : String.join(".", ps);
            }
            return s;
        }
    }
}
