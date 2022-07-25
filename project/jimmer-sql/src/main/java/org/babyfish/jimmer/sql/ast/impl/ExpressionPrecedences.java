package org.babyfish.jimmer.sql.ast.impl;

public class ExpressionPrecedences {

    private ExpressionPrecedences() {}

    /*
     * 1 ~ (Bitwise NOT)
     * 2 * (Multiplication), / (Division), % (Modulus)
     * 3 + (Positive), - (Negative), + (Addition), + (Concatenation), - (Subtraction), & (Bitwise AND), ^ (Bitwise Exclusive OR), | (Bitwise OR)
     * 4 =, >, <, >=, <=, <>, !=, !>, !< (Comparison operators)
     * 5 NOT
     * 6 AND
     * 7 OR
     */
    public static final int TIMES = 2;

    public static final int PLUS = 3;

    public static final int COMPARISON = 4;

    public static final int NOT = 5;

    public static final int AND = 6;

    public static final int OR = 7;
}
