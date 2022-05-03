package org.babyfish.jimmer.meata;

public enum ImmutablePropCategory {

    SCALAR(false, false),
    SCALAR_LIST(false, true),
    REFERENCE(true, false),
    ENTITY_LIST(true, true);

    private boolean isAssociation;

    private boolean isList;

    ImmutablePropCategory(boolean isAssociation, boolean isList) {
        this.isAssociation = isAssociation;
        this.isList = isList;
    }

    public boolean isAssociation() {
        return isAssociation;
    }

    public boolean isList() {
        return isList;
    }
}
