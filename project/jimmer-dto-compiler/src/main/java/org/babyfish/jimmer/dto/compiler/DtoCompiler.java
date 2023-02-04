package org.babyfish.jimmer.dto.compiler;

import org.antlr.v4.runtime.*;
import org.babyfish.jimmer.dto.compiler.spi.BaseProp;
import org.babyfish.jimmer.dto.compiler.spi.BaseType;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.*;
import java.util.stream.Collectors;

public abstract class DtoCompiler<T extends BaseType, P extends BaseProp> {

    private final T baseType;

    protected DtoCompiler(T baseType) {
        this.baseType = baseType;
    }

    public List<DtoType<T, P>> compile(String code) {
        DtoLexer lexer = new DtoLexer(new ANTLRInputStream(code));
        DtoParser parser = new DtoParser(new CommonTokenStream(lexer));
        DtoErrorListener listener = new DtoErrorListener();
        lexer.removeErrorListeners();
        lexer.addErrorListener(listener);
        parser.removeErrorListeners();
        parser.addErrorListener(listener);
        return parse(parser);
    }

    public List<DtoType<T, P>> compile(Reader reader) throws IOException {
        DtoLexer lexer = new DtoLexer(new ANTLRInputStream(reader));
        DtoParser parser = new DtoParser(new CommonTokenStream(lexer));
        DtoErrorListener listener = new DtoErrorListener();
        lexer.removeErrorListeners();
        lexer.addErrorListener(listener);
        parser.removeErrorListeners();
        parser.addErrorListener(listener);
        return parse(parser);
    }

    public List<DtoType<T, P>> compile(InputStream input) throws IOException {
        DtoLexer lexer = new DtoLexer(new ANTLRInputStream(input));
        DtoParser parser = new DtoParser(new CommonTokenStream(lexer));
        DtoErrorListener listener = new DtoErrorListener();
        lexer.removeErrorListeners();
        lexer.addErrorListener(listener);
        parser.removeErrorListeners();
        parser.addErrorListener(listener);
        return parse(parser);
    }

    private List<DtoType<T, P>> parse(DtoParser parser) {
        List<DtoParser.DtoTypeContext> dtoTypes = parser.dto().dtoTypes;
        int size = dtoTypes.size();
        for (int i = 0; i < size - 1; i++) {
            for (int ii = i + 1; ii < size; ii++) {
                if (dtoTypes.get(i).name.getText().equals(dtoTypes.get(ii).getText())) {
                    throw new DtoAstException(
                            dtoTypes.get(ii).name.getLine(),
                            "Duplicate dto type name \"" + dtoTypes.get(ii).getText() + "\""
                    );
                }
            }
        }
        return dtoTypes
                .stream()
                .map(it -> new DtoTypeBuilder(baseType, false).parse(it))
                .collect(Collectors.toList());
    }

    protected abstract boolean isEntity(T baseType);

    protected abstract T getSuperType(T baseType);

    protected abstract Map<String, P> getDeclaredProps(T baseType);

    protected abstract Map<String, P> getProps(T baseType);

    protected abstract boolean isId(P baseProp);

    protected abstract boolean isKey(P baseProp);

    protected abstract T getTargetType(P baseProp);

    private class DtoTypeBuilder {

        private final T baseType;

        private final boolean hasKey;

        private boolean isInput;

        // Key: Original prop name
        private final Map<String, DtoProp<T, P>> propMap = new LinkedHashMap<>();

        // Key: New prop name
        private final Set<String> finalPropNames = new HashSet<>();

        private final P recursiveBaseProp;

        private final String recursiveAlias;

        DtoTypeBuilder(T baseType, boolean isInput) {
            this(baseType, isInput, null, null);
        }

        DtoTypeBuilder(T baseType, boolean isInput, P recursiveBaseProp, String recursiveAlias) {
            this.baseType = baseType;
            this.hasKey = getProps(baseType).values().stream().anyMatch(DtoCompiler.this::isKey);
            this.isInput = isInput;
            this.recursiveBaseProp = recursiveBaseProp;
            this.recursiveAlias = recursiveAlias;
        }

        DtoType<T, P> parse(DtoParser.DtoTypeContext ctx) {
            if (ctx.modifier != null) {
                if (!ctx.modifier.getText().equals("input")) {
                    throw new DtoAstException(
                            ctx.modifier.getLine(),
                            "If the modifier is specified, it must be \"input\""
                    );
                }
                this.isInput = true;
            }
            return new DtoType<>(parse(ctx.body), ctx.name.getText());
        }

        private DtoType<T, P> parse(DtoParser.DtoBodyContext ctx) {
            DtoParser.MacroContext macro = ctx.macro();
            if (macro != null) {
                parse(macro);
            }
            for (DtoParser.ExplicitPropContext explicitPropCtx : ctx.explicitProps) {
                parse(explicitPropCtx);
            }
            RecursiveDtoProp<T, P> recursiveDtoProp =
                    recursiveBaseProp != null ?
                            new RecursiveDtoProp<>(recursiveBaseProp, recursiveAlias) :
                            null;
            if (recursiveDtoProp != null) {
                propMap.put(recursiveBaseProp.getName(), recursiveDtoProp);
            }
            DtoType<T, P> dtoType = new DtoType<>(baseType, isInput, new ArrayList<>(propMap.values()));
            if (recursiveDtoProp != null) {
                recursiveDtoProp.targetType = dtoType;
            }
            return dtoType;
        }

        private void parse(DtoParser.MacroContext macro) {
            if (!macro.name.getText().equals("allScalars")) {
                throw new DtoAstException(
                        macro.name.getLine(),
                        "If the macro is specified, it must be \"allScalars\""
                );
            }
            List<DtoParser.QualifiedNameContext> args = macro.args;
            Set<T> explicitBaseTypes = null;
            if (!args.isEmpty()) {
                explicitBaseTypes = new LinkedHashSet<>();
                Map<String, List<T>> nameBaseTypeMap = new LinkedHashMap<>();
                Map<String, T> qualifiedNameBaseTypeMap = new LinkedHashMap<>();
                for (T bt = baseType; bt != null; bt = getSuperType(bt)) {
                    nameBaseTypeMap.computeIfAbsent(bt.getName(), it -> new ArrayList<>()).add(bt);
                    qualifiedNameBaseTypeMap.put(bt.getQualifiedName(), bt);
                }
                for (DtoParser.QualifiedNameContext qnCtx : args) {
                    String typeName = qnCtx.parts.stream().map(Token::getText).collect(Collectors.joining("."));
                    List<T> baseTypes = nameBaseTypeMap.get(typeName);
                    T baseType;
                    if (baseTypes != null) {
                        if (baseTypes.size() > 1) {
                            throw new DtoAstException(
                                    qnCtx.start.getLine(),
                                    "The type name \"" +
                                            typeName +
                                            "\" indicates conflict types: " +
                                            baseTypes.size()
                            );
                        }
                        baseType = baseTypes.get(0);
                    } else {
                        baseType = qualifiedNameBaseTypeMap.get(typeName);
                    }
                    if (baseType == null) {
                        throw new DtoAstException(
                                qnCtx.start.getLine(),
                                "The type name \"" + typeName + "\" indicates nothing"
                        );
                    }
                    explicitBaseTypes.add(baseType);
                }
            }
            if (explicitBaseTypes == null) {
                for (P baseProp : getProps(baseType).values()) {
                    if (!baseProp.isTransient() && getTargetType(baseProp) == null) {
                        propMap.put(
                                baseProp.getName(),
                                new DtoPropImpl<>(
                                        baseProp,
                                        null,
                                        null,
                                        isInput && isId(baseProp) && hasKey,
                                        false,
                                        false
                                )
                        );
                        finalPropNames.add(baseProp.getName());
                    }
                }
            } else {
                for (T baseType : explicitBaseTypes) {
                    for (P baseProp : getDeclaredProps(baseType).values()) {
                        if (!baseProp.isTransient() && getTargetType(baseProp) == null) {
                            propMap.put(
                                    baseProp.getName(),
                                    new DtoPropImpl<>(
                                            baseProp,
                                            null,
                                            null,
                                            isInput && isId(baseProp) && hasKey,
                                            false,
                                            false
                                    )
                            );
                            finalPropNames.add(baseProp.getName());
                        }
                    }
                }
            }
        }

        private void parse(DtoParser.ExplicitPropContext ctx) {
            if (ctx.positiveProp() != null) {
                parse(ctx.positiveProp());
            } else {
                parse(ctx.negativeProp());
            }
        }

        private void parse(DtoParser.PositivePropContext ctx) {
            if (recursiveBaseProp != null && ctx.prop.getText().equals(recursiveBaseProp.getName())) {
                throw new DtoAstException(
                        ctx.prop.getLine(),
                        "Cannot specify the property \"" +
                                recursiveBaseProp +
                                "\" here, because it is recursive property of current context"
                );
            }
            boolean idOnly = false;
            if (ctx.func != null) {
                if (!ctx.func.getText().equals("id")) {
                    throw new DtoAstException(
                            ctx.func.getLine(),
                            "The function name must be \"id\""
                    );
                }
                if (!isEntity(baseType)) {
                    throw new DtoAstException(
                            ctx.func.getLine(),
                            "Cannot call function \"id\" because the current base type \"" +
                                    baseType.getQualifiedName() +
                                    "\" is not entity"
                    );
                }
                idOnly = true;
            }
            P baseProp = getProps(baseType).get(ctx.prop.getText());
            if (baseProp == null) {
                throw new DtoAstException(
                        ctx.prop.getLine(),
                        "No property \"" +
                                ctx.prop.getText() +
                                "\" is declared in \"" +
                                baseType.getQualifiedName() +
                                "\""
                );
            }
            if (baseProp.isTransient() && !baseProp.hasTransientResolver()) {
                throw new DtoAstException(
                        ctx.prop.getLine(),
                        "The property \"" +
                                baseProp.getName() +
                                "\" cannot be declared in dto because it is transient " +
                                "but has no transient resolver"
                );
            }
            if (idOnly && getTargetType(baseProp) == null) {
                throw new DtoAstException(
                        ctx.func.getLine(),
                        "Cannot call the function \"id\" because the property \"" +
                                baseProp.getName() +
                                "\" is not association"
                );
            }
            String alias = null;
            if (ctx.alias != null) {
                alias = ctx.alias.getText();
            } else if (idOnly) {
                alias = baseProp.getName() + "Id";
            }
            if (recursiveAlias != null && recursiveAlias.equals(alias)) {
                throw new DtoAstException(
                        ctx.alias.getLine(),
                        "Cannot assign the alias \"" +
                                alias +
                                "\" to the the property \"" +
                                recursiveBaseProp +
                                "\" here, because it is the alias of recursive property of current context"
                );
            }
            if (idOnly && baseProp.isList() && alias == null) {
                throw new DtoAstException(
                        ctx.func.getLine(),
                        "The property \"" +
                                baseProp.getName() +
                                "\" is wrapped by `id(...)`, the alias is required because it is list"
                );
            }
            boolean recursive = ctx.recursive != null;
            if (recursive) {
                T targetType = getTargetType(baseProp);
                if (targetType == null) {
                    throw new DtoAstException(
                            ctx.recursive.getLine(),
                            "The property \"" +
                                    baseProp.getName() +
                                    "\" cannot be recursive because it is not association"
                    );
                }
                if (targetType != baseType) {
                    throw new DtoAstException(
                            ctx.recursive.getLine(),
                            "The property \"" +
                                    baseProp.getName() +
                                    "\" cannot be recursive because does not returns the declaring type \"" +
                                    baseType.getQualifiedName() +
                                    "\""
                    );
                }
            }
            DtoType<T, P> targetDtoType = null;
            if (ctx.dtoBody() != null) {
                if (idOnly) {
                    throw new DtoAstException(
                            ctx.dtoBody().start.getLine(),
                            "Cannot specify the target dto type for id(" +
                                    baseProp.getName() +
                                    ") because it is wrapped by id(...)"
                    );
                }
                if (getTargetType(baseProp) == null) {
                    throw new DtoAstException(
                            ctx.dtoBody().start.getLine(),
                            "Cannot specify the target dto type for \"" +
                                    baseProp.getName() +
                                    "\" because it is not association"
                    );
                }
                targetDtoType = new DtoTypeBuilder(
                        getTargetType(baseProp),
                        isInput,
                        recursive ? baseProp : null,
                        alias
                ).parse(ctx.dtoBody());
            } else if (getTargetType(baseProp) != null && !idOnly) {
                throw new DtoAstException(
                        ctx.dtoBody().start.getLine(),
                        "The target dto type for association \"" +
                                baseProp.getName() +
                                "\" is required"
                );
            }
            propMap.put(
                    baseProp.getName(),
                    new DtoPropImpl<>(
                            baseProp,
                            alias,
                            targetDtoType,
                            recursive,
                            idOnly,
                            recursive
                    )
            );
            String finalName = alias != null ? alias : baseProp.getName();
            Token reportErrorToken = ctx.alias != null ?
                    ctx.alias :
                    ctx.func != null ?
                            ctx.func :
                            ctx.prop;
            if (!finalPropNames.add(finalName)) {
                throw new DtoAstException(
                        reportErrorToken.getLine(),
                        "The duplicate property alias \"" +
                                finalName +
                                "\""
                );
            }
        }

        private void parse(DtoParser.NegativePropContext ctx) {
            P baseProp = getProps(baseType).get(ctx.Identifier().getText());
            if (baseProp == null) {
                throw new DtoAstException(
                        ctx.stop.getLine(),
                        "No property \"" +
                                ctx.Identifier().getText() +
                                "\" is declared in \"" +
                                baseType.getQualifiedName() +
                                "\""
                );
            }
            DtoProp<T, P> prop = propMap.remove(baseProp.getName());
            if (prop != null) {
                finalPropNames.remove(prop.getName());
            }
        }
    }

    private static class RecursiveDtoProp<T extends BaseType, P extends BaseProp> implements DtoProp<T, P> {

        private final P baseProp;

        private final String alias;

        DtoType<T, P> targetType;

        private RecursiveDtoProp(P baseProp, String alias) {
            this.baseProp = baseProp;
            this.alias = alias;
        }

        @Override
        public P getBaseProp() {
            return baseProp;
        }

        @Override
        public String getName() {
            return alias != null ? alias : baseProp.getName();
        }

        @Override
        public boolean isNullable() {
            return true;
        }

        @Override
        public boolean isIdOnly() {
            return false;
        }

        @Nullable
        @Override
        public String getAlias() {
            return alias;
        }

        @Nullable
        @Override
        public DtoType<T, P> getTargetType() {
            return targetType;
        }

        @Override
        public boolean isRecursive() {
            return true;
        }

        @Override
        public boolean isNewTarget() {
            return false;
        }
    }

    private static class DtoErrorListener extends BaseErrorListener {

        @Override
        public void syntaxError(
                Recognizer<?, ?> recognizer,
                Object offendingSymbol,
                int line,
                int charPositionInLine,
                String msg,
                RecognitionException ex) {
            throw new DtoAstException(line, msg);
        }
    }
}
