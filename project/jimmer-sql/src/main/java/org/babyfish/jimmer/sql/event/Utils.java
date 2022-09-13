package org.babyfish.jimmer.sql.event;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.TargetLevel;

class Utils {

    private Utils() {}

    static ImmutableProp primaryAssociationProp(ImmutableProp prop) {
        ImmutableProp mappedBy = prop.getMappedBy();
        if (mappedBy != null) {
            return mappedBy;
        }
        if (!prop.isAssociation(TargetLevel.ENTITY)) {
            throw new IllegalArgumentException("\"" + prop + "\" is not association");
        }
        return prop;
    }
}
