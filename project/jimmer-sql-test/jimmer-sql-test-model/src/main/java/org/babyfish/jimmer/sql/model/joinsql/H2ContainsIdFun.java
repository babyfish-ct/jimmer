package org.babyfish.jimmer.sql.model.joinsql;

import java.util.regex.Pattern;

public class H2ContainsIdFun {

    private static final Pattern COMMA_PATTERN = Pattern.compile("\\s*,\\s*");

    public static boolean contains(String idArrStr, long id) {
        if (idArrStr == null || idArrStr.isEmpty()) {
            return false;
        }
        String idStr = Long.toString(id);
        for (String part : COMMA_PATTERN.split(idArrStr)) {
            if (part.equals(idStr)) {
                return true;
            }
        }
        return false;
    }
}
