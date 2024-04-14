package org.babyfish.jimmer.dto.compiler;

import org.antlr.v4.runtime.Token;
import org.babyfish.jimmer.dto.compiler.spi.BaseProp;
import org.babyfish.jimmer.dto.compiler.spi.BaseType;

import java.util.*;

class CompilerContext<T extends BaseType, P extends BaseProp> {

    private final DtoCompiler<T, P> compiler;

    private final Importing importing;

    private final Map<String, DtoTypeBuilder<T, P>> typeBuilderMap = new LinkedHashMap<>();

    public CompilerContext(DtoCompiler<T, P> compiler) {
        this.compiler = compiler;
        this.importing = new Importing(this);
    }

    public DtoTypeBuilder<T, P> get(String name) {
        return typeBuilderMap.get(name);
    }

    public void importStatement(DtoParser.ImportStatementContext statement) {
        importing.add(statement);
    }

    public DtoTypeBuilder<T, P> add(DtoParser.DtoTypeContext type) {
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
        if (modifiers.contains(DtoModifier.INPUT) &&
                modifiers.contains(DtoModifier.SPECIFICATION)) {
            throw exception(
                    type.name.getLine(),
                    type.name.getCharPositionInLine(),
                    "The modifiers 'input' and 'specification' cannot appear at the same time"
            );
        }
        if (modifiers.contains(DtoModifier.UNSAFE) &&
                !modifiers.contains(DtoModifier.INPUT)) {
            throw exception(
                    type.name.getLine(),
                    type.name.getCharPositionInLine(),
                    "The modifier 'unsafe' can only be used for input"
            );
        }
        if (modifiers.contains(DtoModifier.SPECIFICATION) && !compiler.getBaseType().isEntity()) {
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
                compiler.getBaseType(),
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

    public boolean isSameType(P baseProp1, P baseProp2) {
        return compiler.isSameType(baseProp1, baseProp2);
    }

    public boolean isStringProp(P baseProp) {
        return compiler.isStringProp(baseProp);
    }

    public DtoFile getDtoFile() {
        return compiler.getDtoFile();
    }

    public String getTargetPackageName() {
        return compiler.getTargetPackageName();
    }

    public T getBaseType() {
        return compiler.getBaseType();
    }

    public Collection<T> getSuperTypes(T baseType) {
        return compiler.getSuperTypes(baseType);
    }

    public List<String> getEnumConstants(P baseProp) {
        return compiler.getEnumConstants(baseProp);
    }

    public TypeRef resolve(DtoParser.TypeRefContext ctx) {
        return importing.resolve(ctx, compiler);
    }

    public String resolve(DtoParser.QualifiedNameContext ctx) { return importing.resolve(ctx); }

    public String resolve(String qualifiedName, int qualifiedNameLine, int qualifiedNameCol) {
        return importing.resolve(qualifiedName, qualifiedNameLine, qualifiedNameCol);
    }
    
    public DtoAstException exception(int line, int col, String message) {
        return compiler.exception(line, col, message);
    }
}
