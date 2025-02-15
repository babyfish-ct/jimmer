package org.babyfish.jimmer.apt.dto;

import org.babyfish.jimmer.Immutable;
import org.babyfish.jimmer.apt.Context;
import org.babyfish.jimmer.apt.client.DocMetadata;
import org.babyfish.jimmer.apt.immutable.meta.ImmutableProp;
import org.babyfish.jimmer.apt.immutable.meta.ImmutableType;
import org.babyfish.jimmer.dto.compiler.DtoAstException;
import org.babyfish.jimmer.dto.compiler.DtoFile;
import org.babyfish.jimmer.dto.compiler.DtoModifier;
import org.babyfish.jimmer.dto.compiler.DtoType;
import org.babyfish.jimmer.sql.Embeddable;
import org.babyfish.jimmer.sql.Entity;

import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import java.util.*;

public class DtoProcessor {

    private final Context context;

    private final Elements elements;

    private final Collection<String> dtoDirs;

    private final DtoModifier defaultNullableInputModifier;

    public DtoProcessor(
            Context context,
            Elements elements,
            Collection<String> dtoDirs,
            DtoModifier defaultNullableInputModifier
    ) {
        this.context = context;
        this.elements = elements;
        this.dtoDirs = dtoDirs;
        this.defaultNullableInputModifier = defaultNullableInputModifier;
    }

    public boolean process() {
        Map<ImmutableType, List<DtoType<ImmutableType, ImmutableProp>>> dtoTypeMap = parseDtoTypes();
        return generateDtoTypes(dtoTypeMap);
    }

    private Map<ImmutableType, List<DtoType<ImmutableType, ImmutableProp>>> parseDtoTypes() {
        Map<ImmutableType, List<DtoType<ImmutableType, ImmutableProp>>> dtoTypeMap = new LinkedHashMap<>();
        DtoContext dtoContext = new DtoContext(context.getFiler(), dtoDirs);
        AptDtoCompiler compiler;

        for (DtoFile dtoFile : dtoContext.getDtoFiles()) {
            try {
                compiler = new AptDtoCompiler(dtoFile, elements, defaultNullableInputModifier);
            } catch (DtoAstException ex) {
                throw new DtoException(
                        "Failed to parse \"" +
                                dtoFile.getAbsolutePath() +
                                "\": " +
                                ex.getMessage(),
                        ex
                );
            } catch (Throwable ex) {
                throw new DtoException(
                        "Failed to read \"" +
                                dtoFile.getAbsolutePath() +
                                "\": " +
                                ex.getMessage(),
                        ex
                );
            }
            TypeElement typeElement = elements.getTypeElement(compiler.getSourceTypeName());
            if (typeElement == null) {
                throw new DtoException(
                        "Failed to parse \"" +
                                dtoFile.getAbsolutePath() +
                                "\": No immutable type \"" +
                                compiler.getSourceTypeName() +
                                "\""
                );
            }
            if (!context.include(typeElement)) {
                continue;
            }
            if (typeElement.getAnnotation(Entity.class) == null &&
            typeElement.getAnnotation(Embeddable.class) == null &&
            typeElement.getAnnotation(Immutable.class) == null) {
                throw new DtoException(
                        "Failed to parse \"" +
                                dtoFile.getAbsolutePath() +
                                "\": the \"" +
                                compiler.getSourceTypeName() +
                                "\" is not decorated by \"@" +
                                Entity.class.getName() +
                                "\", \"" +
                                Embeddable.class.getName() +
                                "\" or \"" +
                                Immutable.class.getName() +
                                "\""
                );
            }
            ImmutableType immutableType = context.getImmutableType(typeElement);
            dtoTypeMap
                    .computeIfAbsent(immutableType, it -> new ArrayList<>())
                    .addAll(compiler.compile(immutableType));
        }
        return dtoTypeMap;
    }

    private boolean generateDtoTypes(Map<?, List<DtoType<ImmutableType, ImmutableProp>>> dtoTypeMap) {
        boolean result = false;
        DocMetadata docMetadata = new DocMetadata(context);
        for (List<DtoType<ImmutableType, ImmutableProp>> dtoTypes : dtoTypeMap.values()) {
            for (DtoType<ImmutableType, ImmutableProp> dtoType : dtoTypes) {
                new DtoGenerator(context, docMetadata, dtoType).generate();
                result = true;
            }
        }
        return result;
    }
}
