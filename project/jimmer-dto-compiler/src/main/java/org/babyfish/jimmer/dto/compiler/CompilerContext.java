package org.babyfish.jimmer.dto.compiler;

import org.antlr.v4.runtime.Token;
import org.babyfish.jimmer.dto.compiler.spi.BaseProp;
import org.babyfish.jimmer.dto.compiler.spi.BaseType;

import java.util.*;

class CompilerContext<T extends BaseType, P extends BaseProp> {

    private final DtoCompiler<T, P> compiler;

    private final Importing importing;

    private final boolean hasKey;

    private final Map<String, DtoTypeBuilder<T, P>> typeBuilderMap = new LinkedHashMap<>();

    public CompilerContext(DtoCompiler<T, P> compiler) {
        this.compiler = compiler;
        this.importing = new Importing(this);
        boolean hasKey = false;
        for (P baseProp : compiler.getProps(compiler.getBaseType()).values()) {
            if (baseProp.isKey()) {
                hasKey = true;
                break;
            }
        }
        this.hasKey = hasKey;
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
                    "Duplicated dto type name \"" +
                            name +
                            "\""
            );
        }
        EnumSet<DtoTypeModifier> modifiers = EnumSet.noneOf(DtoTypeModifier.class);
        for (Token modifier : type.modifiers) {
            DtoTypeModifier dtoTypeModifier;
            switch (modifier.getText()) {
                case "input":
                    dtoTypeModifier = DtoTypeModifier.INPUT;
                    break;
                case "abstract":
                    dtoTypeModifier = DtoTypeModifier.ABSTRACT;
                    break;
                default:
                    throw exception(
                            modifier.getLine(),
                            "If the modifier of dto type is specified, it must be 'input' or 'abstract'"
                    );
            }
            if (!modifiers.add(dtoTypeModifier)) {
                throw exception(
                        modifier.getLine(),
                        "Duplicated modifier \"" + modifier.getText() + "\""
                );
            }
        }
        Set<String> superSet = new LinkedHashSet<>();
        for (Token superName : type.superNames) {
            if (!superSet.add(superName.getText())) {
                throw exception(
                        superName.getLine(),
                        "Duplicated super dto name \"" + superName.getText() + "\""
                );
            }
        }
        DtoTypeBuilder<T, P> typeBuilder = new DtoTypeBuilder<>(
                compiler.getBaseType(),
                type.body,
                type.name,
                modifiers,
                type.superNames,
                null,
                null,
                this
        );
        typeBuilderMap.put(name, typeBuilder);
        return typeBuilder;
    }

    public List<DtoType<T, P>> getDtoTypes() {
        List<DtoType<T, P>> types = new ArrayList<>(typeBuilderMap.size());
        for (DtoTypeBuilder<T, P> builder : typeBuilderMap.values()) {
            DtoType<T, P> type = builder.build();
            if (!builder.isAbstract()) {
                types.add(type);
            }
        }
        return types;
    }

    public Map<String, P> getProps(T baseType) {
        return compiler.getProps(baseType);
    }

    public Map<String, P> getDeclaredProps(T baseType) {
        return compiler.getDeclaredProps(baseType);
    }

    public boolean isImplicit(P baseProp) {
        return baseProp.isId() && hasKey;
    }

    public T getTargetType(P baseProp) {
        return compiler.getTargetType(baseProp);
    }

    public String getDtoFilePath() {
        return compiler.getDtoFilePath();
    }

    public T getBaseType() {
        return compiler.getBaseType();
    }

    public TypeRef resolve(DtoParser.TypeRefContext ctx) {
        return importing.resolve(ctx);
    }

    public String resolve(DtoParser.QualifiedNameContext ctx) { return importing.resolve(ctx); }

    public String resolve(String qualifiedName, int qualifiedNameLine) {
        return importing.resolve(qualifiedName, qualifiedNameLine);
    }
    
    public DtoAstException exception(int line, String message) {
        return compiler.exception(line, message);
    }
}
