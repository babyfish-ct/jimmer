package org.babyfish.jimmer.apt;

import org.babyfish.jimmer.apt.client.ClientProcessor;
import org.babyfish.jimmer.apt.dto.DtoProcessor;
import org.babyfish.jimmer.apt.error.ErrorProcessor;
import org.babyfish.jimmer.apt.immutable.ImmutableProcessor;
import org.babyfish.jimmer.dto.compiler.DtoUtils;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;
import java.util.*;

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

    private boolean serverGenerated;

    private boolean clientGenerated;

    private Set<? extends Element> delayedClientElements;

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
            if (!serverGenerated) {
                serverGenerated = true;
                new ImmutableProcessor(context, filer, messager).process(roundEnv);
                new ErrorProcessor(context, filer).process(roundEnv);
                boolean dtoGenerated = new DtoProcessor(context, elements, filer, dtoDirs).process();
                if (dtoGenerated) {
                    delayedClientElements = roundEnv.getRootElements();
                    return false;
                }
            }
            if (!clientGenerated) {
                new ClientProcessor(context, elements, filer, delayedClientElements).handleService(roundEnv);
                clientGenerated = true;
                delayedClientElements = null;
            }
        } catch (MetaException ex) {
            messager.printMessage(Diagnostic.Kind.ERROR, ex.getMessage(), ex.getElement());
        }
        return true;
    }
}
