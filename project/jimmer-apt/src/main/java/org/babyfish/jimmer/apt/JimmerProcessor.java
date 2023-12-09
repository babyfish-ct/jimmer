package org.babyfish.jimmer.apt;

import org.babyfish.jimmer.apt.client.ClientProcessor;
import org.babyfish.jimmer.apt.dto.DtoProcessor;
import org.babyfish.jimmer.apt.entry.EntryProcessor;
import org.babyfish.jimmer.apt.error.ErrorProcessor;
import org.babyfish.jimmer.apt.immutable.ImmutableProcessor;
import org.babyfish.jimmer.client.EnableImplicitApi;
import org.babyfish.jimmer.dto.compiler.DtoUtils;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
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

    private boolean checkedException;

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
        String dtoDirs = processingEnv.getOptions().get("jimmer.dto.dirs");
        String[] includeArr = null;
        String[] excludeArr = null;
        if (includes != null && !includes.isEmpty()) {
            includeArr = includes.trim().split("\\s*,\\s*");
        }
        if (excludes != null && !excludes.isEmpty()) {
            excludeArr = excludes.trim().split("\\s*,\\s*");
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
        checkedException = "true".equals(processingEnv.getOptions().get("jimmer.checkedException"));
        context = new Context(
                processingEnv.getElementUtils(),
                processingEnv.getTypeUtils(),
                "true".equals(processingEnv.getOptions().get("jimmer.keepIsPrefix")),
                includeArr,
                excludeArr
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
                boolean dtoGenerated = new DtoProcessor(context, elements, filer, dtoDirs).process();
                if (errorGenerated || dtoGenerated) {
                    delayedClientTypeNames = roundEnv
                            .getRootElements()
                            .stream()
                            .filter(it -> it instanceof TypeElement)
                            .map(it -> ((TypeElement)it).getQualifiedName().toString())
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
        }
        return true;
    }
}
