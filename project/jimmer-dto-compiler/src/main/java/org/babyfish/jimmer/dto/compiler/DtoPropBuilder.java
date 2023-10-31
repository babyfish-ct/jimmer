package org.babyfish.jimmer.dto.compiler;

import org.antlr.v4.runtime.Token;
import org.babyfish.jimmer.dto.compiler.spi.BaseProp;
import org.babyfish.jimmer.dto.compiler.spi.BaseType;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

class DtoPropBuilder<T extends BaseType, P extends BaseProp> implements DtoPropImplementor, AbstractPropBuilder {

    private final DtoTypeBuilder<T, P> parent;

    private final Map<String, P> basePropMap;

    private final int baseLine;

    private final String alias;

    private final int aliasLine;

    private final List<Anno> annotations;

    private final Mandatory mandatory;

    private final String funcName;

    private final DtoTypeBuilder<T, P> targetTypeBuilder;

    private final EnumType enumType;

    private final boolean recursive;

    private final Set<LikeOption> likeOptions;

    DtoPropBuilder(
            DtoTypeBuilder<T, P> parent,
            P baseProp,
            int line,
            Mandatory mandatory
    ) {
        this.parent = Objects.requireNonNull(parent, "parent cannot be null");
        this.basePropMap = Collections.singletonMap(
                Objects.requireNonNull(baseProp, "basePropMap cannot be null").getName(),
                baseProp
        );
        this.aliasLine = line;
        this.alias = parent.currentAliasGroup() != null ?
                parent.currentAliasGroup().alias(baseProp.getName(), 0) :
                baseProp.getName();
        this.baseLine = line;
        this.annotations = Collections.emptyList();
        if (mandatory == Mandatory.DEFAULT && parent.ctx.isImplicitId(baseProp, parent.modifiers)) {
            this.mandatory = Mandatory.OPTIONAL;
        } else {
            this.mandatory = mandatory;
        }
        this.funcName = null;
        this.targetTypeBuilder = null;
        this.enumType = null;
        this.recursive = false;
        this.likeOptions = Collections.emptySet();
    }

    DtoPropBuilder(
        DtoTypeBuilder<T, P> parent,
        DtoParser.PositivePropContext prop
    ) {
        CompilerContext<T, P> ctx = parent.ctx;
        String funcName = null;
        boolean isQbeFunc = false;
        Map<String, P> basePropMap = new LinkedHashMap<>();
        if (prop.func != null) {
            funcName = prop.func.getText();
            isQbeFunc = Constants.QBE_FUNC_NAMES.contains(funcName);
            if (isQbeFunc && !parent.modifiers.contains(DtoTypeModifier.SPECIFICATION)) {
                throw ctx.exception(
                        prop.func.getLine(),
                        "Illegal function \"" +
                                funcName +
                                "\", it can only be declared in specification"
                );
            }
            if (prop.props.size() > 1 && !Constants.MULTI_ARGS_FUNC_NAMES.contains(funcName)) {
                throw ctx.exception(
                        prop.func.getLine(),
                        "Illegal function \"" +
                                funcName +
                                "\", it can not have multiple arguments, the functions support multiple arguments are " +
                                Constants.MULTI_ARGS_FUNC_NAMES
                );
            }
        }
        this.parent = Objects.requireNonNull(parent, "parent cannot be null");
        this.baseLine = prop.props.get(0).getLine();
        this.aliasLine = prop.alias != null ? prop.alias.getLine() : prop.props.get(0).getLine();

        Iterator<Token> itr = prop.props.iterator();
        P firstBaseProp = getBaseProp(parent, itr.next());
        basePropMap.put(firstBaseProp.getName(), firstBaseProp);
        while (itr.hasNext()) {
            Token token = itr.next();
            P baseProp = getBaseProp(parent, token);
            P conflictBaseProp = basePropMap.put(baseProp.getName(), baseProp);
            if (conflictBaseProp != null) {
                throw ctx.exception(
                        prop.func.getLine(),
                        "Illegal property \"" +
                                baseProp.getName() +
                                "\", it is duplicated"
                );
            }
            if (!ctx.isSameType(firstBaseProp, baseProp)) {
                throw ctx.exception(
                        prop.func.getLine(),
                        "Illegal property \"" +
                                baseProp.getName() +
                                "\", its type is not same as the type of \"" +
                                firstBaseProp.getName() +
                                "\""
                );
            }
        }
        this.basePropMap = Collections.unmodifiableMap(basePropMap);

        EnumSet<LikeOption> likeOptions = EnumSet.noneOf(LikeOption.class);
        if (prop.insensitive != null) {
            if (!prop.insensitive.getText().equals("i")) {
                throw ctx.exception(
                        prop.insensitive.getLine(),
                        "Illegal function option identifier `" +
                                prop.insensitive +
                                "`, it can only be `i`"
                );
            }
            if (!"like".equals(funcName)) {
                throw ctx.exception(
                        prop.insensitive.getLine(),
                        "`i` can only be used to decorate the function `like`"
                );
            }
            likeOptions.add(LikeOption.INSENSITIVE);
        }
        if (prop.prefix != null) {
            if (!"like".equals(funcName)) {
                throw ctx.exception(
                        prop.prefix.getLine(),
                        "`^` can only be used to decorate the function `like`"
                );
            }
            likeOptions.add(LikeOption.MATCH_START);
        }
        if (prop.suffix != null) {
            if (!"like".equals(funcName)) {
                throw ctx.exception(
                        prop.suffix.getLine(),
                        "`$` can only be used to decorate the function `like`"
                );
            }
            likeOptions.add(LikeOption.MATCH_END);
        }
        this.likeOptions = Collections.unmodifiableSet(likeOptions);

        List<Anno> annotations;
        if (prop.annotations.isEmpty()) {
            annotations = Collections.emptyList();
        } else {
            AnnoParser parser = new AnnoParser(parent.ctx);
            annotations = new ArrayList<>(prop.annotations.size());
            for (DtoParser.AnnotationContext anno : prop.annotations) {
                annotations.add(parser.parse(anno));
            }
            annotations = Collections.unmodifiableList(annotations);
        }
        this.annotations = annotations;

        P baseProp = basePropMap.values().iterator().next();

        if (funcName != null) {
            switch (funcName) {
                case "id":
                    if (!baseProp.isAssociation(true)) {
                        throw ctx.exception(
                                prop.func.getLine(),
                                "Cannot call the function \"id\" because the current prop \"" +
                                        baseProp +
                                        "\" is not entity level association property"
                        );
                    }
                    if (prop.alias == null && baseProp.isList()) {
                        throw ctx.exception(
                                prop.func.getLine(),
                                "The alias must be specified for the mapping property with function \"id\" because the current prop \"" +
                                        baseProp +
                                        "\" is list association"
                        );
                    }
                    funcName = "id";
                    break;
                case "flat":
                    if (!baseProp.isAssociation(false)) {
                        throw ctx.exception(
                                prop.func.getLine(),
                                "Cannot call the function \"flat\" because the current prop \"" +
                                        baseProp +
                                        "\" is not association"
                        );
                    }
                    if (baseProp.isList() && !parent.modifiers.contains(DtoTypeModifier.SPECIFICATION)) {
                        throw ctx.exception(
                                prop.func.getLine(),
                                "Cannot call the function \"flat\" because the current prop \"" +
                                        baseProp +
                                        "\" is list"
                        );
                    }
                    break;
                case "like":
                    if (!ctx.isStringProp(baseProp)) {
                        throw ctx.exception(
                                prop.func.getLine(),
                                "Cannot call the function \"like\" because the current prop \"" +
                                        baseProp +
                                        "\" is not string"
                        );
                    }
                    break;
                case "eq":
                case "ne":
                case "lt":
                case "le":
                case "gt":
                case "ge":
                case "valueIn":
                case "valueNotIn":
                    if (baseProp.isAssociation(true)) {
                        throw ctx.exception(
                                prop.func.getLine(),
                                "Cannot call the function \"" +
                                        funcName +
                                        "\" the current prop \"" +
                                        baseProp +
                                        "\" cannot be association"
                        );
                    }
                    break;
                case "associatedIdIn":
                case "associatedIdNotIn":
                    if (!baseProp.isAssociation(true)) {
                        throw ctx.exception(
                                prop.func.getLine(),
                                "Cannot call the function \"" + funcName + "\" because the current prop \"" +
                                        baseProp +
                                        "\" is not association"
                        );
                    }
                    break;
                case "null":
                case "nonNull":
                    break;
                default:
                    throw ctx.exception(
                            prop.func.getLine(),
                            "Illegal function name \"" +
                                    funcName +
                                    "\", " +
                                    (parent.modifiers.contains(DtoTypeModifier.SPECIFICATION) ?
                                    "the function name of specification type must be \"id\", \"flat\", " +
                                            Constants.QBE_FUNC_NAMES.stream().collect(Collectors.joining(", ")) :
                                    "the function name must be \"id\" or \"flat\"")
                    );
            }
        }
        this.funcName = funcName;

        String alias;
        if (prop.alias != null) {
            if (parent.currentAliasGroup() != null) {
                throw ctx.exception(
                        prop.alias.getLine(),
                        "The alias cannot be specified in alias group"
                );
            }
            if ("flat".equals(funcName)) {
                throw ctx.exception(
                        prop.alias.getLine(),
                        "The alias cannot be specified when the function `" + funcName + "` is used"
                );
            }
            alias = prop.alias.getText();
        } else {
            if (basePropMap.size() > 1) {
                throw ctx.exception(
                        prop.props.get(prop.props.size() - 1).getLine(),
                        "The alias must be specified when the function has multiple arguments"
                );
            }
            if (funcName == null) {
                alias = baseProp.getName();
            } else {
                switch (funcName) {
                    case "id":
                        if (baseProp.isAssociation(true) && baseProp.isList()) {
                            throw ctx.exception(
                                    prop.props.get(0).getLine(),
                                    "The alias must be specified for the property with " +
                                            "`id` function when the base property is list association"
                            );
                        }
                        alias = baseProp.getName() + "Id";
                        break;
                    case "flat":
                        alias = null;
                        break;
                    case "ne":
                    case "valueIn":
                    case "valueNotIn":
                        throw ctx.exception(
                                prop.props.get(0).getLine(),
                                "The alias must be specified for `" +
                                        funcName +
                                        "` function"
                        );
                    case "gt":
                        alias = baseProp.getName();
                        alias = "min" + Character.toUpperCase(alias.charAt(0)) + alias.substring(1) + "Exclusive";
                        break;
                    case "ge":
                        alias = baseProp.getName();
                        alias = "min" + Character.toUpperCase(alias.charAt(0)) + alias.substring(1);
                        break;
                    case "lt":
                        alias = baseProp.getName();
                        alias = "max" + Character.toUpperCase(alias.charAt(0)) + alias.substring(1) + "Exclusive";
                        break;
                    case "le":
                        alias = baseProp.getName();
                        alias = "max" + Character.toUpperCase(alias.charAt(0)) + alias.substring(1);
                        break;
                    case "null":
                        alias = baseProp.getName();
                        if (!alias.startsWith("is") || alias.length() < 3 || !Character.isUpperCase(alias.charAt(2))) {
                            alias = Character.toUpperCase(alias.charAt(0)) + alias.substring(1);
                        }
                        alias = "is" + alias + "Null";
                        break;
                    case "notNull":
                        alias = baseProp.getName();
                        if (!alias.startsWith("is") || alias.length() < 3 || !Character.isUpperCase(alias.charAt(2))) {
                            alias = Character.toUpperCase(alias.charAt(0)) + alias.substring(1);
                        }
                        alias = "is" + alias + "NotNull";
                        break;
                    case "associatedIdIn":
                        if (baseProp.isAssociation(true) && baseProp.isList()) {
                            throw ctx.exception(
                                    prop.props.get(0).getLine(),
                                    "The alias must be specified for `associatedIdIn` function when base property is list"
                            );
                        }
                        alias = baseProp.getName() + "Ids";
                        break;
                    case "associatedIdNotIn":
                        if (baseProp.isAssociation(true) && baseProp.isList()) {
                            throw ctx.exception(
                                    prop.props.get(0).getLine(),
                                    "The alias must be specified for `associatedIdNotIn` function when base property is list"
                            );
                        }
                        alias = "excluded" + Character.toUpperCase(baseProp.getName().charAt(0)) + baseProp.getName().substring(1) + "Ids";
                        break;
                    default:
                        alias = baseProp.getName();
                        break;
                }
            }
        }

        if (parent.currentAliasGroup() != null) {
            int line = prop.alias != null ? prop.alias.getLine() : prop.props.get(prop.props.size() - 1).getLine();
            alias = parent.currentAliasGroup().alias(alias != null ? alias : prop.props.get(prop.props.size() - 1).getText(), line);
        }

        if (prop.optional != null) {
            if ("flat".equals(funcName)) {
                throw ctx.exception(
                        prop.optional.getLine(),
                        "Illegal optional modifier '?', it is not allowed for the function `flat`"
                );
            }
            if (baseProp.isNullable()) {
                throw ctx.exception(
                        prop.optional.getLine(),
                        "Illegal optional modifier '?' because the base property is already nullable"
                );
            }
            DtoPropBuilder<T, P> nullableFlatParent = getNullableFlatParent();
            while (nullableFlatParent != null) {
                throw ctx.exception(
                        prop.optional.getLine(),
                        "Illegal optional modifier '?' because the flat parent property \"" +
                                nullableFlatParent.basePropMap.values().iterator().next() +
                                "\" is already nullable"
                );
            }
        }
        if (prop.required != null) {
            if ("flat".equals(funcName)) {
                throw ctx.exception(
                        prop.required.getLine(),
                        "Illegal required modifier '!', it is not allowed for the function `flat`"
                );
            }
            if (baseProp.isId()) {
                if (!parent.modifiers.contains(DtoTypeModifier.INPUT) &&
                        !parent.modifiers.contains(DtoTypeModifier.SPECIFICATION)) {
                    throw ctx.exception(
                            prop.required.getLine(),
                            "Illegal required modifier '!' for id property, " +
                                    "the declared type is neither input nor specification"
                    );
                }
            } else {
                if (!parent.modifiers.contains(DtoTypeModifier.SPECIFICATION) &&
                !parent.modifiers.contains(DtoTypeModifier.UNSAFE)) {
                    throw ctx.exception(
                            prop.required.getLine(),
                            "Illegal required modifier '!' for non-id property, " +
                                    "the declared type is neither unsafe input nor specification"
                    );
                }
                if (!baseProp.isNullable() && getNullableFlatParent() == null) {
                    throw ctx.exception(
                            prop.required.getLine(),
                            "Illegal required modifier '!' because the base property is already nonnull"
                    );
                }
            }
        }

        if (prop.recursive != null) {
            if (!baseProp.isRecursive()) {
                throw ctx.exception(
                        prop.recursive.getLine(),
                        "Illegal symbol \"" +
                                prop.recursive.getText() +
                                "\", the property \"" +
                                baseProp.getName() +
                                "\" is not recursive"
                );
            }
            if ("flat".equals(funcName)) {
                throw ctx.exception(
                        prop.recursive.getLine(),
                        "Illegal symbol \"" +
                                prop.recursive.getText() +
                                "\", the flat property \"" +
                                baseProp.getName() +
                                "\" cannot not recursive"
                );
            }
            if (prop.required != null) {
                throw ctx.exception(
                        prop.recursive.getLine(),
                        "Illegal symbol \"" +
                                prop.recursive.getText() +
                                "\", the required property \"" +
                                baseProp.getName() +
                                "\" cannot not recursive"
                );
            }
        }

        DtoTypeBuilder<T, P> targetTypeBuilder = null;
        DtoParser.DtoBodyContext dtoBody = prop.dtoBody();
        if (dtoBody != null) {
            if (!baseProp.isAssociation(false)) {
                throw ctx.exception(
                        dtoBody.start.getLine(),
                        "Illegal property \"" +
                                baseProp.getName() +
                                "\", child body cannot be specified by it is not association"
                );
            }
            if ("id".equals(funcName)) {
                throw ctx.exception(
                        dtoBody.start.getLine(),
                        "Illegal property \"" +
                                baseProp.getName() +
                                "\", child body cannot be specified by it is id view property"
                );
            }
            targetTypeBuilder = new DtoTypeBuilder<>(
                    this,
                    ctx.getTargetType(baseProp),
                    dtoBody,
                    null,
                    prop.annotations,
                    parent.modifiers.contains(DtoTypeModifier.INPUT) ?
                            Collections.singleton(DtoTypeModifier.INPUT) :
                            parent.modifiers.contains(DtoTypeModifier.SPECIFICATION) ?
                            Collections.singleton(DtoTypeModifier.SPECIFICATION) :
                            Collections.emptySet(),
                    Collections.emptyList(),
                    prop.recursive != null ? baseProp : null,
                    prop.recursive != null ? alias : null,
                    ctx
            );
        } else if (baseProp.isAssociation(false) &&
                !"id".equals(funcName) &&
                !"associatedIdIn".equals(funcName) &&
                !"associatedIdNotIn".equals(funcName)) {
            throw ctx.exception(
                    prop.stop.getLine(),
                    "Illegal property \"" +
                            baseProp.getName() +
                            "\", the child body is required"
            );
        }

        DtoParser.EnumBodyContext enumBody = prop.enumBody();
        if (enumBody != null) {
            List<String> constants = ctx.getEnumConstants(baseProp);
            if (constants == null || constants.isEmpty()) {
                throw ctx.exception(
                        enumBody.start.getLine(),
                        "Illegal property \"" +
                                baseProp.getName() +
                                "\", enum body cannot be specified by it is not enum property"
                );
            }
            this.enumType = EnumType.of(ctx, constants, enumBody);
        } else {
            this.enumType = null;
        }

        this.alias = alias;
        if (prop.required != null) {
            this.mandatory = Mandatory.REQUIRED;
        } else if (prop.optional != null || prop.recursive != null || ctx.isImplicitId(baseProp, parent.modifiers)) {
            this.mandatory = Mandatory.OPTIONAL;
        } else {
            this.mandatory = Mandatory.DEFAULT;
        }
        this.targetTypeBuilder = targetTypeBuilder;
        this.recursive = prop.recursive != null;
    }

    public DtoTypeBuilder<T, P> getParent() {
        return parent;
    }

    @Override
    public P getBaseProp() {
        return basePropMap.values().iterator().next();
    }

    @Override
    public Map<String, P> getBasePropMap() {
        return basePropMap;
    }

    @Override
    public int getBaseLine() {
        return baseLine;
    }

    @Nullable
    @Override
    public String getAlias() {
        return alias;
    }

    @Override
    public int getAliasLine() {
        return aliasLine;
    }

    @Override
    public @Nullable String getFuncName() {
        return funcName;
    }

    @Override
    public Mandatory getMandatory() {
        return mandatory;
    }

    @Override
    public List<Anno> getAnnotations() {
        return annotations;
    }

    public DtoTypeBuilder<T, P> getTargetBuilder() {
        return targetTypeBuilder;
    }

    private static <T extends BaseType, P extends BaseProp > P getBaseProp(DtoTypeBuilder<T, P> parent, Token token) {

        T baseType = parent.baseType;
        P recursiveBaseProp = parent.recursiveBaseProp;
        CompilerContext<T, P> ctx = parent.ctx;

        if (recursiveBaseProp != null && token.getText().equals(recursiveBaseProp.getName())) {
            throw ctx.exception(
                    token.getLine(),
                    "The property \"" +
                            token.getText() +
                            "\" cannot be specified because it is implicit recursive association"
            );
        }

        String baseName = token.getText();
        final P baseProp = ctx.getProps(baseType).get(baseName);
        if (baseProp == null) {
            throw ctx.exception(
                    token.getLine(),
                    "There is no property \"" + baseName + "\" in \"" +
                            baseType.getQualifiedName() +
                            "\" or its super types"
            );
        }
        boolean isInput = parent.modifiers.contains(DtoTypeModifier.INPUT);
        if (baseProp.isFormula() && isInput) {
            throw ctx.exception(
                    token.getLine(),
                    "The property \"" +
                            baseProp.getName() +
                            "\" cannot be declared in input dto because it is formula"
            );
        }
        if (baseProp.getManyToManyViewBaseProp() != null && isInput) {
            throw ctx.exception(
                    token.getLine(),
                    "The property \"" +
                            baseProp.getName() +
                            "\" cannot be declared in input dto because it is many-to-many-view"
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
        return baseProp;
    }

    private DtoPropBuilder<T, P> getNullableFlatParent() {
        DtoPropBuilder<T, P> parentProp = parent.parentProp;
        while (parentProp != null) {
            if (parentProp.getBaseProp().isNullable() && "flat".equals(parentProp.funcName)) {
                return parentProp;
            }
            parentProp = parentProp.parent.parentProp;
        }
        return null;
    }

    @Override
    public DtoProp<T, P> build() {
        return new DtoPropImpl<>(
                basePropMap,
                baseLine,
                alias,
                aliasLine,
                annotations,
                targetTypeBuilder != null ? targetTypeBuilder.build() : null,
                enumType,
                mandatory,
                funcName,
                recursive,
                likeOptions
        );
    }
}
