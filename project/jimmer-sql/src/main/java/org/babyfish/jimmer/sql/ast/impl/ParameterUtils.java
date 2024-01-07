package org.babyfish.jimmer.sql.ast.impl;

class ParameterUtils {

    static <T> T validate(String predicateName, String parameterName, T parameter) {
        if (parameter == null) {
            throw new IllegalArgumentException(
                    "The predicate \"" +
                            predicateName +
                            "\" cannot accept a null parameter \"" +
                            parameterName +
                            "\", this is to ensure that the current predicate can be created to avoid bugs; " +
                            "if you are sure you expect to ignore the creation of the current predicate " +
                            "when the parameter is null in order to achieve the purpose of dynamic query, " +
                            "please use \"" +
                            predicateName +
                            "If\""
            );
        }
        return parameter;
    }
}
