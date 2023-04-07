package org.babyfish.jimmer.sql.fetcher.compiler;

import org.antlr.v4.runtime.*;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.TargetLevel;
import org.babyfish.jimmer.sql.Entity;
import org.babyfish.jimmer.sql.fetcher.*;
import org.babyfish.jimmer.sql.fetcher.impl.FetcherImpl;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class FetcherCompiler {

    private static final Object JAVA_CODE_VALUE = new Object();

    private FetcherCompiler() {}

    public static Fetcher<?> compile(String code) {
        return compile(code, null);
    }

    @SuppressWarnings("unchecked")
    public static Fetcher<?> compile(String code, ClassLoader classLoader) {
        FetcherLexer lexer = new FetcherLexer(
                new ANTLRInputStream(code)
        );
        lexer.removeErrorListeners();
        lexer.addErrorListener(new ErrorListenerImpl());
        FetcherParser parser = new FetcherParser(
                new CommonTokenStream(lexer)
        );
        parser.removeErrorListeners();
        parser.addErrorListener(new ErrorListenerImpl());

        FetcherParser.FetcherContext ctx = parser.fetcher();
        Fetcher<?> fetcher = new FetcherImpl<>((Class<Object>) type(ctx, classLoader));
        fetcher = addFields(fetcher, ctx.body);
        return fetcher;
    }

    private static Class<?> type(FetcherParser.FetcherContext ctx, ClassLoader classLoader) {
        List<Token> parts = ctx.type.parts;
        String typeName = parts
                .stream().map(Token::getText)
                .collect(Collectors.joining("."));
        Class<?> type;
        try {
            type = classLoader != null ?
                    Class.forName(typeName, true, classLoader) :
                    Class.forName(typeName);
        } catch (ClassNotFoundException ex) {
            throw new FetcherCompileException(
                    "There is no type \"" +
                            typeName +
                            "\"",
                    ex,
                    parts.get(0).getLine(),
                    parts.get(0).getCharPositionInLine()
            );
        }
        if (!type.isInterface() || !type.isAnnotationPresent(Entity.class)) {
            throw new FetcherCompileException(
                    "The \"" +
                            typeName +
                            "\" is not entity type",
                    parts.get(0).getLine(),
                    parts.get(0).getCharPositionInLine()
            );
        }
        return type;
    }

    private static Fetcher<?> addFields(Fetcher<?> fetcher, FetcherParser.FetchBodyContext body) {
        for (FetcherParser.FieldContext field : body.fields) {
            fetcher = addField(fetcher, field);
        }
        return fetcher;
    }

    @SuppressWarnings("unchecked")
    private static Fetcher<?> addField(Fetcher<?> fetcher, FetcherParser.FieldContext field) {
        String propName = field.prop.getText();
        ImmutableProp prop = fetcher.getImmutableType().getProps().get(propName);
        if (prop == null) {
            throw new FetcherCompileException(
                    "There is no property \"" +
                            propName +
                            "\" declared in \"" +
                            fetcher.getImmutableType() +
                            "\"",
                    field.prop.getLine(),
                    field.prop.getCharPositionInLine()
            );
        }
        if (prop.isId()) {
            return fetcher;
        }
        int limit = Integer.MAX_VALUE;
        int offset = 0;
        int depth = 0;
        int batchSize = 0;
        boolean recursive = false;
        for (FetcherParser.ArgumentContext argument : field.arguments) {
            Object value = parseValue(argument.value.getText());
            if (value == null) {
                throw new FetcherCompileException(
                        "Illegal value expression \"" +
                                argument.value.getText(),
                        argument.value.getLine(),
                        argument.value.getCharPositionInLine()
                );
            }
            switch (argument.name.getText()) {
                case "batchSize":
                    if (!(value instanceof Integer)) {
                        throw new FetcherCompileException(
                                "Illegal value of `batchSize` must be integer",
                                argument.value.getLine(),
                                argument.value.getCharPositionInLine()
                        );
                    }
                    batchSize = (Integer)value;
                    break;
                case "limit":
                    if (!(value instanceof Integer)) {
                        throw new FetcherCompileException(
                                "Illegal value of `limit` must be integer",
                                argument.value.getLine(),
                                argument.value.getCharPositionInLine()
                        );
                    }
                    limit = (Integer)value;
                    break;
                case "offset":
                    if (!(value instanceof Integer)) {
                        throw new FetcherCompileException(
                                "Illegal value of `offset` must be integer",
                                argument.value.getLine(),
                                argument.value.getCharPositionInLine()
                        );
                    }
                    offset = (Integer)value;
                    break;
                case "depth":
                    if (!(value instanceof Integer)) {
                        throw new FetcherCompileException(
                                "Illegal value of `depth` must be integer",
                                argument.value.getLine(),
                                argument.value.getCharPositionInLine()
                        );
                    }
                    depth = (Integer)value;
                    break;
                case "recursive":
                    if (value == JAVA_CODE_VALUE) {
                        throw new FetcherCompileException.CodeBasedRecursionException(
                                "For fetcher which will be serialized and sent to remote microservice, " +
                                        "argument value of `recursive` cannot be java code",
                                argument.value.getLine(),
                                argument.value.getCharPositionInLine()
                        );
                    }
                    if (!(value instanceof Boolean)) {
                        throw new FetcherCompileException(
                                "Illegal value of `recursive` must be integer",
                                argument.value.getLine(),
                                argument.value.getCharPositionInLine()
                        );
                    }
                    recursive = (Boolean) value;
                    break;
                case "filter":
                    throw new FetcherCompileException.CodeBasedFilterException(
                            "For fetcher which will be serialized and sent to remote microservice, " +
                                    "argument `filter` is not supported",
                            argument.value.getLine(),
                            argument.value.getCharPositionInLine()
                    );
                default:
                    throw new FetcherCompileException(
                            "Unsupported fetcher field argument \"" +
                                    argument.name.getText() +
                                    "\"",
                            argument.value.getLine(),
                            argument.value.getCharPositionInLine()
                    );
            }
        }
        if ((batchSize != 0 || limit != Integer.MAX_VALUE || offset != 0) &&
                !prop.isAssociation(TargetLevel.PERSISTENT)) {
            throw new FetcherCompileException(
                    "The field argument \"batchSize\", \"limit\" or \"offset\" " +
                            "can not be specified for non-remote association \"" +
                            prop +
                            "\"",
                    field.prop.getLine(),
                    field.prop.getCharPositionInLine()
            );
        }
        if ((depth != 0 || recursive) && prop.getTargetType() != prop.getDeclaringType()) {
            throw new FetcherCompileException(
                    "The field argument \"depth\" or \"recursive\" can not be specified for non-recursive association \"" +
                            prop +
                            "\"",
                    field.prop.getLine(),
                    field.prop.getCharPositionInLine()
            );
        }
        Fetcher<?> childFetcher = null;
        FetcherParser.FetchBodyContext body = field.body;
        if (body != null) {
            if (!prop.isAssociation(TargetLevel.ENTITY) || (prop.isTransient() && !prop.hasTransientResolver())) {
                throw new FetcherCompileException(
                        "The child fetcher can not be specified for non-association property \"" +
                                prop +
                                "\"",
                        field.prop.getLine(),
                        field.prop.getCharPositionInLine()
                );
            }
            childFetcher = new FetcherImpl<>((Class<Object>) prop.getTargetType().getJavaClass());
            childFetcher = addFields(childFetcher, body);
        }
        if (childFetcher == null) {
            return fetcher.add(propName);
        }
        return fetcher.add(
                propName,
                childFetcher,
                cfgBlock(
                        batchSize,
                        limit,
                        offset,
                        depth,
                        recursive
                )
        );
    }

    private static Object parseValue(String value) {
        if ("<java-code>".equals(value)) {
            return JAVA_CODE_VALUE;
        }
        if ("true".equals(value)) {
            return true;
        }
        if ("false".equals(value)) {
            return false;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static Consumer<FieldConfig<?, ?>> cfgBlock(
            int batchSize,
            int limit,
            int offset,
            int depth,
            boolean recursive
    ) {
        Consumer<FieldConfig<?, ?>> cfgBlock = null;
        if (batchSize == 0 && limit == Integer.MAX_VALUE && offset == 0 && depth == 0 && !recursive) {
            return null;
        }
        return cfg -> {
            if (batchSize != 0) {
                cfg.batch(batchSize);
            }
            if (limit != Integer.MAX_VALUE || offset != 0) {
                if ((long)offset + limit > Integer.MAX_VALUE) {
                    ((ListFieldConfig<?, ?>)cfg).limit(Integer.MAX_VALUE - offset, offset);
                } else {
                    ((ListFieldConfig<?, ?>) cfg).limit(limit, offset);
                }
            }
            if (depth != 0) {
                ((RecursiveFieldConfig<?, ?>)cfg).depth(depth);
            }
            if (recursive) {
                ((RecursiveFieldConfig<?, ?>)cfg).recursive();
            }
        };
    }

    private static class ErrorListenerImpl extends BaseErrorListener {

        @Override
        public void syntaxError(
                Recognizer<?, ?> recognizer,
                Object offendingSymbol,
                int line,
                int charPositionInLine,
                String msg,
                RecognitionException ex
        ) {
            throw new FetcherCompileException(
                    msg,
                    ex,
                    line,
                    charPositionInLine
            );
        }
    }
}
