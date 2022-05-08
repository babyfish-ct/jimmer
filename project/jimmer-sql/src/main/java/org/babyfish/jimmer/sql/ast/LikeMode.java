package org.babyfish.jimmer.sql.ast;

public enum LikeMode {

    EXACT(true, true),
    START(true, false),
    END(false, true),
    ANYWHERE(false, false);

    private boolean startExact;

    private boolean endExact;

    LikeMode(boolean startExact, boolean endExact) {
        this.startExact = startExact;
        this.endExact = endExact;
    }

    public boolean isStartExact() {
        return startExact;
    }

    public boolean isEndExact() {
        return endExact;
    }
}
