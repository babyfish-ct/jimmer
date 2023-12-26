package org.babyfish.jimmer.dto.compiler;

import org.antlr.v4.runtime.Token;

import java.util.*;

public class EnumType {

    private final boolean isNumeric;

    private final Map<String, String> valueMap;

    private final Map<String, String> constantMap;

    private EnumType(
            boolean isNumeric,
            Map<String, String> valueMap,
            Map<String, String> constantMap
    ) {
        this.isNumeric = isNumeric;
        this.valueMap = valueMap;
        this.constantMap = constantMap;
    }

    public boolean isNumeric() {
        return isNumeric;
    }

    public Map<String, String> getValueMap() {
        return valueMap;
    }

    public Map<String, String> getConstantMap() {
        return constantMap;
    }

    public static EnumType of(
            CompilerContext<?, ?> ctx,
            Collection<String> constants,
            DtoParser.EnumBodyContext enumBody
    ) {
        Boolean isNumeric = null;
        Set<String> constantSet = new HashSet<>(constants);
        Map<String, String> valueMap = new LinkedHashMap<>((enumBody.mappings.size() * 4 + 2) / 3);
        Map<String, String> constantMap = new LinkedHashMap<>((enumBody.mappings.size() * 4 + 2) / 3);
        for (DtoParser.EnumMappingContext mapping : enumBody.mappings) {
            String constant = mapping.constant.getText();
            if (!constantSet.remove(constant)) {
                if (valueMap.containsKey(constant)) {
                    throw ctx.exception(
                            mapping.constant.getLine(),
                            mapping.constant.getCharPositionInLine(),
                            "Duplicated enum constant: \"" + constant + "\""
                    );
                }
                throw ctx.exception(
                        mapping.constant.getLine(),
                        mapping.constant.getCharPositionInLine(),
                        "Illegal enum constant: \"" + constant + "\""
                );
            }
            Token valueToken = mapping.value;
            if (isNumeric != null && isNumeric != (valueToken.getType() == DtoParser.IntegerLiteral)) {
                throw ctx.exception(
                        mapping.constant.getLine(),
                        mapping.constant.getCharPositionInLine(),
                        "Illegal value of enum constant: \"" +
                                constant +
                                "\", integer value and string value cannot be mixed"
                );
            }
            isNumeric = valueToken.getType() == DtoParser.IntegerLiteral;
            valueMap.put(constant, valueToken.getText());
            String conflictConstant = constantMap.put(valueToken.getText(), constant);
            if (conflictConstant != null) {
                throw ctx.exception(
                        mapping.constant.getLine(),
                        mapping.constant.getCharPositionInLine(),
                        "Illegal enum constant: \"" + constant + "\", " +
                                "its value is same with the value of \"" + conflictConstant + "\""
                );
            }
        }
        if (!constantSet.isEmpty()) {
            throw ctx.exception(
                    enumBody.start.getLine(),
                    enumBody.start.getCharPositionInLine(),
                    "The mapping(s) for " + constantSet + " is(are) not defined"
            );
        }
        assert isNumeric != null;
        return new EnumType(isNumeric, valueMap, constantMap);
    }
}
