package org.babyfish.jimmer.meta.impl.dto.ast;

import org.antlr.v4.runtime.*;
import org.babyfish.jimmer.meta.impl.dto.ast.spi.BaseProp;
import org.babyfish.jimmer.meta.impl.dto.ast.spi.BaseType;

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
        DtoTypeBuilder compileContext = new DtoTypeBuilder(baseType, false);
        return parser.dto().dtoTypes.stream().map(compileContext::parse).collect(Collectors.toList());
    }

    protected abstract boolean isEntity(T baseType);

    protected abstract T getSuperType(T baseType);

    protected abstract Map<String, P> getDeclaredProps(T baseType);

    protected abstract Map<String, P> getProps(T baseType);

    protected abstract boolean isMappable(P baseProp);

    protected abstract boolean isNullable(P baseProp);

    protected abstract boolean isId(P baseProp);

    protected abstract boolean isList(P baseProp);

    protected abstract T getTargetType(P baseProp);

    private class DtoTypeBuilder {

        private final T baseType;

        private boolean isInput;

        private final Map<String, DtoProp<T, P>> propMap = new LinkedHashMap<>();

        private DtoTypeBuilder(T baseType, boolean isInput) {
            this.baseType = baseType;
            this.isInput = isInput;
        }

        private DtoType<T, P> parse(DtoParser.DtoTypeContext ctx) {
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
            return new DtoType<>(baseType, isInput, new ArrayList<>(propMap.values()));
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
                    if (isMappable(baseProp) && getTargetType(baseProp) == null) {
                        propMap.put(
                                baseProp.getName(),
                                new DtoProp<>(
                                        baseProp,
                                        null,
                                        null,
                                        isInput && isId(baseProp),
                                        false,
                                        false
                                )
                        );
                    }
                }
            } else {
                for (T baseType : explicitBaseTypes) {
                    for (P baseProp : getDeclaredProps(baseType).values()) {
                        if (isMappable(baseProp) && getTargetType(baseProp) == null) {
                            propMap.put(
                                    baseProp.getName(),
                                    new DtoProp<>(
                                            baseProp,
                                            null,
                                            null,
                                            isInput && isId(baseProp),
                                            false,
                                            false
                                    )
                            );
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
            if (idOnly && isList(baseProp) && alias == null) {
                throw new DtoAstException(
                        ctx.func.getLine(),
                        "The property \"" +
                                baseProp.getName() +
                                "\" is wrapped by `id(...)`, the alias is required because it is list"
                );
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
                targetDtoType = new DtoTypeBuilder(getTargetType(baseProp), isInput).parse(ctx.dtoBody());
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
                    new DtoProp<T, P>(
                            baseProp,
                            alias,
                            targetDtoType,
                            ctx.recursive != null,
                            idOnly,
                            ctx.recursive != null
                    )
            );
        }

        private DtoProp<T, P> parse(DtoParser.NegativePropContext ctx) {
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
            return new DtoProp<>(baseProp, true);
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
