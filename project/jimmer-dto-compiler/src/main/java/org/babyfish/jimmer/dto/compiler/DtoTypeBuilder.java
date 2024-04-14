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

    final List<TypeRef> superInterfaces;

    final String doc;

    final Set<DtoModifier> modifiers;

    final Map<String, DtoPropBuilder<T, P>> autoPropMap;

    final Map<P, List<DtoPropBuilder<T, P>>> positivePropMap;

    final Map<String, AbstractPropBuilder> aliasPositivePropMap;

    final List<DtoPropBuilder<T, P>> flatPositiveProps;

    final Map<String, Boolean> negativePropAliasMap;

    final List<Token> negativePropAliasTokens;

    private DtoType<T, P> dtoType;

    private AliasPattern currentAliasGroup;

    private Map<String, AbstractProp> declaredProps;

    DtoTypeBuilder(
            DtoPropBuilder<T, P> parentProp,
            T baseType,
            DtoParser.DtoBodyContext body,
            Token name,
            String doc,
            Set<DtoModifier> modifiers,
            List<DtoParser.AnnotationContext> annotations,
            List<DtoParser.TypeRefContext> superInterfaces,
            CompilerContext<T, P> ctx
    ) {
        this.parentProp = parentProp;
        this.baseType = baseType;
        this.ctx = ctx;
        this.name = name;
        this.bodyStart = body.start;
        this.autoPropMap = new LinkedHashMap<>();
        this.positivePropMap = new LinkedHashMap<>();
        this.aliasPositivePropMap = new LinkedHashMap<>();
        this.flatPositiveProps = new ArrayList<>();
        this.negativePropAliasMap = new LinkedHashMap<>();
        this.negativePropAliasTokens = new ArrayList<>();
        this.doc = doc;
        this.modifiers = Collections.unmodifiableSet(modifiers);
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
        if (superInterfaces.isEmpty()) {
            this.superInterfaces = Collections.emptyList();
        } else {
            List<TypeRef> parsedSuperInterfaces = new ArrayList<>(superInterfaces.size());
            Set<String> typeNames = new LinkedHashSet<>((parsedSuperInterfaces.size() * 4 + 2) / 3);
            for (DtoParser.TypeRefContext superInterface : superInterfaces) {
                if (superInterface.optional != null) {
                    throw ctx.exception(
                            superInterface.optional.getLine(),
                            superInterface.optional.getCharPositionInLine(),
                            "The super interface type cannot be nullable"
                    );
                }
                TypeRef superTypeRef = ctx.resolve(superInterface);
                if (superTypeRef.getTypeName().startsWith("org.babyfish.jimmer.")) {
                    throw ctx.exception(
                            superInterface.stop.getLine(),
                            superInterface.stop.getCharPositionInLine(),
                            "Illegal super interface type \"" +
                                    superTypeRef.getTypeName() +
                                    "\", types under `org.babyfish.jimmer` are not allowed"
                    );
                }
                if (!typeNames.add(superTypeRef.getTypeName())) {
                    throw ctx.exception(
                            superInterface.stop.getLine(),
                            superInterface.stop.getCharPositionInLine(),
                            "Duplicate super interface \"" +
                                    superTypeRef.getTypeName() +
                                    "\""
                    );
                }
                parsedSuperInterfaces.add(superTypeRef);
            }
            this.superInterfaces = Collections.unmodifiableList(parsedSuperInterfaces);
        }

        for (DtoParser.ExplicitPropContext prop : body.explicitProps) {
            if (prop.micro() != null) {
                handleMicro(prop.micro());
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

    private DtoTypeBuilder(DtoTypeBuilder<T, P> base, Set<DtoPropBuilder<T, P>> removedPropBuilders) {
        this.parentProp = base.parentProp;
        this.baseType = base.baseType;
        this.ctx = base.ctx;
        this.bodyStart = base.bodyStart;
        this.modifiers = base.modifiers;
        this.annotations = base.annotations;
        this.superInterfaces = base.superInterfaces;
        this.doc = base.doc;
        this.autoPropMap = base.autoPropMap;
        this.flatPositiveProps = base.flatPositiveProps;
        this.negativePropAliasMap = base.negativePropAliasMap;
        this.negativePropAliasTokens = base.negativePropAliasTokens;

        this.name = null;
        Map<P, List<DtoPropBuilder<T, P>>> positivePropMap = new LinkedHashMap<>(base.positivePropMap);
        Iterator<List<DtoPropBuilder<T, P>>> itr = positivePropMap.values().iterator();
        while (itr.hasNext()) {
            List<DtoPropBuilder<T, P>> list = itr.next();
            list.removeAll(removedPropBuilders);
            if (list.isEmpty()) {
                itr.remove();
            }
        }
        this.positivePropMap = positivePropMap;

        Map<String, AbstractPropBuilder> aliasPositiveMap = new LinkedHashMap<>(base.aliasPositivePropMap);
        aliasPositiveMap.values().removeAll(removedPropBuilders);
        this.aliasPositivePropMap = aliasPositiveMap;
    }

    private void handleMicro(DtoParser.MicroContext micro) {
        boolean isAllReferences = micro.name.getText().equals("allReferences");
        if (!micro.name.getText().equals("allScalars") && !isAllReferences) {
            throw ctx.exception(
                    micro.name.getLine(),
                    micro.name.getCharPositionInLine(),
                    "Illegal micro name \"" +
                            micro.name.getText() +
                            "\", it must be \"#allScalars\" or \"#allReferences\""
            );
        }

        if (!positivePropMap.isEmpty() || !negativePropAliasMap.isEmpty()) {
            throw ctx.exception(
                    micro.name.getLine(),
                    micro.name.getCharPositionInLine(),
                    "`#" +
                            micro.name +
                            "` must be defined at the beginning"
            );
        }

        Mandatory mandatory;
        if (micro.required != null) {
            mandatory = Mandatory.REQUIRED;
        } else if (micro.optional != null) {
            if (modifiers.contains(DtoModifier.SPECIFICATION)) {
                throw ctx.exception(
                        micro.name.getLine(),
                        micro.name.getCharPositionInLine(),
                        "Unnecessary optional modifier '?', all properties of specification are automatically optional"
                );
            }
            mandatory = Mandatory.OPTIONAL;
        } else {
            mandatory = modifiers.contains(DtoModifier.SPECIFICATION) ? Mandatory.OPTIONAL : Mandatory.DEFAULT;
        }
        DtoModifier inputModifier = modifiers
                .stream()
                .filter(DtoModifier::isInputStrategy)
                .findFirst()
                .orElse(DtoModifier.STATIC);

        if (micro.args.isEmpty()) {
            for (P baseProp : ctx.getProps(baseType).values()) {
                if (isAllReferences ? isAutoReference(baseProp) : isAutoScalar(baseProp)) {
                    DtoPropBuilder<T, P> propBuilder =
                            new DtoPropBuilder<>(
                                    this,
                                    currentAliasGroup,
                                    baseProp,
                                    micro.start.getLine(),
                                    micro.start.getCharPositionInLine(),
                                    isAllReferences ? "id" : null,
                                    mandatory,
                                     inputModifier,
                                    null
                            );
                    autoPropMap.put(propBuilder.getAlias(), propBuilder);
                }
            }
        } else {
            Map<String, T> qualifiedNameTypeMap = new HashMap<>();
            Map<String, Set<T>> nameTypeMap = new HashMap<>();
            collectSuperTypes(baseType, qualifiedNameTypeMap, nameTypeMap);
            Set<T> handledBaseTypes = new LinkedHashSet<>();
            for (DtoParser.QualifiedNameContext qnCtx : micro.args) {
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
                                    qnCtx.start.getCharPositionInLine(),
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
                                    qnCtx.start.getCharPositionInLine(),
                                    "Illegal type name \"" + qualifiedName + "\", " +
                                            "it is not super type of \"" +
                                            this.baseType +
                                            "\""
                            );
                        }
                    }
                }
                if (!handledBaseTypes.add(baseType)) {
                    throw ctx.exception(
                            qnCtx.start.getLine(),
                            qnCtx.start.getCharPositionInLine(),
                            "Illegal type name \"" + qualifiedName + "\", " +
                                    "it is not super type of \"" +
                                    baseType.getName() +
                                    "\""
                    );
                }
                for (P baseProp : ctx.getDeclaredProps(baseType).values()) {
                    if ((isAllReferences ? isAutoReference(baseProp) : isAutoScalar(baseProp)) &&
                            !autoPropMap.containsKey(baseProp.getName())) {
                        DtoPropBuilder<T, P> propBuilder =
                                new DtoPropBuilder<>(
                                        this,
                                        currentAliasGroup,
                                        baseProp,
                                        qnCtx.stop.getLine(),
                                        qnCtx.stop.getCharPositionInLine(),
                                        isAllReferences ? "id" : null,
                                        mandatory,
                                        inputModifier,
                                        null
                                );
                        autoPropMap.put(propBuilder.getAlias(), propBuilder);
                    }
                }
            }
        }
    }

    public AliasPattern currentAliasGroup() {
        return currentAliasGroup;
    }

    private void handlePositiveProp(DtoParser.PositivePropContext prop) {
        DtoPropBuilder<T, P> builder = new DtoPropBuilder<>(this, currentAliasGroup, prop);
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
                        propBuilder.getBaseColumn(),
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
                        propBuilder.getAliasColumn(),
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
                    prop.prop.getCharPositionInLine(),
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
                if (prop.micro() != null) {
                    handleMicro(prop.micro());
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
                !modifiers.contains(DtoModifier.SPECIFICATION) &&
                !TypeRef.TNS_WITH_DEFAULT_VALUE.contains(typeRef.getTypeName())) {
            throw ctx.exception(
                    prop.prop.getLine(),
                    prop.prop.getCharPositionInLine(),
                    "Illegal user defined property \"" +
                            prop.prop.getText() +
                            "\", it is not null but its default value cannot be determined, " +
                            "so it must be declared in dto type with the modifier 'specification'"
            );
        }
        UserProp userProp = new UserProp(prop.prop, typeRef, annotations, prop.doc != null ? prop.doc.getText() : null);
        if (aliasPositivePropMap.put(userProp.getAlias(), userProp) != null) {
            throw ctx.exception(
                    prop.prop.getLine(),
                    prop.prop.getCharPositionInLine(),
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
                !baseProp.isAssociation(true);
    }

    private boolean isAutoReference(P baseProp) {
        return baseProp.isAssociation(true) &&
                !baseProp.isList() &&
                !baseProp.isTransient();
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
                ctx.getTargetPackageName(),
                modifiers,
                annotations,
                superInterfaces,
                name != null ? name.getText() : null,
                ctx.getDtoFile(),
                doc
        );

        Map<String, AbstractProp> propMap = resolveDeclaredProps();

        validateUnusedNegativePropTokens();

        List<AbstractProp> props = new ArrayList<>(propMap.size());
        for (AbstractProp prop : propMap.values()) {
            if (!(prop instanceof UserProp)) {
                props.add(prop);
            }
        }
        for (AbstractProp prop : propMap.values()) {
            if (prop instanceof UserProp) {
                props.add(prop);
            }
        }
        dtoType.setProps(Collections.unmodifiableList(props));
        return dtoType;
    }

    @SuppressWarnings("unchecked")
    private Map<String, AbstractProp> resolveDeclaredProps() {
        if (this.declaredProps != null) {
            return this.declaredProps;
        }
        Map<String, AbstractProp> declaredPropMap = new LinkedHashMap<>();
        for (DtoPropBuilder<T, P> builder : autoPropMap.values()) {
            if (isExcluded(builder.getAlias()) || positivePropMap.containsKey(builder.getBaseProp())) {
                continue;
            }
            addProps(builder, declaredPropMap);
        }
        for (AbstractPropBuilder builder : aliasPositivePropMap.values()) {
            if (isExcluded(builder.getAlias()) || declaredPropMap.containsKey(builder.getAlias())) {
                continue;
            }
            addProps(builder, declaredPropMap);
        }
        for (DtoPropBuilder<T, P> builder : flatPositiveProps) {
            DtoProp<T, P> head = builder.build(dtoType);
            List<AbstractProp> deeperProps = builder.getTargetBuilder().build().getProps();
            for (AbstractProp deeperProp : deeperProps) {
                if (deeperProp instanceof UserProp) {
                    UserProp userProp = (UserProp)deeperProp;
                    throw ctx.exception(
                            userProp.getAliasLine(),
                            userProp.getAliasColumn(),
                            "User defined property cannot be declared under flat type"
                    );
                }
                DtoProp<T, P> deeperDtoProp = (DtoProp<T, P>) deeperProp;
                String alias = deeperDtoProp.getAlias();
                DtoProp<T, P> dtoProp = new DtoPropImpl<>(head, deeperDtoProp, null);
                if (isExcluded(alias)) {
                    continue;
                }
                if (declaredPropMap.put(alias, dtoProp) != null) {
                    throw ctx.exception(
                            dtoProp.getAliasLine(),
                            dtoProp.getAliasColumn(),
                            "Duplicated property alias \"" +
                                    alias +
                                    "\""
                    );
                }
            }
        }
        return this.declaredProps = Collections.unmodifiableMap(declaredPropMap);
    }

    @SuppressWarnings("unchecked")
    private void addProps(AbstractPropBuilder propBuilder, Map<String, AbstractProp> outMap) {
        AbstractProp prop = propBuilder.build(dtoType);
        if (prop instanceof DtoProp<?, ?> && ((DtoProp<?, ?>)prop).isFlat()) {
            for (AbstractProp deeperProp : ((DtoProp<?, ?>)prop).getTargetType().getProps()) {
                DtoProp<T, P> flattedProp = new DtoPropImpl<>((DtoPropImpl<T, P>) prop, (DtoPropImpl<T, P>)deeperProp, propBuilder.getAliasPattern());
                if (outMap.put(flattedProp.getAlias(), flattedProp) != null) {
                    throw ctx.exception(
                            flattedProp.getAliasLine(),
                            flattedProp.getAliasColumn(),
                            "Duplicated property alias \"" +
                                    flattedProp.getAlias() +
                                    "\""
                    );
                }
            }
        } else if (outMap.put(prop.getAlias(), prop) != null) {
            throw ctx.exception(
                    prop.getAliasLine(),
                    prop.getAliasColumn(),
                    "Duplicated property alias \"" +
                            propBuilder.getAlias() +
                            "\""
            );
        }
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
                        token.getCharPositionInLine(),
                        "There is no property alias \"" +
                                token.getText() +
                                "\" that is need to be removed"
                );
            }
        }
    }
}
