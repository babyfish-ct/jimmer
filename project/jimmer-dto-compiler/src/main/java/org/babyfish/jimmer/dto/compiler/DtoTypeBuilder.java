package org.babyfish.jimmer.dto.compiler;

import org.antlr.v4.runtime.Token;
import org.babyfish.jimmer.dto.compiler.spi.BaseProp;
import org.babyfish.jimmer.dto.compiler.spi.BaseType;

import java.util.*;
import java.util.stream.Collectors;

class DtoTypeBuilder<T extends BaseType, P extends BaseProp> {

    final DtoPropBuilder<T, P> parentProp;

    final T baseType;

    final CompilerContext<T, P> ctx;

    final Token name;

    final Token bodyStart;

    final List<Anno> annotations;

    final Set<DtoTypeModifier> modifiers;

    final List<Token> superNames;

    final P recursiveBaseProp;

    final String recursiveAlias;

    final Map<String, DtoPropBuilder<T, P>> autoScalarPropMap = new LinkedHashMap<>();

    final Map<P, List<DtoPropBuilder<T, P>>> positivePropMap = new LinkedHashMap<>();

    final Map<String, AbstractPropBuilder> aliasPositivePropMap = new LinkedHashMap<>();

    final List<DtoPropBuilder<T, P>> flatPositiveProps = new ArrayList<>();

    final Map<String, Boolean> negativePropAliasMap = new LinkedHashMap<>();

    final List<Token> negativePropAliasTokens = new ArrayList<>();

    private List<DtoTypeBuilder<T, P>> superTypeBuilders;

    private DtoType<T, P> dtoType;

    private AliasPattern currentAliasGroup;

    private Map<String, AbstractProp> declaredProps;

    DtoTypeBuilder(
            DtoPropBuilder<T, P> parentProp,
            T baseType,
            DtoParser.DtoBodyContext body,
            Token name,
            List<DtoParser.AnnotationContext> annotations,
            Set<DtoTypeModifier> modifiers,
            List<Token> superNames,
            P recursiveBaseProp,
            String recursiveAlias,
            CompilerContext<T, P> ctx
    ) {
        this.parentProp = parentProp;
        this.baseType = baseType;
        this.ctx = ctx;
        this.name = name;
        this.bodyStart = body.start;
        if (annotations.isEmpty()) {
            this.annotations = Collections.emptyList();
        } else {
            List<Anno> parsedAnnotations = new ArrayList<>(annotations.size());
            AnnoParser parser = new AnnoParser(ctx);
            for (DtoParser.AnnotationContext annotation : annotations) {
                parsedAnnotations.add(parser.parse(annotation));
            }
            parsedAnnotations = Collections.unmodifiableList(parsedAnnotations);
            this.annotations = parsedAnnotations;
        }
        this.modifiers = Collections.unmodifiableSet(modifiers);
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

        if (!positivePropMap.isEmpty() || !negativePropAliasMap.isEmpty()) {
            throw ctx.exception(
                    allScalars.name.getLine(),
                    "`#allScalars` must be defined at the beginning"
            );
        }

        Mandatory mandatory;
        if (allScalars.required != null) {
            mandatory = Mandatory.REQUIRED;
        } else if (allScalars.optional != null) {
            if (modifiers.contains(DtoTypeModifier.SPECIFICATION)) {
                throw ctx.exception(
                        allScalars.name.getLine(),
                        "Unnecessary optional modifier '?', all properties of specification are automatically optional"
                );
            }
            mandatory = Mandatory.OPTIONAL;
        } else {
            mandatory = modifiers.contains(DtoTypeModifier.SPECIFICATION) ? Mandatory.OPTIONAL : Mandatory.DEFAULT;
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
                                    mandatory
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
                T baseType = qualifiedName.equals("this") ? this.baseType : qualifiedNameTypeMap.get(qualifiedName);
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
                        if (qualifiedName.indexOf('.') == -1) {
                            String imported;
                            try {
                                imported = ctx.resolve(qnCtx);
                            } catch (Throwable ex) {
                                imported = null;
                            }
                            if (imported != null) {
                                baseType = qualifiedNameTypeMap.get(imported);
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
                                        mandatory
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
        for (P baseProp : builder.getBasePropMap().values()) {
            handlePositiveProp0(builder, baseProp);
        }
    }

    private void handlePositiveProp0(DtoPropBuilder<T, P> propBuilder, P baseProp) {
        List<DtoPropBuilder<T, P>> builders = positivePropMap.get(baseProp);
        if (builders == null) {
            builders = new ArrayList<>();
            positivePropMap.put(baseProp, builders);
        } else {
            boolean valid = false;
            if (builders.size() < 2) {
                String oldFuncName = builders.get(0).getFuncName();
                String newFuncName = propBuilder.getFuncName();
                if (!Objects.equals(oldFuncName, newFuncName) &&
                        Constants.QBE_FUNC_NAMES.contains(oldFuncName) &&
                        (Constants.QBE_FUNC_NAMES.contains(newFuncName))) {
                    valid = true;
                }
            }
            if (!valid) {
                throw ctx.exception(
                        propBuilder.getBaseLine(),
                        "Base property \"" +
                                baseProp +
                                "\" cannot be referenced too many times"
                );
            }
        }
        builders.add(propBuilder);
        if (propBuilder.getAlias() != null) {
            AbstractPropBuilder conflictPropBuilder = aliasPositivePropMap.put(propBuilder.getAlias(), propBuilder);
            if (conflictPropBuilder != null && conflictPropBuilder != propBuilder) {
                throw ctx.exception(
                        propBuilder.getAliasLine(),
                        "Duplicated property alias \"" +
                                propBuilder.getAlias() +
                                "\""
                );
            }
        } else {
            flatPositiveProps.add(propBuilder);
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
        TypeRef typeRef = ctx.resolve(prop.typeRef());
        if (!typeRef.isNullable() &&
                !modifiers.contains(DtoTypeModifier.SPECIFICATION) &&
                !TypeRef.TNS_WITH_DEFAULT_VALUE.contains(typeRef.getTypeName())) {
            throw ctx.exception(
                    prop.prop.getLine(),
                    "Illegal user defined property \"" +
                            prop.prop.getText() +
                            "\", it is not null but its default value cannot be determined, " +
                            "so it must be declared in dto type with the modifier 'specification'"
            );
        }
        UserProp userProp = new UserProp(prop.prop, typeRef, annotations);
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
                baseProp.getIdViewBaseProp() == null &&
                baseProp.getManyToManyViewBaseProp() == null &&
                !baseProp.isList() &&
                ctx.getTargetType(baseProp) == null;
    }

    private void collectSuperTypes(
            T baseType,
            Map<String, T> qualifiedNameTypeMap,
            Map<String, Set<T>> nameTypeMap
    ) {
        qualifiedNameTypeMap.put(baseType.getQualifiedName(), baseType);
        nameTypeMap.computeIfAbsent(baseType.getName(), it -> new LinkedHashSet<>()).add(baseType);
        for (T superType : ctx.getSuperTypes(baseType)) {
            collectSuperTypes(superType, qualifiedNameTypeMap, nameTypeMap);
        }
    }

    DtoType<T, P> build() {

        if (dtoType != null) {
            return dtoType;
        }

        dtoType = new DtoType<>(
                baseType,
                annotations,
                modifiers,
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
                String category = category(modifiers);
                String superCategory = category(superType.getModifiers());
                if (!category.equals(superCategory)) {
                    assert name != null;
                    throw ctx.exception(
                            name.getLine(),
                            "Illegal type \"" +
                                    name.getText() +
                                    "\", it is \"" +
                                    category +
                                    "\", but its super type \"" +
                                    superType.getName() +
                                    "\" is not"
                    );
                }
                if (!modifiers.contains(DtoTypeModifier.UNSAFE) &&
                        superType.getModifiers().contains(DtoTypeModifier.UNSAFE)) {
                    assert name != null;
                    throw ctx.exception(
                            name.getLine(),
                            "Illegal type \"" +
                                    name.getText() +
                                    "\", its super type \"" +
                                    superType.getName() +
                                    "\" is unsafe so that it must unsafe too"
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

        List<AbstractProp> props = new ArrayList<>();
        for (AbstractProp prop : superProps.values()) {
            if (prop instanceof DtoProp<?, ?>) {
                props.add(prop);
            }
        }
        for (AbstractProp prop : declaredProps.values()) {
            if (prop instanceof DtoProp<?, ?>) {
                props.add(prop);
            }
        }
        for (AbstractProp prop : superProps.values()) {
            if (prop instanceof UserProp) {
                props.add(prop);
            }
        }
        for (AbstractProp prop : declaredProps.values()) {
            if (prop instanceof UserProp) {
                props.add(prop);
            }
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

    private static String category(Set<DtoTypeModifier> modifiers) {
        if (modifiers.contains(DtoTypeModifier.INPUT)) {
            return "input";
        }
        if (modifiers.contains(DtoTypeModifier.SPECIFICATION)) {
            return "specification";
        }
        return "view(neither input nor specification)";
    }
}
