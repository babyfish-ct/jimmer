package org.babyfish.jimmer.apt;

import org.babyfish.jimmer.apt.client.ClientProcessor;
import org.babyfish.jimmer.apt.client.FetchByUnsupportedException;
import org.babyfish.jimmer.apt.dto.DtoProcessor;
import org.babyfish.jimmer.apt.entry.EntryProcessor;
import org.babyfish.jimmer.apt.error.ErrorProcessor;
import org.babyfish.jimmer.apt.immutable.ImmutableProcessor;
import org.babyfish.jimmer.client.EnableImplicitApi;
import org.babyfish.jimmer.client.FetchBy;
import org.babyfish.jimmer.dto.compiler.DtoAstException;
import org.babyfish.jimmer.dto.compiler.DtoModifier;
import org.babyfish.jimmer.dto.compiler.DtoUtils;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@SupportedAnnotationTypes({
        "org.babyfish.jimmer.Immutable",
        "org.babyfish.jimmer.sql.Entity",
        "org.babyfish.jimmer.sql.MappedSuperclass",
        "org.babyfish.jimmer.sql.Embeddable",
        "org.babyfish.jimmer.sql.EnableDtoGeneration",
        "org.babyfish.jimmer.error.ErrorFamily",
        "org.babyfish.jimmer.client.Api",
        "org.springframework.web.bind.annotation.RestController"
})
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class JimmerProcessor extends AbstractProcessor {

    private Context context;

    private Filer filer;

    private Elements elements;

    private Messager messager;

    private Collection<String> dtoDirs;

    private Collection<String> dtoTestDirs;

    private DtoModifier defaultNullableInputModifier = DtoModifier.STATIC;

    private boolean checkedException;

    private boolean ignoreJdkWarning;

    private boolean serverGenerated;

    private Boolean clientExplicitApi;

    private boolean clientGenerated;

    private List<String> delayedClientTypeNames;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        messager = processingEnv.getMessager();
        String includes = processingEnv.getOptions().get("jimmer.source.includes");
        String excludes = processingEnv.getOptions().get("jimmer.source.excludes");
        String[] includeArr = null;
        String[] excludeArr = null;
        if (includes != null && !includes.isEmpty()) {
            includeArr = includes.trim().split("\\s*,\\s*");
        }
        if (excludes != null && !excludes.isEmpty()) {
            excludeArr = excludes.trim().split("\\s*,\\s*");
        }
        this.dtoDirs = dtoDirs(
                processingEnv,
                "jimmer.dto.dirs",
                "src/main/",
                Collections.singletonList("src/main/dto")
        );
        this.dtoTestDirs = dtoDirs(
                processingEnv,
                "jimmer.dto.testDirs",
                "src/test/",
                Collections.singletonList("src/test/dto")
        );
        String inputModifierText = processingEnv.getOptions().get("jimmer.dto.defaultNullableInputModifier");
        if (inputModifierText != null && !inputModifierText.isEmpty()) {
            switch (inputModifierText) {
                case "fixed":
                    defaultNullableInputModifier = DtoModifier.FIXED;
                    break;
                case "static":
                    defaultNullableInputModifier = DtoModifier.STATIC;
                    break;
                case "dynamic":
                    defaultNullableInputModifier = DtoModifier.DYNAMIC;
                    break;
                case "fuzzy":
                    defaultNullableInputModifier = DtoModifier.FUZZY;
                    break;
                default:
                    throw new IllegalArgumentException(
                            "The apt options `jimmer.dto.defaultNullableInputModifier` can only be " +
                                    "\"fixed\", \"static\", \"dynamic\" or \"fuzzy\""
                    );
            }
        }

        checkedException = "true".equals(processingEnv.getOptions().get("jimmer.client.checkedException"));
        ignoreJdkWarning = "true".equals(processingEnv.getOptions().get("jimmer.client.ignoreJdkWarning"));
        context = new Context(
                processingEnv.getElementUtils(),
                processingEnv.getTypeUtils(),
                "true".equals(processingEnv.getOptions().get("jimmer.keepIsPrefix")),
                includeArr,
                excludeArr,
                processingEnv.getOptions().get("jimmer.entry.objects"),
                processingEnv.getOptions().get("jimmer.entry.tables"),
                processingEnv.getOptions().get("jimmer.entry.tableExes"),
                processingEnv.getOptions().get("jimmer.entry.fetchers")
        );
        filer = processingEnv.getFiler();
        elements = processingEnv.getElementUtils();
    }

    @Override
    public boolean process(
            Set<? extends TypeElement> annotations,
            RoundEnvironment roundEnv
    ) {
        try {
            if (clientExplicitApi == null) {
                clientExplicitApi = roundEnv.getRootElements().stream().anyMatch(
                        it -> it instanceof TypeElement &&
                                context.include((TypeElement) it) &&
                                it.getAnnotation(EnableImplicitApi.class) != null
                );
            }
            if (!serverGenerated) {
                serverGenerated = true;
                Collection<TypeElement> immutableTypeElements =
                        new ImmutableProcessor(context, filer, messager).process(roundEnv).keySet();
                new EntryProcessor(context, immutableTypeElements, filer).process();
                boolean errorGenerated = new ErrorProcessor(context, checkedException, filer).process(roundEnv);
                boolean dtoGenerated = new DtoProcessor(context, elements, filer, isTest() ? dtoTestDirs : dtoDirs, defaultNullableInputModifier).process();
                if (!immutableTypeElements.isEmpty() || errorGenerated || dtoGenerated) {
                    delayedClientTypeNames = roundEnv
                            .getRootElements()
                            .stream()
                            .filter(it -> it instanceof TypeElement)
                            .map(it -> ((TypeElement) it).getQualifiedName().toString())
                            .collect(Collectors.toList());
                    return false;
                }
            }
            if (!clientGenerated) {
                clientGenerated = true;
                new ClientProcessor(context, elements, filer, clientExplicitApi, delayedClientTypeNames).process(roundEnv);
                delayedClientTypeNames = null;
            }
        } catch (MetaException ex) {
            messager.printMessage(Diagnostic.Kind.ERROR, ex.getMessage(), ex.getElement());
        } catch (DtoAstException ex) {
            messager.printMessage(Diagnostic.Kind.ERROR, ex.getMessage());
            throw ex;
        } catch (FetchByUnsupportedException ex) {
            String message =
                    "In order to parse the `@" +
                            FetchBy.class.getName() +
                            "` annotations that decorate generic type parameters, " +
                            "please make sure the java compiler version is 11 or higher " +
                            "(`source.version` and `target.version` can still remain `1.8`). " +
                            "However, once compilation is complete, " +
                            "you can still use Java 8 to deploy and run the project";
            if (ignoreJdkWarning) {
                messager.printMessage(
                        Diagnostic.Kind.WARNING,
                        message
                );
            } else {
                messager.printMessage(
                        Diagnostic.Kind.ERROR,
                        message +
                                ". If you want to suppress this error" +
                                "(Note, this will lead to generating incorrect client code such as openapi and typescript), " +
                                "please add the argument `-Ajimmer.client.ignoreJdkWarning=true` to java compiler by maven or gradle"
                );
            }
        }
        return true;
    }

    private boolean isTest() {
        try {
            String path = filer.getResource(
                    StandardLocation.CLASS_OUTPUT,
                    "",
                    "dummy.txt"
            ).toUri().getPath();
            return path.endsWith("/test/dummy.txt");
        } catch (IOException ex) {
            throw new GeneratorException("Cannot get the class output dir", ex);
        }
    }

    private static Collection<String> dtoDirs(
            ProcessingEnvironment env,
            String configurationName,
            String prefix,
            Collection<String> defaultDirs) {
        String dtoDirs = env.getOptions().get(configurationName);
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
            for (String dir : dirs) {
                if (!dir.startsWith(prefix)) {
                    throw new GeneratorException(
                            "Illegal annotation processor configuration \"" +
                                    configurationName +
                                    "\", it contains an illegal path \"" +
                                    dir +
                                    "\" which does not start with \"" +
                                    prefix +
                                    "\"",
                            null
                    );
                }
            }
            return DtoUtils.standardDtoDirs(dirs);
        }
        return defaultDirs;
    }
}
