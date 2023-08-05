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

    final Map<String, DtoPropBuilder<T, P>> keyPropMap = new LinkedHashMap<>();

    final Map<String, DtoPropBuilder<T, P>> aliasPropMap = new LinkedHashMap<>();

    final Set<String> negativePropAliases = new LinkedHashSet<>();

    private List<DtoTypeBuilder<T, P>> superTypeBuilders;

    private DtoType<T, P> dtoType;

    private AliasPattern currentAliasGroup;

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
            } else {
                handleNegativeProp(prop.negativeProp());
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

        if (!keyPropMap.isEmpty()) {
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
                                    allScalars.start.getLine()
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
                                        qnCtx.stop.getLine()
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
        if (keyPropMap.put(builder.getKey(), builder) != null) {
            throw new DtoAstException(
                    builder.getBaseLine(),
                    "Duplicate base property reference \"" +
                            builder.getKey() +
                            "\""
            );
        }
        if (builder.getAlias() != null && aliasPropMap.put(builder.getAlias(), builder) != null) {
            throw new DtoAstException(
                    builder.getAliasLine(),
                    "Duplicate property alias \"" +
                            builder.getAlias() +
                            "\""
            );
        }
    }

    private void handleNegativeProp(DtoParser.NegativePropContext prop) {
        negativePropAliases.add(prop.prop.getText());
    }

    private void handleAliasGroup(DtoParser.AliasGroupContext group) {
        currentAliasGroup = new AliasPattern(group.pattern);
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
                name != null ? name.getText() : null
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

        Map<String, DtoProp<T, P>> superDtoPropMap = new LinkedHashMap<>();
        for (DtoType<T, P> superType : superTypes) {
            for (DtoProp<T, P> superDtoProp : superType.getProps()) {
                if (recursiveBaseProp != null && superDtoProp.getBaseProp().getName().equals(recursiveBaseProp.getName())) {
                    continue;
                }
                String key = superDtoProp.getKey();
                if (negativePropAliases.contains(key) || keyPropMap.containsKey(key)) {
                    continue;
                }
                DtoProp<T, P> conflictProp = superDtoPropMap.get(key);
                if (conflictProp != null) {
                    if (DtoPropImpl.canMerge(conflictProp, superDtoProp)) {
                        continue;
                    }
                    assert name != null;
                    throw ctx.exception(
                            name.getLine(),
                            "Illegal dto type \"" +
                                    name.getText() +
                                    "\", the property \"" +
                                    key +
                                    "\" is defined differently by multiple super type so that it must be overridden"
                    );
                }
                superDtoPropMap.put(key, superDtoProp);
            }
        }

        Map<String, DtoProp<T, P>> dtoPropMap = new LinkedHashMap<>(superDtoPropMap);
        for (Map.Entry<String, DtoPropBuilder<T, P>> e : autoScalarPropMap.entrySet()) {
            String key = e.getKey();
            if (negativePropAliases.contains(key) || keyPropMap.containsKey(key)) {
                continue;
            }
            DtoProp<T, P> dtoProp = e.getValue().build();
            dtoPropMap.put(e.getKey(), dtoProp);
        }
        for (Map.Entry<String, DtoPropBuilder<T, P>> e : keyPropMap.entrySet()) {
            if (negativePropAliases.contains(e.getKey())) {
                continue;
            }
            DtoProp<T, P> dtoProp = e.getValue().build();
            dtoPropMap.put(e.getKey(), dtoProp);
        }
        if (recursiveBaseProp != null) {
            DtoProp<T, P> recursiveDtoProp = new RecursiveDtoProp<>(recursiveBaseProp, recursiveAlias, dtoType);
            dtoPropMap.put(recursiveDtoProp.getKey(), recursiveDtoProp);
        }

        Map<String, DtoProp<T, P>> finalNameMap = new HashMap<>();
        for (DtoProp<T, P> dtoProp : dtoPropMap.values()) {
            // `DtoProp.name` is alias of base property
            DtoProp<T, P> conflictDtoProp = finalNameMap.put(dtoProp.getName(), dtoProp);
            if (conflictDtoProp != null) {
                throw ctx.exception(
                        bodyStart.getLine(),
                        "Illegal dto type " +
                                (name != null ? "\"" + name.getText() + "\"" : "") +
                                ", The property \"" +
                                conflictDtoProp.getKey() +
                                "\" and \"" +
                                dtoProp.getKey() +
                                "\" share the same alias \"" +
                                dtoProp.getName() +
                                "\""
                );
            }
        }

        dtoType.setProps(Collections.unmodifiableList(new ArrayList<>(dtoPropMap.values())));
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
}
