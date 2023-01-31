package org.babyfish.jimmer.meta.impl.dto.ast;

import org.antlr.v4.runtime.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class Dto {

    private final boolean isInput;

    @Nullable
    private final String name;

    private final boolean allScalars;

    @Nullable
    private final Set<String> allScalarImmutableTypes;

    private final List<DtoProp> explicitProps;

    private Dto(boolean isInput, boolean allScalars, @Nullable Set<String> allScalarImmutableTypes, List<DtoProp> explicitProps) {
        this.isInput = isInput;
        this.name = null;
        this.allScalars = allScalars;
        this.allScalarImmutableTypes = allScalarImmutableTypes;
        this.explicitProps = explicitProps;
    }

    private Dto(Dto base, @NotNull String name) {
        this.isInput = base.isInput;
        this.name = name;
        this.allScalars = base.allScalars;
        this.allScalarImmutableTypes = base.allScalarImmutableTypes;
        this.explicitProps = base.explicitProps;
    }

    public boolean isInput() {
        return isInput;
    }

    @Nullable
    public String getName() {
        return name;
    }

    public boolean isAllScalars() {
        return allScalars;
    }

    @Nullable
    public Set<String> getAllScalarImmutableTypes() {
        return allScalarImmutableTypes;
    }

    @Override
    public String toString() {
        return "Dto{" +
                "isInput=" + isInput +
                ", name='" + name + '\'' +
                ", allScalars=" + allScalars +
                ", allScalarImmutableTypes=" + allScalarImmutableTypes +
                ", explicitProps=" + explicitProps +
                '}';
    }

    public static List<Dto> parse(String code) {
        DtoLexer lexer = new DtoLexer(new ANTLRInputStream(code));
        DtoParser parser = new DtoParser(new CommonTokenStream(lexer));
        DtoErrorListener listener = new DtoErrorListener();
        lexer.removeErrorListeners();
        lexer.addErrorListener(listener);
        parser.removeErrorListeners();
        parser.addErrorListener(listener);
        return parse(parser);
    }

    public static List<Dto> parse(Reader reader) throws IOException {
        DtoLexer lexer = new DtoLexer(new ANTLRInputStream(reader));
        DtoParser parser = new DtoParser(new CommonTokenStream(lexer));
        DtoErrorListener listener = new DtoErrorListener();
        lexer.removeErrorListeners();
        lexer.addErrorListener(listener);
        parser.removeErrorListeners();
        parser.addErrorListener(listener);
        return parse(parser);
    }

    public static List<Dto> parse(InputStream input) throws IOException {
        DtoLexer lexer = new DtoLexer(new ANTLRInputStream(input));
        DtoParser parser = new DtoParser(new CommonTokenStream(lexer));
        DtoErrorListener listener = new DtoErrorListener();
        lexer.removeErrorListeners();
        lexer.addErrorListener(listener);
        parser.removeErrorListeners();
        parser.addErrorListener(listener);
        return parse(parser);
    }

    private static List<Dto> parse(DtoParser parser) {
        ParsingContext parsingContext = new ParsingContext();
        return parser.dto().dtoTypes.stream().map(parsingContext::parse).collect(Collectors.toList());
    }

    private static class ParsingContext {

        private boolean underInput = false;

        private Dto parse(DtoParser.DtoTypeContext ctx) {
            if (ctx.modifier != null) {
                if (!ctx.modifier.getText().equals("input")) {
                    throw new DtoAstException(
                            ctx.modifier.getLine(),
                            "If the modifier is specified, it must be \"input\""
                    );
                }
                underInput = true;
                try {
                    return new Dto(parse(ctx.body), ctx.name.getText());
                } finally {
                    underInput = false;
                }
            }
            return new Dto(parse(ctx.body), ctx.name.getText());
        }

        private Dto parse(DtoParser.DtoBodyContext ctx) {
            boolean allScalars = false;
            Set<String> allScalarTypeNames = null;
            DtoParser.MacroContext macro = ctx.macro();
            if (macro != null) {
                if (!macro.name.getText().equals("allScalars")) {
                    throw new DtoAstException(
                            macro.name.getLine(),
                            "If the macro is specified, it must be \"allScalars\""
                    );
                }
                allScalars = true;
                List<Token> args = macro.args;
                if (!args.isEmpty()) {
                    allScalarTypeNames = args.stream().map(Token::getText).collect(Collectors.toSet());
                    if (allScalarTypeNames.size() < args.size()) {
                        int size = macro.args.size();
                        for (int i = 0; i < size; i++) {
                            for (int ii = i + 1; ii < size; ii++) {
                                if (args.get(i).getText().equals(args.get(ii).getText())) {
                                    throw new DtoAstException(
                                            args.get(ii).getLine(),
                                            "The macro has duplicated argument \"" + args.get(ii).getText() + "\""
                                    );
                                }
                            }
                        }
                    }
                }
            }
            List<DtoProp> explicitProps = ctx.explicitProps.stream().map(this::parse).collect(Collectors.toList());
            return new Dto(
                    underInput,
                    allScalars,
                    allScalarTypeNames,
                    explicitProps
            );
        }

        private DtoProp parse(DtoParser.ExplicitPropContext ctx) {
            if (ctx.positiveProp() != null) {
                return parse(ctx.positiveProp());
            }
            return parse(ctx.negativeProp());
        }

        private DtoProp parse(DtoParser.PositivePropContext ctx) {
            boolean idOnly = false;
            if (ctx.func != null) {
                if (!ctx.func.getText().equals("id")) {
                    throw new DtoAstException(
                            ctx.func.getLine(),
                            "The function name must be \"id\""
                    );
                }
                idOnly = true;
            }
            String prop = ctx.prop.getText();
            String alias = null;
            if (ctx.alias != null) {
                alias = ctx.alias.getText();
            }
            Dto targetDto = null;
            if (ctx.dtoBody() != null) {
                targetDto = parse(ctx.dtoBody());
            }
            return new DtoProp(
                    ctx.prop.getText(),
                    idOnly,
                    alias,
                    targetDto,
                    ctx.recursive != null
            );
        }

        private DtoProp parse(DtoParser.NegativePropContext ctx) {
            return new DtoProp(true, ctx.Identifier().getText());
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
