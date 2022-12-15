package org.babyfish.jimmer.client.meta;

import java.util.Objects;

public class FetchByInfo {

    private final Class<?> ownerType;

    private final String constant;

    public FetchByInfo(Class<?> ownerType, String constant) {
        this.ownerType = ownerType;
        this.constant = constant;
    }

    public Class<?> getOwnerType() {
        return ownerType;
    }

    public String getConstant() {
        return constant;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FetchByInfo that = (FetchByInfo) o;
        return ownerType.equals(that.ownerType) && constant.equals(that.constant);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ownerType, constant);
    }

    @Override
    public String toString() {
        return "FetchByInfo{" +
                "ownerType=" + ownerType +
                ", constant='" + constant + '\'' +
                '}';
    }
}
