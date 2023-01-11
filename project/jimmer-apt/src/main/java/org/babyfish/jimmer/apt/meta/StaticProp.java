package org.babyfish.jimmer.apt.meta;

public class StaticProp {

    private final String ownerAlias;

    private final String name;

    private final boolean enabled;

    private final boolean optional;

    private final boolean asTargetId;

    private final String targetAlias;

    private final StaticDeclaration target;

    public StaticProp(
            String ownerAlias,
            String name,
            boolean enabled,
            boolean optional,
            boolean asTargetId,
            String targetAlias
    ) {
        this.ownerAlias = ownerAlias;
        this.name = name;
        this.enabled = enabled;
        this.optional = optional;
        this.asTargetId = asTargetId;
        this.targetAlias = targetAlias;
        this.target = null;
    }

    private StaticProp(
            StaticProp base,
            StaticDeclaration target
    ) {
        this.ownerAlias = base.ownerAlias;
        this.name = base.name;
        this.enabled = base.enabled;
        this.optional = base.optional;
        this.asTargetId = base.asTargetId;
        this.targetAlias = base.targetAlias;
        this.target = target;
    }

    public String getOwnerAlias() {
        return ownerAlias;
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

    public boolean isAsTargetId() {
        return asTargetId;
    }

    public String getTargetAlias() {
        return targetAlias;
    }

    public StaticDeclaration getTarget() {
        return target;
    }

    public StaticProp target(StaticDeclaration target) {
        if (this.target == target) {
            return this;
        }
        return new StaticProp(this, target);
    }

    @Override
    public String toString() {
        return "StaticProp{" +
                "ownerAlias='" + ownerAlias + '\'' +
                ", name='" + name + '\'' +
                ", enabled=" + enabled +
                ", optional=" + optional +
                ", asTargetId=" + asTargetId +
                ", targetAlias='" + targetAlias + '\'' +
                '}';
    }
}
