package org.babyfish.jimmer.apt.dto;

import org.babyfish.jimmer.Input;
import org.babyfish.jimmer.View;
import org.babyfish.jimmer.apt.Context;
import org.babyfish.jimmer.apt.client.DocMetadata;
import org.babyfish.jimmer.apt.immutable.generator.Constants;
import org.babyfish.jimmer.apt.immutable.meta.ImmutableProp;
import org.babyfish.jimmer.apt.immutable.meta.ImmutableType;
import org.babyfish.jimmer.apt.util.GenericParser;
import org.babyfish.jimmer.dto.compiler.*;

import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class DtoProcessor {

    private final Context context;

    private final Elements elements;

    private final Collection<String> dtoDirs;

    private final boolean dtoBundleEnabled;

    private final DtoModifier defaultNullableInputModifier;

    public DtoProcessor(
            Context context,
            Elements elements,
            Collection<String> dtoDirs,
            boolean dtoBundleEnabled,
            DtoModifier defaultNullableInputModifier
    ) {
        this.context = context;
        this.elements = elements;
        this.dtoDirs = dtoDirs;
        this.dtoBundleEnabled = dtoBundleEnabled;
        this.defaultNullableInputModifier = defaultNullableInputModifier;
    }

    public boolean process() {
        return generateDtoTypes(parseDtoTypes());
    }

    private List<DtoType<ImmutableType, ImmutableProp>> parseDtoTypes() {
        List<AptDtoCompiler> compilers = new ArrayList<>();
        DtoContext dtoContext = new DtoContext(context.getFiler(), dtoDirs, dtoBundleEnabled);
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
        List<DtoType<ImmutableType, ImmutableProp>> dtoTypes = DtoCompiler
                .compileAll(compilers, context::includeDtoTarget)
                .values()
                .stream()
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
        DtoTypeLinker.link(dtoTypes, this::resolveDtoType);
        return dtoTypes;
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
        } else if (types.isSubtype(
                type,
                types.erasure(
                        elements.getTypeElement(Constants.JSPECIFICATION_CLASS_NAME.canonicalName()).asType()
                )
        )) {
            kind = DtoTypeKind.SPECIFICATION;
            superName = Constants.JSPECIFICATION_CLASS_NAME.canonicalName();
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

    private boolean generateDtoTypes(List<DtoType<ImmutableType, ImmutableProp>> dtoTypes) {
        boolean result = false;
        DocMetadata docMetadata = new DocMetadata(context);
        for (DtoType<ImmutableType, ImmutableProp> dtoType : dtoTypes) {
            new DtoGenerator(context, docMetadata, dtoType).generate();
            result = true;
        }
        return result;
    }
}
