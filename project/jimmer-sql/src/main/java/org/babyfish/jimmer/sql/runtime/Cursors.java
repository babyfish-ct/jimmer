package org.babyfish.jimmer.sql.runtime;

public class Cursors {

    private static final ThreadLocal<Long> CURRENT_ID_LOCAL = new ThreadLocal<>();

    private Cursors() {}

    public static Long currentCursorId() {
        return CURRENT_ID_LOCAL.get();
    }

    static Long setCurrentCursorId(Long cursorId) {
        Long oldValue = CURRENT_ID_LOCAL.get();
        if (cursorId != null) {
            CURRENT_ID_LOCAL.set(cursorId);
        } else {
            CURRENT_ID_LOCAL.remove();
        }
        return oldValue;
    }
}
