package org.babyfish.jimmer.client.runtime;

import org.babyfish.jimmer.client.meta.Doc;

public class FetchByInfo {

    private final String constant;

    private final Class<?> ownerType;

    private final Doc doc;

    public FetchByInfo(String constant, Class<?> ownerType, Doc doc) {
        this.constant = constant;
        this.ownerType = ownerType;
        this.doc = doc;
    }

    public String getConstant() {
        return constant;
    }

    public Class<?> getOwnerType() {
        return ownerType;
    }

    public Doc getDoc() {
        return doc;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FetchByInfo fetchByInfo = (FetchByInfo) o;

        if (!constant.equals(fetchByInfo.constant)) return false;
        return ownerType.equals(fetchByInfo.ownerType);
    }

    @Override
    public int hashCode() {
        int result = constant.hashCode();
        result = 31 * result + ownerType.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return ownerType.getName() + '/' + constant;
    }
}
