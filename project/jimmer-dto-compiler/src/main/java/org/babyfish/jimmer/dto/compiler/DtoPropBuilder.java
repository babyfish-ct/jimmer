package org.babyfish.jimmer.dto.compiler;

import org.antlr.v4.runtime.Token;
import org.babyfish.jimmer.dto.compiler.spi.BaseProp;
import org.babyfish.jimmer.dto.compiler.spi.BaseType;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class DtoPropBuilder<T extends BaseType, P extends BaseProp> implements DtoPropImplementor, AbstractPropBuilder {

    private final DtoTypeBuilder<T, P> parent;

    private final P baseProp;

    private final int baseLine;

    private final String alias;

    private final int aliasLine;

    private final List<Anno> annotations;

    private final Mandatory mandatory;

    private final String funcName;

    private final DtoTypeBuilder<T, P> targetTypeBuilder;

    private final EnumType enumType;

    private final boolean recursive;

    DtoPropBuilder(
            DtoTypeBuilder<T, P> parent,
            P baseProp,
            int line,
            Mandatory mandatory
    ) {
        this.parent = parent;
        this.baseProp = baseProp;
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
    }

    DtoPropBuilder(
        DtoTypeBuilder<T, P> parent,
        DtoParser.PositivePropContext prop
    ) {
        this.parent = parent;
        this.baseLine = prop.prop.getLine();
        this.aliasLine = prop.alias != null ? prop.alias.getLine() : prop.prop.getLine();

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
            if (baseProp.getName().equals("s")) {
                throw ctx.exception(
                        prop.prop.getLine(),
                        "The alias must be specified for the property with " +
                                "`id` function when the base property name ends with 's'"
                );
            }
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
        if (prop.required != null) {
            if ("flat".equals(funcName)) {
                throw ctx.exception(
                        prop.required.getLine(),
                        "Illegal required modifier '!', it is not allowed for the function `flat`"
                );
            }
            if (baseProp.isId()) {
                if (!parent.modifiers.contains(DtoTypeModifier.INPUT) &&
                        !parent.modifiers.contains(DtoTypeModifier.INPUT_ONLY)) {
                    throw ctx.exception(
                            prop.required.getLine(),
                            "Illegal required modifier '!' for id property, " +
                                    "it can only be used in input/input-only type"
                    );
                }
            } else {
                if (!parent.modifiers.contains(DtoTypeModifier.INPUT_ONLY)) {
                    throw ctx.exception(
                            prop.required.getLine(),
                            "Illegal required modifier '!' for non-id property, " +
                                    "it can only be used in input-only type"
                    );
                }
                if (!baseProp.isNullable()) {
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
                    ctx.getTargetType(baseProp),
                    dtoBody,
                    null,
                    prop.annotations,
                    parent.modifiers.contains(DtoTypeModifier.INPUT) ?
                            Collections.singleton(DtoTypeModifier.INPUT) :
                            parent.modifiers.contains(DtoTypeModifier.INPUT_ONLY) ?
                            Collections.singleton(DtoTypeModifier.INPUT_ONLY) :
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

        this.baseProp = baseProp;
        this.alias = alias;
        if (prop.required != null) {
            this.mandatory = Mandatory.REQUIRED;
        } else if (prop.optional != null || prop.recursive != null || ctx.isImplicitId(baseProp, parent.modifiers)) {
            this.mandatory = Mandatory.OPTIONAL;
        } else {
            this.mandatory = Mandatory.DEFAULT;
        }
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

    @Override
    public DtoProp<T, P> build() {
        return new DtoPropImpl<>(
                baseProp,
                baseLine,
                alias,
                aliasLine,
                annotations,
                targetTypeBuilder != null ? targetTypeBuilder.build() : null,
                enumType,
                mandatory,
                funcName,
                recursive
        );
    }
}
