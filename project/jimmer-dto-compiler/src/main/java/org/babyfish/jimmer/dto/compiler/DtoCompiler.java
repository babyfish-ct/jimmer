package org.babyfish.jimmer.dto.compiler;

import org.antlr.v4.runtime.*;
import org.babyfish.jimmer.dto.compiler.spi.BaseProp;
import org.babyfish.jimmer.dto.compiler.spi.BaseType;

import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.*;
import java.util.stream.Collectors;

public abstract class DtoCompiler<T extends BaseType, P extends BaseProp> {

    private final DtoFile dtoFile;

    private T baseType;

    private final DtoParser.DtoContext ast;

    private final String sourceTypeName;

    private final String targetPackageName;

    protected DtoCompiler(DtoFile dtoFile) throws IOException {
        this.dtoFile = dtoFile;
        try (Reader reader = dtoFile.openReader()) {
            DtoLexer lexer = new DtoLexer(new ANTLRInputStream(reader));
            DtoParser parser = new DtoParser(new CommonTokenStream(lexer));
            DtoErrorListener listener = new DtoErrorListener();
            lexer.removeErrorListeners();
            lexer.addErrorListener(listener);
            parser.removeErrorListeners();
            parser.addErrorListener(listener);
            this.ast = parser.dto();
            DtoParser.ExportStatementContext export = ast.exportStatement();
            String sourceTypeName = null;
            String targetPackageName = null;
            if (export != null) {
                List<Token> typeParts = export.typeParts;
                if (typeParts.size() == 1) {
                    sourceTypeName = dtoFile.getPackageName() + '.' + typeParts.get(0).getText();
                } else {
                    sourceTypeName = typeParts.stream().map(Token::getText).collect(Collectors.joining("."));
                }
                List<Token> packageParts = export.packageParts;
                if (packageParts.isEmpty()) {
                    int lastIndex = sourceTypeName.lastIndexOf('.');
                    targetPackageName = lastIndex != -1 ? sourceTypeName.substring(0, lastIndex) + ".dto" : "";
                } else {
                    targetPackageName = packageParts.stream().map(Token::getText).collect(Collectors.joining("."));
                }
            }
            if (sourceTypeName == null) {
                String name = dtoFile.getName();
                sourceTypeName = dtoFile.getPackageName() + '.' + name.substring(0, name.length() - 4);
            }
            this.sourceTypeName = sourceTypeName;
            this.targetPackageName = targetPackageName;
        }
    }

    public T getBaseType() {
        if (baseType == null) {
            throw new IllegalStateException("baseType has not be set");
        }
        return baseType;
    }

    public DtoFile getDtoFile() {
        return dtoFile;
    }

    public String getSourceTypeName() {
        return sourceTypeName;
    }

    public String getTargetPackageName() {
        return targetPackageName;
    }

    public List<DtoType<T, P>> compile(T baseType) {
        this.baseType = baseType;
        CompilerContext<T, P> ctx = new CompilerContext<>(this);
        for (DtoParser.ImportStatementContext importStatement : ast.importStatements) {
            ctx.importStatement(importStatement);
        }
        for (DtoParser.DtoTypeContext dtoType : ast.dtoTypes) {
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

    protected abstract boolean isStringProp(P baseProp);

    protected abstract boolean isSameType(P baseProp1, P baseProp2);

    protected abstract Integer getGenericTypeCount(String qualifiedName);

    DtoAstException exception(int line, int col, String message) {
        return new DtoAstException(dtoFile, line, col, message);
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
            throw exception(line, charPositionInLine, msg);
        }
    }
}
