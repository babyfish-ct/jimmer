package org.babyfish.jimmer.dto.compiler;

import org.antlr.v4.runtime.Token;
import org.babyfish.jimmer.dto.compiler.spi.BaseProp;
import org.babyfish.jimmer.dto.compiler.spi.BaseType;

import java.util.*;
import java.util.stream.Collectors;

class DtoTypeBuilder<T extends BaseType, P extends BaseProp> {

    final T baseType;

    final CompilerContext<T, P> ctx;

    final Token name;

    final Token bodyStart;

    final Set<DtoTypeModifier> modifiers;

    final List<Token> superNames;

    final P recursiveBaseProp;

    final String recursiveAlias;

    final Map<String, DtoPropBuilder<T, P>> autoScalarPropMap = new LinkedHashMap<>();

    final Map<P, DtoPropBuilder<T, P>> positivePropMap = new LinkedHashMap<>();

    final Map<String, AbstractPropBuilder> aliasPositivePropMap = new LinkedHashMap<>();

    final List<DtoPropBuilder<T, P>> flatPositiveProps = new ArrayList<>();

    final Map<String, Boolean> negativePropAliasMap = new LinkedHashMap<>();

    final List<Token> negativePropAliasTokens = new ArrayList<>();

    private List<DtoTypeBuilder<T, P>> superTypeBuilders;

    private DtoType<T, P> dtoType;

    private AliasPattern currentAliasGroup;

    private Map<String, AbstractProp> declaredProps;

    DtoTypeBuilder(
            T baseType,
            DtoParser.DtoBodyContext body,
            Token name,
            Set<DtoTypeModifier> modifiers,
            List<Token> superNames,
            P recursiveBaseProp,
            String recursiveAlias,
            CompilerContext<T, P> ctx
    ) {
        this.baseType = baseType;
        this.ctx = ctx;
        this.name = name;
        this.bodyStart = body.start;
        this.modifiers = modifiers;
        this.superNames = superNames;
        this.recursiveBaseProp = recursiveBaseProp;
        this.recursiveAlias = recursiveAlias;
        for (DtoParser.ExplicitPropContext prop : body.explicitProps) {
            if (prop.allScalars() != null) {
                handleAllScalars(prop.allScalars());
            } else if (prop.aliasGroup() != null) {
                handleAliasGroup(prop.aliasGroup());
            } else if (prop.positiveProp() != null) {
                handlePositiveProp(prop.positiveProp());
            } else if (prop.negativeProp() != null) {
                handleNegativeProp(prop.negativeProp());
            } else {
                handleUserProp(prop.userProp());
            }
        }
    }

    public boolean isAbstract() {
        return modifiers.contains(DtoTypeModifier.ABSTRACT);
    }

    private void handleAllScalars(DtoParser.AllScalarsContext allScalars) {
        if (!allScalars.name.getText().equals("allScalars")) {
            throw ctx.exception(
                    allScalars.name.getLine(),
                    "Illegal allScalars name \"" +
                            allScalars.name.getText() +
                            "\", it must be \"allScalars\""
            );
        }

        if (!positivePropMap.isEmpty()) {
            throw ctx.exception(
                    allScalars.name.getLine(),
                    "`#allScalars` must be defined at the beginning"
            );
        }

        if (allScalars.args.isEmpty()) {
            for (P baseProp : ctx.getProps(baseType).values()) {
                if (isAutoScalar(baseProp)) {
                    autoScalarPropMap.put(
                            baseProp.getName(),
                            new DtoPropBuilder<>(
                                    this,
                                    baseProp,
                                    allScalars.start.getLine(),
                                    allScalars.optional != null
                            )
                    );
                }
            }
        } else {
            Map<String, T> qualifiedNameTypeMap = new HashMap<>();
            Map<String, Set<T>> nameTypeMap = new HashMap<>();
            collectSuperTypes(baseType, qualifiedNameTypeMap, nameTypeMap);
            Set<T> handledBaseTypes = new LinkedHashSet<>();
            for (DtoParser.QualifiedNameContext qnCtx : allScalars.args) {
                String qualifiedName = qnCtx.parts.stream().map(Token::getText).collect(Collectors.joining("."));
                T baseType = qualifiedNameTypeMap.get(qualifiedName);
                if (baseType == null) {
                    Set<T> baseTypes = nameTypeMap.get(qualifiedName);
                    if (baseTypes != null) {
                        if (baseTypes.size() == 1) {
                            baseType = baseTypes.iterator().next();
                        } else {
                            throw ctx.exception(
                                    qnCtx.start.getLine(),
                                    "Illegal type name \"" + qualifiedName + "\", " +
                                            "it matches several types: " +
                                            baseTypes
                                                    .stream()
                                                    .map(BaseType::getQualifiedName)
                                                    .collect(Collectors.joining(", "))
                            );
                        }
                    }
                    if (baseType == null) {
                        throw ctx.exception(
                                qnCtx.start.getLine(),
                                "Illegal type name \"" + qualifiedName + "\", " +
                                        "it is not super type of \"" +
                                        this.baseType.getQualifiedName() +
                                        "\""
                        );
                    }
                }
                if (!handledBaseTypes.add(baseType)) {
                    throw ctx.exception(
                            qnCtx.start.getLine(),
                            "Illegal type name \"" + qualifiedName + "\", " +
                                    "it is not super type of \"" +
                                    baseType.getName() +
                                    "\""
                    );
                }
                for (P baseProp : ctx.getDeclaredProps(baseType).values()) {
                    if (isAutoScalar(baseProp) && !autoScalarPropMap.containsKey(baseProp.getName())) {
                        autoScalarPropMap.put(
                                baseProp.getName(),
                                new DtoPropBuilder<>(
                                        this,
                                        baseProp,
                                        qnCtx.stop.getLine(),
                                        allScalars.optional != null
                                )
                        );
                    }
                }
            }
        }
    }

    public AliasPattern currentAliasGroup() {
        return currentAliasGroup;
    }

    private void handlePositiveProp(DtoParser.PositivePropContext prop) {
        DtoPropBuilder<T, P> builder = new DtoPropBuilder<>(this, prop);
        if (positivePropMap.put(builder.getBaseProp(), builder) != null) {
            throw ctx.exception(
                    builder.getBaseLine(),
                    "Base property \"" +
                            builder.getBaseProp() +
                            "\" cannot be referenced twice"
            );
        }
        if (builder.getAlias() != null) {
            if (aliasPositivePropMap.put(builder.getAlias(), builder) != null) {
                throw ctx.exception(
                        builder.getAliasLine(),
                        "Duplicated property alias \"" +
                                builder.getAlias() +
                                "\""
                );
            }
        } else {
            flatPositiveProps.add(builder);
        }
    }

    private void handleNegativeProp(DtoParser.NegativePropContext prop) {
        if (negativePropAliasMap.put(prop.prop.getText(), false) != null) {
            throw ctx.exception(
                    prop.prop.getLine(),
                    "Duplicate negative property alias \"" +
                            prop.prop.getText() +
                            "\""
            );
        }
        negativePropAliasTokens.add(prop.prop);
    }

    private void handleAliasGroup(DtoParser.AliasGroupContext group) {
        currentAliasGroup = new AliasPattern(ctx, group.pattern);
        try {
            for (DtoParser.AliasGroupPropContext prop : group.props) {
                if (prop.allScalars() != null) {
                    handleAllScalars(prop.allScalars());
                } else {
                    handlePositiveProp(prop.positiveProp());
                }
            }
        } finally {
            currentAliasGroup = null;
        }
    }

    private void handleUserProp(DtoParser.UserPropContext prop) {
        List<Anno> annotations;
        if (prop.annotations.isEmpty()) {
            annotations = Collections.emptyList();
        } else {
            annotations = new ArrayList<>(prop.annotations.size());
            AnnoParser annoParser = new AnnoParser(ctx);
            for (DtoParser.AnnotationContext anno : prop.annotations) {
                annotations.add(annoParser.parse(anno));
            }
            annotations = Collections.unmodifiableList(annotations);
        }
        UserProp userProp = new UserProp(prop.prop, ctx.resolve(prop.typeRef()), annotations);
        if (aliasPositivePropMap.put(userProp.getAlias(), userProp) != null) {
            throw ctx.exception(
                    prop.prop.getLine(),
                    "Duplicated property alias \"" +
                            prop.prop.getText() +
                            "\""
            );
        }
    }

    private boolean isAutoScalar(P baseProp) {
        return !baseProp.isFormula() &&
                !baseProp.isTransient() &&
                !baseProp.isView() &&
                !baseProp.isList() &&
                ctx.getTargetType(baseProp) == null;
    }

    private static <T extends BaseType> void collectSuperTypes(
            T baseType,
            Map<String, T> qualifiedNameTypeMap,
            Map<String, Set<T>> nameTypeMap
    ) {
        qualifiedNameTypeMap.put(baseType.getQualifiedName(), baseType);
        nameTypeMap.computeIfAbsent(baseType.getName(), it -> new LinkedHashSet<>()).add(baseType);
    }

    DtoType<T, P> build() {

        if (dtoType != null) {
            return dtoType;
        }

        dtoType = new DtoType<>(
                baseType,
                modifiers.contains(DtoTypeModifier.INPUT),
                name != null ? name.getText() : null,
                ctx.getDtoFilePath()
        );

        resolveSuperTypes(new LinkedList<>());
        List<DtoType<T, P>> superTypes;
        if (superTypeBuilders.isEmpty()) {
            superTypes = Collections.emptyList();
        } else {
            superTypes = new ArrayList<>(superTypeBuilders.size());
            for (DtoTypeBuilder<T, P> superTypeBuilder : superTypeBuilders) {
                DtoType<T, P> superType = superTypeBuilder.build();
                if (modifiers.contains(DtoTypeModifier.INPUT) && !superType.isInput()) {
                    assert name != null;
                    throw ctx.exception(
                            name.getLine(),
                            "Illegal type \"" +
                                    name.getText() +
                                    "\", it is input type but the super type \"" +
                                    superType.getName() +
                                    "\" is not"
                    );
                }
                superTypes.add(superType);
            }
        }

        Map<String, AbstractProp> declaredProps = resolveDeclaredProps();

        Map<String, AbstractProp> superProps = new LinkedHashMap<>();
        if (!superTypes.isEmpty()) {
            Map<String, DtoProp<T, P>> basePathSuperProps = new LinkedHashMap<>();
            Set<String> declaredBasePaths = new HashSet<>();
            for (AbstractProp declaredProp : declaredProps.values()) {
                if (declaredProp instanceof DtoProp<?, ?>) {
                    declaredBasePaths.add(((DtoProp<?, ?>)declaredProp).getBasePath());
                }
            }
            for (DtoType<T, P> superType : superTypes) {
                for (DtoProp<T, P> superDtoProp : superType.getDtoProps()) {
                    String alias = superDtoProp.getAlias();
                    if (isExcluded(alias) ||
                            declaredProps.containsKey(superDtoProp.getAlias()) ||
                            declaredBasePaths.contains(superDtoProp.getBasePath())) {
                        continue;
                    }
                    DtoProp<T, P> baseConflictProp = basePathSuperProps.put(superDtoProp.getBasePath(), superDtoProp);
                    if (baseConflictProp != null && !DtoPropImpl.canMerge(baseConflictProp, superDtoProp)) {
                        assert name != null;
                        throw ctx.exception(
                                name.getLine(),
                                "Illegal dto type \"" +
                                        name.getText() +
                                        "\", the base property \"" +
                                        superDtoProp.getBasePath() +
                                        "\" is defined differently by multiple super type so that it must be overridden"
                        );
                    }
                    AbstractProp conflictAliasProp = superProps.put(alias, superDtoProp);
                    if (conflictAliasProp != null && !DtoPropImpl.canMerge(conflictAliasProp, superDtoProp)) {
                        assert name != null;
                        throw ctx.exception(
                                name.getLine(),
                                "Illegal dto type \"" +
                                        name.getText() +
                                        "\", the property alias \"" +
                                        alias +
                                        "\" is defined differently by multiple super type so that it must be overridden"
                        );
                    }
                }
            }
        }

        List<AbstractProp> props;
        if (superProps.isEmpty()) {
            props = new ArrayList<>(declaredProps.values());
        } else {
            props = new ArrayList<>();
            props.addAll(superProps.values());
            props.addAll(declaredProps.values());
        }

        validateUnusedNegativePropTokens();

        dtoType.setProps(Collections.unmodifiableList(props));
        return dtoType;
    }

    private void resolveSuperTypes(LinkedList<DtoTypeBuilder<T, P>> stack) {
        if (this.superTypeBuilders != null) {
            return;
        }
        if (superNames.isEmpty()) {
            this.superTypeBuilders = Collections.emptyList();
            return;
        }
        int index = stack.indexOf(this);
        if (index != -1) {
            throw ctx.exception(
                    name.getLine(),
                    "Illegal circular inheritance: " +
                            stack.subList(index, stack.size()).stream().map(it -> it.name.getText()).collect(Collectors.joining("->")) +
                            "->" +
                            name.getText()
            );
        }
        stack.push(this);
        try {
            List<DtoTypeBuilder<T, P>> superTypeBuilders = new ArrayList<>(superNames.size());
            for (Token superName : superNames) {
                DtoTypeBuilder<T, P> superTypeBuilder = ctx.get(superName.getText());
                if (superTypeBuilder == null) {
                    throw ctx.exception(
                            superName.getLine(),
                            "Illegal super dto name \"" +
                                    superName.getText() +
                                    "\""
                    );
                }
                superTypeBuilders.add(superTypeBuilder);
            }
            this.superTypeBuilders = superTypeBuilders;
        } finally {
            stack.pop();
        }
    }

    private Map<String, AbstractProp> resolveDeclaredProps() {
        if (this.declaredProps != null) {
            return this.declaredProps;
        }
        Map<String, AbstractProp> declaredPropMap = new LinkedHashMap<>();
        for (DtoPropBuilder<T, P> builder : autoScalarPropMap.values()) {
            if (isExcluded(builder.getAlias()) || positivePropMap.containsKey(builder.getBaseProp())) {
                continue;
            }
            DtoProp<T, P> dtoProp = builder.build();
            declaredPropMap.put(dtoProp.getAlias(), dtoProp);
        }
        for (AbstractPropBuilder builder : aliasPositivePropMap.values()) {
            if (isExcluded(builder.getAlias()) || declaredPropMap.containsKey(builder.getAlias())) {
                continue;
            }
            AbstractProp abstractProp = builder.build();
            if (declaredPropMap.put(abstractProp.getAlias(), abstractProp) != null) {
                throw ctx.exception(
                        abstractProp.getAliasLine(),
                        "Duplicated property alias \"" +
                                builder.getAlias() +
                                "\""
                );
            }
        }
        for (DtoPropBuilder<T, P> builder : flatPositiveProps) {
            DtoProp<T, P> head = builder.build();
            Map<String, AbstractProp> deeperProps = builder.getTargetBuilder().resolveDeclaredProps();
            for (AbstractProp deeperProp : deeperProps.values()) {
                DtoProp<T, P> dtoProp = new DtoPropImpl<>(head, (DtoProp<T, P>) deeperProp);
                if (isExcluded(dtoProp.getAlias())) {
                    continue;
                }
                if (declaredPropMap.put(dtoProp.getAlias(), dtoProp) != null) {
                    throw ctx.exception(
                            dtoProp.getAliasLine(),
                            "Duplicated property alias \"" +
                                    dtoProp.getAlias() +
                                    "\""
                    );
                }
            }
        }
        if (recursiveBaseProp != null) {
            DtoProp<T, P> recursiveDtoProp = new RecursiveDtoProp<>(recursiveBaseProp, recursiveAlias, dtoType);
            AbstractProp conflictProp = declaredPropMap.put(recursiveDtoProp.getAlias(), recursiveDtoProp);
            if (conflictProp != null) {
                throw ctx.exception(
                        conflictProp.getAliasLine(),
                        "Duplicated property alias \"" +
                                conflictProp.getAlias() +
                                "\""
                );
            }
        }
        return this.declaredProps = Collections.unmodifiableMap(declaredPropMap);
    }

    private boolean isExcluded(String alias) {
        if (!negativePropAliasMap.containsKey(alias)) {
            return false;
        }
        negativePropAliasMap.put(alias, true);
        return true;
    }

    private void validateUnusedNegativePropTokens() {
        for (Token token : negativePropAliasTokens) {
            if (!negativePropAliasMap.get(token.getText())) {
                throw ctx.exception(
                        token.getLine(),
                        "There is no property alias \"" +
                                token.getText() +
                                "\" that is need to be removed"
                );
            }
        }
    }
}
