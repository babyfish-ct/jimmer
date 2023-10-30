package org.babyfish.jimmer.dto.compiler;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class Constants {

    public static final Map<String, String> QBE_FUNC_MAP;

    static {
        Map<String, String> qbeFuncMap = new HashMap<>();
        qbeFuncMap.put("eq", "ne");
        qbeFuncMap.put("ne", "eq");
        qbeFuncMap.put("gt", "le");
        qbeFuncMap.put("ge", "lt");
        qbeFuncMap.put("lt", "ge");
        qbeFuncMap.put("le", "gt");
        qbeFuncMap.put("like", null);
        qbeFuncMap.put("valueIn", null);
        qbeFuncMap.put("associatedIdIn", null);
        QBE_FUNC_MAP = Collections.unmodifiableMap(qbeFuncMap);
    }
}
