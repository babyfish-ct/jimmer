package org.babyfish.jimmer.apt.dto;

import org.babyfish.jimmer.Input;
import org.babyfish.jimmer.View;
import org.babyfish.jimmer.apt.Context;
import org.babyfish.jimmer.apt.client.DocMetadata;
import org.babyfish.jimmer.apt.immutable.meta.ImmutableProp;
import org.babyfish.jimmer.apt.immutable.meta.ImmutableType;
import org.babyfish.jimmer.apt.util.GenericParser;
import org.babyfish.jimmer.dto.compiler.*;

import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
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
        List<AptDtoCompiler> compilers = new ArrayList<>();
        DtoContext dtoContext = new DtoContext(context.getFiler(), dtoDirs);
        AptDtoCompiler compiler;

        for (DtoFile dtoFile : dtoContext.getDtoFiles()) {
            try {
                compiler = new AptDtoCompiler(dtoFile, context, elements, defaultNullableInputModifier);
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
            compilers.add(compiler);
        }
        for (List<DtoType<ImmutableType, ImmutableProp>> dtoTypes :
                DtoCompiler.compileAll(compilers, context::includeDtoTarget).values()) {
            for (DtoType<ImmutableType, ImmutableProp> dtoType : dtoTypes) {
                dtoTypeMap
                        .computeIfAbsent(dtoType.getBaseType(), it -> new ArrayList<>())
                        .add(dtoType);
            }
        }
        DtoTypeLinker.link(
                dtoTypeMap.values().stream().flatMap(Collection::stream).collect(java.util.stream.Collectors.toList()),
                this::resolveDtoType
        );
        return dtoTypeMap;
    }

    private DtoTypeInfo<ImmutableType> resolveDtoType(String qualifiedName) {
        TypeElement typeElement = elements.getTypeElement(qualifiedName);
        if (typeElement == null) {
            return null;
        }
        Types types = context.getTypes();
        TypeMirror type = types.erasure(typeElement.asType());
        DtoTypeKind kind;
        String superName;
        if (types.isSubtype(type, types.erasure(elements.getTypeElement(Input.class.getName()).asType()))) {
            kind = DtoTypeKind.INPUT;
            superName = Input.class.getName();
        } else if (types.isSubtype(type, types.erasure(elements.getTypeElement(View.class.getName()).asType()))) {
            kind = DtoTypeKind.VIEW;
            superName = View.class.getName();
        } else {
            return null;
        }
        TypeMirror baseTypeMirror = new GenericParser(
                "reusable DTO",
                typeElement,
                superName
        ).parse().arguments.get(0);
        ImmutableType baseType = context.getImmutableType(baseTypeMirror);
        if (baseType == null) {
            throw new DtoException(
                    "The entity type argument of reusable DTO type \"" +
                            qualifiedName +
                            "\" is not an immutable type"
            );
        }
        return new DtoTypeInfo<>(baseType, kind);
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
