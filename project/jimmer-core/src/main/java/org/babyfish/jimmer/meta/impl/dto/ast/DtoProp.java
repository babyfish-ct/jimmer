package org.babyfish.jimmer.meta.impl.dto.ast;

import org.jetbrains.annotations.Nullable;

public class DtoProp {

    private final boolean negative;

    private final String name;

    private final boolean idOnly;

    @Nullable
    private final String alias;

    @Nullable
    private final Dto targetDto;

    private final boolean recursive;

    DtoProp(String name, boolean idOnly, @Nullable String alias, @Nullable Dto targetDto, boolean recursive) {
        this.recursive = recursive;
        this.negative = false;
        this.name = name;
        this.idOnly = idOnly;
        this.alias = alias;
        this.targetDto = targetDto;
    }

    DtoProp(boolean negative, String name) {
        this.negative = negative;
        this.name = name;
        this.idOnly = false;
        this.alias = null;
        this.targetDto = null;
        this.recursive = false;
    }

    public boolean isNegative() {
        return negative;
    }

    public String getName() {
        return name;
    }

    public boolean isIdOnly() {
        return idOnly;
    }

    @Nullable
    public String getAlias() {
        return alias;
    }

    @Nullable
    public Dto getTargetDto() {
        return targetDto;
    }

    public boolean isRecursive() {
        return recursive;
    }

    @Override
    public String toString() {
        return "DtoProp{" +
                "negative=" + negative +
                ", name='" + name + '\'' +
                ", idOnly=" + idOnly +
                ", alias='" + alias + '\'' +
                ", targetDto=" + targetDto +
                ", recursive=" + recursive +
                '}';
    }
}
