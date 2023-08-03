package org.babyfish.jimmer.dto.compiler;

import org.antlr.v4.runtime.Token;
import org.babyfish.jimmer.dto.compiler.spi.BaseProp;
import org.babyfish.jimmer.dto.compiler.spi.BaseType;

import java.util.*;
import java.util.stream.Collectors;

class DtoTypeBuilder<T extends BaseType, P extends BaseProp> {

    private final T baseType;

    private final CompilerContext<T, P> ctx;

    private final Token name;

    private final Token bodyStart;

    private final Set<DtoTypeModifier> modifiers;

    private final List<Token> superNames;

    private final P recursiveBaseProp;

    private final String recursiveAlias;

    private final Map<String, DtoPropBuilder<T, P>> autoScalarPropMap = new LinkedHashMap<>();

    private final Map<String, DtoPropBuilder<T, P>> positivePropMap = new LinkedHashMap<>();

    private final Set<String> negativePropKeys = new LinkedHashSet<>();

    private List<DtoTypeBuilder<T, P>> superTypeBuilders;

    private DtoType<T, P> dtoType;

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
        if (body.macro() != null) {
            handleAllScalars(body.macro());
        }
        for (DtoParser.ExplicitPropContext prop : body.explicitProps) {
            if (prop.positiveProp() != null) {
                handlePositiveProp(prop.positiveProp());
            } else {
                handleNegativeProp(prop.negativeProp());
            }
        }
    }

    private void handleAllScalars(DtoParser.MacroContext macro) {
        if (!macro.name.getText().equals("allScalars")) {
            throw ctx.exception(
                    macro.name.getLine(),
                    "Illegal macro name \"" +
                            macro.name.getText() +
                            "\", it must be \"allScalars\""
            );
        }
        if (macro.args.isEmpty()) {
            for (P baseProp : ctx.getProps(baseType).values()) {
                if (isAutoScalar(baseProp)) {
                    autoScalarPropMap.put(
                            baseProp.getName(),
                            new DtoPropBuilder<>(baseProp, ctx.isImplicit(baseProp))
                    );
                }
            }
        } else {
            Map<String, T> qualifiedNameTypeMap = new HashMap<>();
            Map<String, Set<T>> nameTypeMap = new HashMap<>();
            collectSuperTypes(baseType, qualifiedNameTypeMap, nameTypeMap);
            Set<T> handledBaseTypes = new LinkedHashSet<>();
            for (DtoParser.QualifiedNameContext qnCtx : macro.args) {
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
                                        baseType.getName() +
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
                                        baseProp,
                                        ctx.isImplicit(baseProp)
                                )
                        );
                    }
                }
            }
        }
    }

    private void handlePositiveProp(DtoParser.PositivePropContext prop) {
        String funcName = null;
        if (prop.func != null) {
            funcName = prop.func.getText();
        }
        P baseProp = getExplicitProp(prop.prop, prop.func);
        if (prop.func != null) {
            switch (prop.func.getText()) {
                case "id":
                    if (!baseProp.isAssociation(true)) {
                        throw ctx.exception(
                                prop.func.getLine(),
                                "Cannot call the function \"id\" because the current prop \"" +
                                        baseProp +
                                        "\" is not entity level association property"
                        );
                    }
                    funcName = "id";
                    break;
                default:
                    throw ctx.exception(
                            prop.func.getLine(),
                            "The function name must be \"id\""
                    );
            }
        }

        String alias = null;
        if (prop.alias != null) {
            alias = prop.alias.getText();
        } else if ("id".equals(funcName)) {
            alias = baseProp.getName() + "Id";
        }

        if (prop.recursive != null && !baseProp.isRecursive()) {
            throw ctx.exception(
                    prop.recursive.getLine(),
                    "Illegal symbol \"" +
                            prop.recursive.getText() +
                            "\", the property \"" +
                            baseProp.getName() +
                            "\" is not recursive"
            );
        }

        DtoTypeBuilder<T, P> targetTypeBuilder = null;
        if (prop.dtoBody() != null) {
            if (!baseProp.isAssociation(true)) {
                throw ctx.exception(
                        prop.dtoBody().start.getLine(),
                        "Illegal property \"" +
                                baseProp.getName() +
                                "\", child body cannot be specified by it is not association"
                );
            }
            if ("id".equals(funcName)) {
                throw ctx.exception(
                        prop.dtoBody().start.getLine(),
                        "Illegal property \"" +
                                baseProp.getName() +
                                "\", child body cannot be specified by it is id view property"
                );
            }
            targetTypeBuilder = new DtoTypeBuilder<>(
                    ctx.getTargetType(baseProp),
                    prop.dtoBody(),
                    null,
                    modifiers.contains(DtoTypeModifier.INPUT) ?
                            Collections.singleton(DtoTypeModifier.INPUT) :
                            Collections.emptySet(),
                    Collections.emptyList(),
                    prop.recursive != null ? baseProp : null,
                    prop.recursive != null ? alias : null,
                    ctx
            );
        } else if (baseProp.isAssociation(true) && funcName == null) {
            throw ctx.exception(
                    prop.stop.getLine(),
                    "Illegal property \"" +
                            baseProp.getName() +
                            "\", the child body is required"
            );
        }

        positivePropMap.put(
                key(prop.prop, prop.func),
                new DtoPropBuilder<>(
                        baseProp,
                        ctx.isImplicit(baseProp) || prop.recursive != null,
                        funcName,
                        alias,
                        targetTypeBuilder,
                        prop.recursive != null
                )
        );
    }

    private void handleNegativeProp(DtoParser.NegativePropContext prop) {
        getExplicitProp(prop.prop, prop.func);
        negativePropKeys.add(key(prop.prop, prop.func));
    }

    private P getExplicitProp(Token token, Token func) {
        if (recursiveBaseProp != null && token.getText().equals(recursiveBaseProp.getName())) {
            throw ctx.exception(
                    token.getLine(),
                    "The property \"" +
                            token.getText() +
                            "\" cannot be specified because it is implicit recursive association"
            );
        }

        String name = token.getText();
        String key = key(token, func);
        if (positivePropMap.containsKey(key)) {
            throw ctx.exception(
                    token.getLine(),
                    "The property key \"" + key + "\" has already been included"
            );
        }
        if (negativePropKeys.contains(key)) {
            throw ctx.exception(
                    token.getLine(),
                    "The property key \"" + key + "\" has already been excluded"
            );
        }
        P baseProp = ctx.getProps(baseType).get(name);
        if (baseProp == null) {
            throw ctx.exception(
                    token.getLine(),
                    "There is no property \"" + name + "\" in \"" +
                            baseType.getQualifiedName() +
                            "\" or its super types"
            );
        }
        boolean isInput = modifiers.contains(DtoTypeModifier.INPUT);
        if (baseProp.isFormula() && isInput) {
            throw ctx.exception(
                    token.getLine(),
                    "The property \"" +
                            baseProp.getName() +
                            "\" cannot be declared in input dto because it is formula"
            );
        }
        if (baseProp.isView() && isInput) {
            throw ctx.exception(
                    token.getLine(),
                    "The property \"" +
                            baseProp.getName() +
                            "\" cannot be declared in input dto because it is view"
            );
        }
        if (baseProp.isTransient()) {
            if (isInput) {
                throw ctx.exception(
                        token.getLine(),
                        "The property \"" +
                                baseProp.getName() +
                                "\" cannot be declared in input dto because it is transient"
                );
            } else if (!baseProp.hasTransientResolver()) {
                throw ctx.exception(
                        token.getLine(),
                        "The property \"" +
                                baseProp.getName() +
                                "\" cannot be declared in dto because it is transient " +
                                "but has no transient resolver"
                );
            }
        }
        if (func != null) {
            switch (func.getText()) {
                case "id":
                    if (!baseProp.isAssociation(true)) {
                        throw ctx.exception(
                                func.getLine(),
                                "Cannot call the function \"id\" because the current prop \"" +
                                        baseProp +
                                        "\" is not entity level association property"
                        );
                    }
                    break;
                default:
                    throw ctx.exception(
                            func.getLine(),
                            "The function name must be \"id\""
                    );
            }
        }
        return baseProp;
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
                modifiers.contains(DtoTypeModifier.ABSTRACT),
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
                String key = key(superDtoProp);
                if (negativePropKeys.contains(key) || positivePropMap.containsKey(key)) {
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
            if (negativePropKeys.contains(key) || positivePropMap.containsKey(key)) {
                continue;
            }
            DtoProp<T, P> dtoProp = e.getValue().build();
            dtoPropMap.put(e.getKey(), dtoProp);
        }
        for (Map.Entry<String, DtoPropBuilder<T, P>> e : positivePropMap.entrySet()) {
            if (negativePropKeys.contains(e.getKey())) {
                continue;
            }
            DtoProp<T, P> dtoProp = e.getValue().build();
            dtoPropMap.put(e.getKey(), dtoProp);
        }
        if (recursiveBaseProp != null) {
            DtoProp<T, P> recursiveDtoProp = new RecursiveDtoProp<>(recursiveBaseProp, recursiveAlias, dtoType);
            dtoPropMap.put(key(recursiveDtoProp), recursiveDtoProp);
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
                                key(conflictDtoProp) +
                                "\" and \"" +
                                key(dtoProp) +
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

    private static String key(Token token, Token func) {
        return func != null ? func.getText() + '(' + token.getText() + ')' : token.getText();
    }

    private static String key(DtoProp<?, ?> prop) {
        return prop.isIdOnly() ? "id(" + prop.getBaseProp().getName() + ')' : prop.getName();
    }
}
