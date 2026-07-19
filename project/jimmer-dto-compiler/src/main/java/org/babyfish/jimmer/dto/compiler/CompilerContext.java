package org.babyfish.jimmer.dto.compiler;

import org.antlr.v4.runtime.Token;
import org.babyfish.jimmer.dto.compiler.spi.BaseProp;
import org.babyfish.jimmer.dto.compiler.spi.BaseType;
import org.jetbrains.annotations.Nullable;

import java.util.*;

class CompilerContext<T extends BaseType, P extends BaseProp> {

    private final DtoCompiler<T, P> compiler;

    private final Importing importing;

    private final DtoFragmentRegistry<T, P> fragmentRegistry;

    private final Set<String> sourceDtoTypeNames;

    private final Set<String> inactiveSourceDtoTypeNames;

    private final Set<String> inactiveFragmentTypeNames;

    private final Map<String, DtoTypeBuilder<T, P>> typeBuilderMap = new LinkedHashMap<>();

    private final Map<String, Boolean> immutableTypeExistenceMap = new HashMap<>();

    private final Map<String, T> typeMap = new HashMap<>();

    private final Set<String> unresolvedTypeNames = new HashSet<>();

    public CompilerContext(
            DtoCompiler<T, P> compiler,
            DtoFragmentRegistry<T, P> fragmentRegistry,
            Set<String> sourceDtoTypeNames,
            Set<String> inactiveSourceDtoTypeNames,
            Set<String> inactiveFragmentTypeNames
    ) {
        this.compiler = compiler;
        this.fragmentRegistry = fragmentRegistry;
        this.sourceDtoTypeNames = sourceDtoTypeNames;
        this.inactiveSourceDtoTypeNames = inactiveSourceDtoTypeNames;
        this.inactiveFragmentTypeNames = inactiveFragmentTypeNames;
        this.importing = new Importing(this);
    }

    public DtoTypeBuilder<T, P> get(String name) {
        return typeBuilderMap.get(name);
    }

    public void importStatement(DtoParser.ImportStatementContext statement) {
        importing.add(statement);
    }

    public DtoTypeBuilder<T, P> add(DtoParser.DtoTypeContext type, T baseType) {
        String name = type.name.getText();
        if (typeBuilderMap.containsKey(name)) {
            throw exception(
                    type.name.getLine(),
                    type.name.getCharPositionInLine(),
                    "Duplicated dto type name \"" +
                            name +
                            "\""
            );
        }
        Set<DtoModifier> modifiers = EnumSet.noneOf(DtoModifier.class);
        Token sealedModifier = null;
        for (Token modifier : type.modifiers) {
            DtoModifier dtoModifier;
            switch (modifier.getText()) {
                case "input":
                    dtoModifier = DtoModifier.INPUT;
                    break;
                case "specification":
                    dtoModifier = DtoModifier.SPECIFICATION;
                    break;
                case "unsafe":
                    dtoModifier = DtoModifier.UNSAFE;
                    break;
                case "sealed":
                    dtoModifier = DtoModifier.SEALED;
                    sealedModifier = modifier;
                    break;
                case "fixed":
                    dtoModifier = DtoModifier.FIXED;
                    break;
                case "static":
                    dtoModifier = DtoModifier.STATIC;
                    break;
                case "dynamic":
                    dtoModifier = DtoModifier.DYNAMIC;
                    break;
                case "fuzzy":
                    dtoModifier = DtoModifier.FUZZY;
                    break;
                default:
                    throw exception(
                            modifier.getLine(),
                            modifier.getCharPositionInLine(),
                            "If the modifier of dto type is specified, it must be " +
                                    "'input', 'specification', 'unsafe', 'fixed', 'static', 'dynamic' or 'fuzzy'"
                    );
            }
            if (!modifiers.add(dtoModifier)) {
                throw exception(
                        modifier.getLine(),
                        modifier.getCharPositionInLine(),
                        "Duplicated modifier \"" + modifier.getText() + "\""
                );
            }
        }
        if (sealedModifier != null && type.body.typesBlocks.isEmpty()) {
            throw exception(
                    sealedModifier.getLine(),
                    sealedModifier.getCharPositionInLine(),
                    "The modifier 'sealed' can only be used for polymorphic DTOs with #types"
            );
        }
        if (modifiers.contains(DtoModifier.INPUT) &&
                modifiers.contains(DtoModifier.SPECIFICATION)) {
            throw exception(
                    type.name.getLine(),
                    type.name.getCharPositionInLine(),
                    "The modifiers 'input' and 'specification' cannot appear at the same time"
            );
        }
        if (modifiers.contains(DtoModifier.UNSAFE) &&
                modifiers.contains(DtoModifier.SPECIFICATION)) {
            throw exception(
                    type.name.getLine(),
                    type.name.getCharPositionInLine(),
                    "The modifier 'unsafe' cannot be used with specification"
            );
        }
        if (modifiers.contains(DtoModifier.SPECIFICATION) && !baseType.isEntity()) {
            throw exception(
                    type.name.getLine(),
                    type.name.getCharPositionInLine(),
                    "The modifier 'specification' can only be used to decorate entity type"
            );
        }
        if (!modifiers.contains(DtoModifier.INPUT)) {
            for (DtoModifier modifier : modifiers) {
                if (modifier.isInputStrategy()) {
                    throw exception(
                            type.name.getLine(),
                            type.name.getCharPositionInLine(),
                            "The modifier '" +
                                    modifier.name().toLowerCase() +
                                    "' can only be used for input"
                    );
                }
            }
        }
        DtoModifier inputStrategyModifier = null;
        for (DtoModifier modifier : modifiers) {
            if (modifier.isInputStrategy()) {
                if (inputStrategyModifier == null) {
                    inputStrategyModifier = modifier;
                } else {
                    throw exception(
                            type.name.getLine(),
                            type.name.getCharPositionInLine(),
                            "The modifiers '" +
                                    inputStrategyModifier.name().toLowerCase() +
                                    "' and '" +
                                    modifier.name().toLowerCase() +
                                    "' cannot appear at the same time"
                    );
                }
            }
        }
        if (modifiers.contains(DtoModifier.INPUT) && !modifiers.stream().anyMatch(DtoModifier::isInputStrategy)) {
            modifiers.add(compiler.getDefaultNullableInputModifier());
        }
        if (!modifiers.isEmpty()) {
            List<DtoModifier> list = new ArrayList<>(modifiers);
            Collections.sort(list, Comparator.comparing(DtoModifier::getOrder));
            modifiers = new LinkedHashSet<>(list);
        }
        DtoTypeBuilder<T, P> typeBuilder = new DtoTypeBuilder<>(
                null,
                baseType,
                type.body,
                type.name,
                Docs.parse(type.doc),
                modifiers,
                type.annotations,
                type.superInterfaces,
                this
        );
        typeBuilderMap.put(name, typeBuilder);
        return typeBuilder;
    }

    String resolveTargetTypeName(@Nullable DtoParser.QualifiedNameContext targetType) {
        return targetType != null ?
                importing.resolveImmutableType(targetType) :
                compiler.getSourceTypeName();
    }

    T resolveTargetType(
            String qualifiedName,
            @Nullable DtoParser.QualifiedNameContext targetType,
            Token declarationName,
            String declarationKind
    ) {
        T baseType = null;
        if (targetType == null && compiler.baseTypeOrNull() != null &&
                compiler.baseTypeOrNull().getQualifiedName().equals(qualifiedName)) {
            baseType = compiler.baseTypeOrNull();
        }
        if (baseType == null) {
            baseType = getType(qualifiedName);
        }
        if (baseType == null) {
            if (targetType == null && !compiler.isExplicitSourceType()) {
                throw exception(
                        declarationName.getLine(),
                        declarationName.getCharPositionInLine(),
                        "The " +
                                declarationKind +
                                " \"" +
                                declarationName.getText() +
                                "\" must specify its target type by 'for' because this dto file is not associated with an immutable type"
                );
            }
            throw exception(
                    targetType != null ? targetType.start.getLine() : declarationName.getLine(),
                    targetType != null ? targetType.start.getCharPositionInLine() : declarationName.getCharPositionInLine(),
                    "Illegal target type \"" +
                            qualifiedName +
                            "\" of " +
                            declarationKind +
                            " \"" +
                            declarationName.getText() +
                            "\", it cannot be resolved as immutable type"
            );
        }
        return baseType;
    }

    DtoFragmentUse<T, P> resolveFragment(
            DtoParser.IncludeContext include,
            T baseType
    ) {
        return fragmentRegistry.resolve(this, include, baseType);
    }

    public List<DtoType<T, P>> getDtoTypes() {
        List<DtoType<T, P>> types = new ArrayList<>(typeBuilderMap.size());
        for (DtoTypeBuilder<T, P> builder : typeBuilderMap.values()) {
            DtoType<T, P> type = builder.build();
            types.add(type);
        }
        return types;
    }

    public Map<String, P> getProps(T baseType) {
        return compiler.getProps(baseType);
    }

    public Map<String, P> getDeclaredProps(T baseType) {
        return compiler.getDeclaredProps(baseType);
    }

    public boolean isImplicitId(P baseProp, Set<DtoModifier> modifiers) {
        if (modifiers.contains(DtoModifier.INPUT) || modifiers.contains(DtoModifier.SPECIFICATION)) {
            return baseProp.isId() && compiler.isGeneratedValue(baseProp);
        }
        return false;
    }

    public T getTargetType(P baseProp) {
        return compiler.getTargetType(baseProp);
    }

    @Nullable
    public P getIdProp(T baseType) {
        return compiler.getIdProp(baseType);
    }

    public boolean isSameType(P baseProp1, P baseProp2) {
        return compiler.isSameType(baseProp1, baseProp2);
    }

    public SimplePropType getSimpleType(P baseProp) {
        return compiler.getSimplePropType(baseProp);
    }

    public SimplePropType getSimpleType(PropConfig.PathNode<P> pathNode) {
        return compiler.getSimplePropType(pathNode);
    }

    public DtoFile getDtoFile() {
        return compiler.getDtoFile();
    }

    public String getTargetPackageName() {
        return compiler.getTargetPackageName();
    }

    public String getDtoPackageName() {
        return compiler.getTargetPackageName();
    }

    public String getDtoQualifiedName(String name) {
        String packageName = getDtoPackageName();
        return packageName.isEmpty() ? name : packageName + '.' + name;
    }

    public String getDefaultBasePackageName() {
        return compiler.getDefaultBasePackageName();
    }

    public Collection<T> getSuperTypes(T baseType) {
        return compiler.getSuperTypes(baseType);
    }

    @Nullable
    public T getType(String qualifiedName) {
        T type = typeMap.get(qualifiedName);
        if (type != null || unresolvedTypeNames.contains(qualifiedName)) {
            return type;
        }
        type = compiler.getType(qualifiedName);
        if (type != null) {
            typeMap.put(qualifiedName, type);
        } else {
            unresolvedTypeNames.add(qualifiedName);
        }
        return type;
    }

    public Collection<T> getDirectSubTypes(T baseType) {
        return compiler.getDirectSubTypes(baseType);
    }

    public boolean isSameType(T baseType1, T baseType2) {
        return compiler.isSameType(baseType1, baseType2);
    }

    public boolean isInstantiable(T baseType) {
        return compiler.isInstantiable(baseType);
    }

    public List<String> getEnumConstants(P baseProp) {
        return compiler.getEnumConstants(baseProp);
    }

    public TypeRef resolve(DtoParser.TypeRefContext ctx) {
        return importing.resolve(ctx, compiler);
    }

    public String resolve(DtoParser.QualifiedNameContext ctx) {
        return importing.resolve(ctx);
    }

    public String resolveDtoType(DtoParser.QualifiedNameContext ctx) {
        String qualifiedName = importing.resolveDtoType(ctx);
        if (!sourceDtoTypeNames.contains(qualifiedName) &&
                inactiveSourceDtoTypeNames.contains(qualifiedName)) {
            throw exception(
                    ctx.start.getLine(),
                    ctx.start.getCharPositionInLine(),
                    "Source DTO type \"" + qualifiedName + "\" is not active in the current processor compilation"
            );
        }
        return qualifiedName;
    }

    String resolveFragmentType(DtoParser.QualifiedNameContext ctx) {
        return importing.resolveFragmentType(ctx);
    }

    public String resolveImmutableType(DtoParser.QualifiedNameContext ctx) {
        return importing.resolveImmutableType(ctx);
    }

    public String resolve(String qualifiedName, int qualifiedNameLine, int qualifiedNameCol) {
        return importing.resolve(qualifiedName, qualifiedNameLine, qualifiedNameCol);
    }

    boolean typeExists(String qualifiedName) {
        return compiler.getGenericTypeCount(qualifiedName) != null;
    }

    boolean immutableTypeExists(String qualifiedName) {
        return immutableTypeExistenceMap.computeIfAbsent(qualifiedName, compiler::isImmutableType);
    }

    boolean dtoTypeExists(String qualifiedName) {
        return sourceDtoTypeNames.contains(qualifiedName) ||
                inactiveSourceDtoTypeNames.contains(qualifiedName) ||
                typeExists(qualifiedName);
    }

    boolean fragmentTypeExists(String qualifiedName) {
        return fragmentRegistry.contains(qualifiedName) || inactiveFragmentTypeNames.contains(qualifiedName);
    }

    boolean inactiveFragmentTypeExists(String qualifiedName) {
        return inactiveFragmentTypeNames.contains(qualifiedName) && !fragmentRegistry.contains(qualifiedName);
    }

    public DtoAstException exception(int line, int col, String message) {
        return compiler.exception(line, col, message);
    }
}
