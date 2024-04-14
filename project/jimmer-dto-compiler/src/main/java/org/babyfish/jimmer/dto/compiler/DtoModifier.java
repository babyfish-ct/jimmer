package org.babyfish.jimmer.dto.compiler;

public enum DtoModifier {
    INPUT(false, 2),
    SPECIFICATION(false, 2),
    UNSAFE(false, 0),
    FIXED(true, 1),
    STATIC(true, 1),
    DYNAMIC(true, 1),
    FUZZY(true, 1);

    private final boolean inputStrategy;

    private final int order;

    DtoModifier(boolean inputStrategy, int order) {
        this.inputStrategy = inputStrategy;
        this.order = order;
    }

    public boolean isInputStrategy() {
        return inputStrategy;
    }

    public int getOrder() {
        return order;
    }
}
