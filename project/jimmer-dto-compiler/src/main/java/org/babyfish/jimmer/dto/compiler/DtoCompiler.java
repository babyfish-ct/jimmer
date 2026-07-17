package org.babyfish.jimmer.dto.compiler;

import org.antlr.v4.runtime.*;
import org.babyfish.jimmer.dto.compiler.spi.BaseProp;
import org.babyfish.jimmer.dto.compiler.spi.BaseType;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public abstract class DtoCompiler<T extends BaseType, P extends BaseProp> {

    private final DtoFile dtoFile;

    private T baseType;

    private final DtoParser.DtoContext ast;

    private final String sourceTypeName;

    private final String targetPackageName;

    private final String defaultBasePackageName;

    private final boolean explicitSourceType;

    protected DtoCompiler(DtoFile dtoFile) throws IOException {
        this.dtoFile = dtoFile;
        DtoLexer lexer = new DtoLexer(CharStreams.fromReader(dtoFile.openReader()));
        DtoParser parser = new DtoParser(new CommonTokenStream(lexer));
        DtoErrorListener listener = new DtoErrorListener();
        lexer.removeErrorListeners();
        lexer.addErrorListener(listener);
        parser.removeErrorListeners();
        parser.addErrorListener(listener);
        this.ast = parser.dto();
        DtoParser.ExportStatementContext export = ast.exportStatement();
        DtoParser.PackageStatementContext packageStatement = ast.packageStatement();
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
        if (packageStatement != null) {
            if (targetPackageName != null) {
                throw exception(
                        packageStatement.start.getLine(),
                        packageStatement.start.getCharPositionInLine(),
                        "The package cannot be specified by both 'package' and 'export'"
                );
            }
            targetPackageName = packageStatement.packageParts
                    .stream()
                    .map(Token::getText)
                    .collect(Collectors.joining("."));
        }
        if (sourceTypeName == null) {
            String name = dtoFile.getName();
            sourceTypeName = dtoFile.getPackageName() + '.' + name.substring(0, name.length() - 4);
        }
        int lastDotIndex = sourceTypeName.lastIndexOf('.');
        String defaultBasePackageName = lastDotIndex != -1 ?
                sourceTypeName.substring(0, lastDotIndex) :
                "";
        if (targetPackageName == null) {
            targetPackageName = DtoType.defaultPackageName(defaultBasePackageName);
        }
        this.sourceTypeName = sourceTypeName;
        this.targetPackageName = targetPackageName;
        this.defaultBasePackageName = defaultBasePackageName;
        this.explicitSourceType = export != null;
    }

    public T getBaseType() {
        if (baseType == null) {
            throw new IllegalStateException("baseType has not be set");
        }
        return baseType;
    }

    @Nullable
    T baseTypeOrNull() {
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

    public boolean isExplicitSourceType() {
        return explicitSourceType;
    }

    String getDefaultBasePackageName() {
        return defaultBasePackageName;
    }

    public List<DtoType<T, P>> compile(@Nullable T baseType) {
        Map<DtoCompiler<T, P>, T> compilerMap = new LinkedHashMap<>();
        compilerMap.put(this, baseType);
        return compileAll(compilerMap).get(this);
    }

    public static <
            T extends BaseType,
            P extends BaseProp,
            C extends DtoCompiler<T, P>
            > Map<C, List<DtoType<T, P>>> compileAll(Map<C, T> compilerMap) {
        return compileAll(compilerMap.keySet(), compilerMap, it -> true);
    }

    public static <
            T extends BaseType,
            P extends BaseProp,
            C extends DtoCompiler<T, P>
            > Map<C, List<DtoType<T, P>>> compileAll(
            Collection<C> compilers,
            Predicate<String> targetTypeNameFilter
    ) {
        return compileAll(compilers, Collections.emptyMap(), targetTypeNameFilter);
    }

    private static <
            T extends BaseType,
            P extends BaseProp,
            C extends DtoCompiler<T, P>
            > Map<C, List<DtoType<T, P>>> compileAll(
            Collection<C> compilers,
            Map<C, T> baseTypeMap,
            Predicate<String> targetTypeNameFilter
    ) {
        DtoFragmentRegistry<T, P> fragmentRegistry = new DtoFragmentRegistry<>();
        Set<String> sourceDtoTypeNames = new LinkedHashSet<>();
        Set<String> inactiveSourceDtoTypeNames = new LinkedHashSet<>();
        Set<String> inactiveFragmentTypeNames = new LinkedHashSet<>();
        Map<String, Boolean> targetTypeActivationMap = new HashMap<>();
        Map<C, CompilerContext<T, P>> ctxMap = new LinkedHashMap<>();
        for (C compiler : compilers) {
            DtoCompiler<T, P> rawCompiler = compiler;
            rawCompiler.baseType = baseTypeMap.get(compiler);
            CompilerContext<T, P> ctx = new CompilerContext<>(
                    compiler,
                    fragmentRegistry,
                    sourceDtoTypeNames,
                    inactiveSourceDtoTypeNames,
                    inactiveFragmentTypeNames
            );
            for (DtoParser.ImportStatementContext importStatement : rawCompiler.ast.importStatements) {
                ctx.importStatement(importStatement);
            }
            ctxMap.put(compiler, ctx);
        }
        Map<C, Map<DtoParser.DtoTypeContext, T>> dtoTargetMap = new LinkedHashMap<>();
        for (Map.Entry<C, CompilerContext<T, P>> e : ctxMap.entrySet()) {
            C compiler = e.getKey();
            DtoCompiler<T, P> rawCompiler = compiler;
            CompilerContext<T, P> ctx = e.getValue();
            Map<DtoParser.DtoTypeContext, T> targets = new LinkedHashMap<>();
            for (DtoParser.DtoTypeContext dtoType : rawCompiler.ast.dtoTypes) {
                String dtoQualifiedName = ctx.getDtoQualifiedName(dtoType.name.getText());
                String targetTypeName = ctx.resolveTargetTypeName(dtoType.targetType);
                if (targetTypeActivationMap.computeIfAbsent(targetTypeName, targetTypeNameFilter::test)) {
                    sourceDtoTypeNames.add(dtoQualifiedName);
                    targets.put(
                            dtoType,
                            ctx.resolveTargetType(
                                    targetTypeName,
                                    dtoType.targetType,
                                    dtoType.name,
                                    "dto type"
                            )
                    );
                } else {
                    inactiveSourceDtoTypeNames.add(dtoQualifiedName);
                }
            }
            dtoTargetMap.put(compiler, targets);
        }
        for (Map.Entry<C, CompilerContext<T, P>> e : ctxMap.entrySet()) {
            DtoCompiler<T, P> compiler = e.getKey();
            CompilerContext<T, P> ctx = e.getValue();
            for (DtoParser.DtoFragmentContext fragment : compiler.ast.fragments) {
                String fragmentQualifiedName = ctx.getDtoQualifiedName(fragment.name.getText());
                String targetTypeName = ctx.resolveTargetTypeName(fragment.targetType);
                if (targetTypeActivationMap.computeIfAbsent(targetTypeName, targetTypeNameFilter::test)) {
                    T targetType = ctx.resolveTargetType(
                            targetTypeName,
                            fragment.targetType,
                            fragment.name,
                            "fragment"
                    );
                    fragmentRegistry.add(ctx, fragment, targetType);
                } else {
                    inactiveFragmentTypeNames.add(fragmentQualifiedName);
                }
            }
        }
        fragmentRegistry.validate();
        Map<C, List<DtoType<T, P>>> resultMap = new LinkedHashMap<>();
        for (Map.Entry<C, CompilerContext<T, P>> e : ctxMap.entrySet()) {
            DtoCompiler<T, P> compiler = e.getKey();
            for (Map.Entry<DtoParser.DtoTypeContext, T> targetEntry : dtoTargetMap.get(compiler).entrySet()) {
                e.getValue().add(targetEntry.getKey(), targetEntry.getValue());
            }
            resultMap.put(e.getKey(), e.getValue().getDtoTypes());
        }
        return resultMap;
    }

    protected abstract Collection<T> getSuperTypes(T baseType);

    protected boolean isImmutableType(String qualifiedName) {
        return getType(qualifiedName) != null;
    }

    @Nullable
    protected T getType(String qualifiedName) {
        T baseType = baseTypeOrNull();
        if (baseType == null) {
            return null;
        }
        if (baseType.getQualifiedName().equals(qualifiedName)) {
            return baseType;
        }
        for (T superType : getSuperTypes(baseType)) {
            if (superType.getQualifiedName().equals(qualifiedName)) {
                return superType;
            }
        }
        return null;
    }

    protected Collection<T> getDirectSubTypes(T baseType) {
        return Collections.emptyList();
    }

    protected boolean isSameType(T baseType1, T baseType2) {
        return baseType1.getQualifiedName().equals(baseType2.getQualifiedName());
    }

    protected boolean isInstantiable(T baseType) {
        return true;
    }

    protected abstract Map<String, P> getDeclaredProps(T baseType);

    protected abstract Map<String, P> getProps(T baseType);

    protected abstract T getTargetType(P baseProp);

    @Nullable
    protected abstract P getIdProp(T baseType);

    protected abstract boolean isGeneratedValue(P baseProp);

    protected abstract List<String> getEnumConstants(P baseProp);

    protected abstract SimplePropType getSimplePropType(P baseProp);

    protected abstract SimplePropType getSimplePropType(PropConfig.PathNode<P> pathNode);

    protected abstract boolean isSameType(P baseProp1, P baseProp2);

    protected abstract Integer getGenericTypeCount(String qualifiedName);

    DtoAstException exception(int line, int col, String message) {
        return new DtoAstException(dtoFile, line, col, message);
    }

    public DtoModifier getDefaultNullableInputModifier() {
        return DtoModifier.STATIC;
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
