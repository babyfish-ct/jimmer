package org.babyfish.jimmer.dto.compiler;

import org.antlr.v4.runtime.Token;
import org.babyfish.jimmer.dto.compiler.spi.BaseProp;
import org.babyfish.jimmer.dto.compiler.spi.BaseType;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;

class DtoPropBuilder<T extends BaseType, P extends BaseProp> implements DtoPropImplementor {

    private final DtoTypeBuilder<T, P> parent;

    private final P baseProp;

    private final int baseLine;

    private final String alias;

    private final int aliasLine;

    private final boolean isOptional;

    private final String funcName;

    private final DtoTypeBuilder<T, P> targetTypeBuilder;

    private final boolean recursive;

    DtoPropBuilder(
            DtoTypeBuilder<T, P> parent,
            P baseProp,
            int line
    ) {
        this.parent = parent;
        this.baseProp = baseProp;
        this.aliasLine = line;
        this.alias = parent.currentAliasGroup() != null ?
                parent.currentAliasGroup().alias(baseProp.getName(), 0) :
                baseProp.getName();
        this.baseLine = line;
        this.isOptional = parent.ctx.isImplicit(baseProp);
        this.funcName = null;
        this.targetTypeBuilder = null;
        this.recursive = false;
    }

    DtoPropBuilder(
        DtoTypeBuilder<T, P> parent,
        DtoParser.PositivePropContext prop
    ) {
        this.parent = parent;
        this.baseLine = prop.prop.getLine();
        this.aliasLine = prop.alias != null ? prop.alias.getLine() : prop.prop.getLine();

        CompilerContext<T, P> ctx = parent.ctx;
        P baseProp = getBaseProp(parent, prop.prop);

        String funcName = null;
        if (prop.func != null) {
            if (parent.currentAliasGroup() != null) {
                throw ctx.exception(
                        prop.func.getLine(),
                        "Function invocation is forbidden in alias group"
                );
            }
            funcName = prop.func.getText();
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
                    if (baseProp.isList()) {
                        throw ctx.exception(
                                prop.func.getLine(),
                                "Cannot call the function \"flat\" because the current prop \"" +
                                        baseProp +
                                        "\" is list"
                        );
                    }
                    break;
                default:
                    throw ctx.exception(
                            prop.func.getLine(),
                            "The function name must be \"id\" or \"flat\""
                    );
            }
        }

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
                        "The alias cannot be specified when the function `flat` is used"
                );
            }
            alias = prop.alias.getText();
        } else if (parent.currentAliasGroup() != null) {
            alias = parent.currentAliasGroup().alias(prop.prop);
        } else if ("id".equals(funcName)) {
            alias = baseProp.getName() + "Id";
        } else if ("flat".equals(funcName)) {
            alias = null;
        } else {
            alias = baseProp.getName();
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
        }

        DtoTypeBuilder<T, P> targetTypeBuilder = null;
        if (prop.dtoBody() != null) {
            if (!baseProp.isAssociation(false)) {
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
                    parent.modifiers.contains(DtoTypeModifier.INPUT) ?
                            Collections.singleton(DtoTypeModifier.INPUT) :
                            Collections.emptySet(),
                    Collections.emptyList(),
                    prop.recursive != null ? baseProp : null,
                    prop.recursive != null ? alias : null,
                    ctx
            );
        } else if (baseProp.isAssociation(false) && !"id".equals(funcName)) {
            throw ctx.exception(
                    prop.stop.getLine(),
                    "Illegal property \"" +
                            baseProp.getName() +
                            "\", the child body is required"
            );
        }

        this.baseProp = baseProp;
        this.alias = alias;
        this.isOptional = ctx.isImplicit(baseProp) || prop.recursive != null || prop.optional != null;
        this.funcName = funcName;
        this.targetTypeBuilder = targetTypeBuilder;
        this.recursive = prop.recursive != null;
    }

    @Override
    public P getBaseProp() {
        return baseProp;
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
        P baseProp = ctx.getProps(baseType).get(baseName);
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
        return baseProp;
    }

    DtoProp<T, P> build() {
        return new DtoPropImpl<>(
                baseProp,
                baseLine,
                alias,
                aliasLine,
                targetTypeBuilder != null ? targetTypeBuilder.build() : null,
                isOptional,
                funcName,
                recursive
        );
    }
}
