package org.babyfish.jimmer.dto.compiler;

import java.util.*;

public class Constants {

    public static final Set<String> QBE_FUNC_NAMES;

    public static final Set<String> MULTI_ARGS_FUNC_NAMES;

    static {
        Set<String> qbeFuncNames = new HashSet<>();
        qbeFuncNames.add("eq");
        qbeFuncNames.add("ne");
        qbeFuncNames.add("gt");
        qbeFuncNames.add("ge");
        qbeFuncNames.add("lt");
        qbeFuncNames.add("le");
        qbeFuncNames.add("like");
        qbeFuncNames.add("notLike");
        qbeFuncNames.add("null");
        qbeFuncNames.add("notNull");
        qbeFuncNames.add("valueIn");
        qbeFuncNames.add("valueNotIn");
        qbeFuncNames.add("associatedIdEq");
        qbeFuncNames.add("associatedIdNe");
        qbeFuncNames.add("associatedIdIn");
        qbeFuncNames.add("associatedIdNotIn");
        QBE_FUNC_NAMES = Collections.unmodifiableSet(qbeFuncNames);

        Set<String> multiArgsFuncNames = new HashSet<>();
        multiArgsFuncNames.add("eq");
        multiArgsFuncNames.add("like");
        multiArgsFuncNames.add("null");
        multiArgsFuncNames.add("notNull");
        multiArgsFuncNames.add("valueIn");
        multiArgsFuncNames.add("associatedIdEq");
        multiArgsFuncNames.add("associatedIdIn");

        MULTI_ARGS_FUNC_NAMES = Collections.unmodifiableSet(multiArgsFuncNames);
    }
}
