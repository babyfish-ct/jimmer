package org.babyfish.jimmer.apt.meta;

import com.squareup.javapoet.TypeName;

public class StaticProp {

    private final ImmutableProp immutableProp;

    private final String alias;

    private final String name;

    private final boolean enabled;

    private final boolean optional;

    private final boolean idOnly;

    private final String targetAlias;

    private final StaticDeclaration target;

    public StaticProp(
            ImmutableProp immutableProp,
            String alias,
            String name,
            boolean enabled,
            boolean optional,
            boolean idOnly,
            String targetAlias
    ) {
        this.immutableProp = immutableProp;
        this.alias = alias;
        this.name = name;
        this.enabled = enabled;
        this.optional = optional;
        this.idOnly = idOnly;
        this.targetAlias = targetAlias;
        this.target = null;
    }

    private StaticProp(
            StaticProp base,
            StaticDeclaration target
    ) {
        this.immutableProp = base.immutableProp;
        this.alias = base.alias;
        this.name = base.name;
        this.enabled = base.enabled;
        this.optional = base.optional;
        this.idOnly = base.idOnly;
        this.targetAlias = base.targetAlias;
        this.target = target;
    }

    private StaticProp(
            StaticProp base,
            boolean optional
    ) {
        this.immutableProp = base.immutableProp;
        this.alias = base.alias;
        this.name = base.name;
        this.enabled = base.enabled;
        this.optional = optional;
        this.idOnly = base.idOnly;
        this.targetAlias = base.targetAlias;
        this.target = base.target;
    }

    public ImmutableProp getImmutableProp() {
        return immutableProp;
    }

    public String getAlias() {
        return alias;
    }

    public String getName() {
        return name;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isOptional() {
        return optional;
    }

    public boolean isIdOnly() {
        return idOnly;
    }

    public String getTargetAlias() {
        return targetAlias;
    }

    public StaticDeclaration getTarget() {
        return target;
    }

    public boolean isNullable() {
        return optional || immutableProp.isNullable();
    }

    public StaticProp target(StaticDeclaration target) {
        if (this.target == target) {
            return this;
        }
        return new StaticProp(this, target);
    }

    public StaticProp optional(boolean optional) {
        if (this.optional) {
            return this;
        }
        return new StaticProp(this, optional);
    }

    public String getGetterName() {
        return (immutableProp.getTypeName() == TypeName.BOOLEAN ? "is": "get") +
                Character.toUpperCase(name.charAt(0)) +
                name.substring(1);
    }

    public String getSetterName() {
        return "set" +
                Character.toUpperCase(name.charAt(0)) +
                name.substring(1);
    }

    public String getDefaultValue() {
        TypeName typeName = immutableProp.getTypeName();
        if (typeName.isPrimitive()) {
            if (typeName == TypeName.BOOLEAN) {
                return "false";
            }
            if (typeName == TypeName.CHAR) {
                return "'\\0'";
            }
            return "0";
        }
        return "null";
    }

    @Override
    public String toString() {
        return "StaticProp{" +
                "immutableProp=" + immutableProp +
                ", ownerAlias='" + alias + '\'' +
                ", name='" + name + '\'' +
                ", enabled=" + enabled +
                ", optional=" + optional +
                ", idOnly=" + idOnly +
                ", targetAlias='" + targetAlias + '\'' +
                ", target=" + target +
                '}';
    }
}
