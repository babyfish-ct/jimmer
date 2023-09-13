package org.babyfish.jimmer.dto.compiler;

import org.antlr.v4.runtime.*;
import org.babyfish.jimmer.dto.compiler.spi.BaseProp;
import org.babyfish.jimmer.dto.compiler.spi.BaseType;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.*;

public abstract class DtoCompiler<T extends BaseType, P extends BaseProp> {

    private final T baseType;

    private final String dtoFilePath;

    protected DtoCompiler(T baseType, String dtoFilePath) {
        this.baseType = baseType;
        this.dtoFilePath = dtoFilePath;
    }

    public T getBaseType() {
        return baseType;
    }

    public String getDtoFilePath() {
        return dtoFilePath;
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
        CompilerContext<T, P> ctx = new CompilerContext<>(this);
        DtoParser.DtoContext dto = parser.dto();
        for (DtoParser.ImportStatementContext importStatement : dto.importStatements) {
            ctx.importStatement(importStatement);
        }
        for (DtoParser.DtoTypeContext dtoType : dto.dtoTypes) {
            ctx.add(dtoType);
        }
        return ctx.getDtoTypes();
    }

    protected abstract Collection<T> getSuperTypes(T baseType);

    protected abstract Map<String, P> getDeclaredProps(T baseType);

    protected abstract Map<String, P> getProps(T baseType);

    protected abstract T getTargetType(P baseProp);

    protected abstract boolean isGeneratedValue(P baseProp);

    protected abstract List<String> getEnumConstants(P baseProp);

    DtoAstException exception(int line, String message) {
        return new DtoAstException(dtoFilePath, line, message);
    }

    private class DtoErrorListener extends BaseErrorListener {

        @Override
        public void syntaxError(
                Recognizer<?, ?> recognizer,
                Object offendingSymbol,
                int line,
                int charPositionInLine,
                String msg,
                RecognitionException ex) {
            throw exception(line, msg);
        }
    }
}
