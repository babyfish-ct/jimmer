package org.babyfish.jimmer.sql.ast.impl.query;

import org.babyfish.jimmer.sql.ast.query.LockMode;
import org.babyfish.jimmer.sql.ast.query.LockWait;
import org.jetbrains.annotations.NotNull;

public final class ForUpdate {

    private final LockMode lockMode;

    private final LockWait lockWait;

    public ForUpdate(LockMode lockMode, LockWait lockWait) {
        if (lockMode == null) {
            lockMode = LockMode.UPDATE;
        }
        if (lockWait == null) {
            lockWait = LockWait.DEFAULT;
        }
        if (lockMode.isShared() && lockWait == LockWait.SKIP_LOCKED) {
            throw new IllegalArgumentException("shared lock mode cannot work with skip locked");
        }
        this.lockMode = lockMode;
        this.lockWait = lockWait;
    }

    @NotNull
    public LockMode getLockMode() {
        return lockMode;
    }

    @NotNull
    public LockWait getLockWait() {
        return lockWait;
    }

    @Override
    public int hashCode() {
        int result = lockMode.hashCode();
        result = 31 * result + lockWait.hashCode();
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ForUpdate)) return false;

        ForUpdate forUpdate = (ForUpdate) o;
        return lockMode == forUpdate.lockMode && lockWait.equals(forUpdate.lockWait);
    }

    @Override
    public String toString() {
        return "ForUpdate{" +
                "lockMode=" + lockMode +
                ", lockWait=" + lockWait +
                '}';
    }

    public static ForUpdate combine(ForUpdate a, ForUpdate b) {
        if (a == null) {
            return b;
        }
        if (b == null) {
            return a;
        }
        if (a.equals(b)) {
            return a;
        }
        throw new IllegalArgumentException(
                "Conflict ForUpdate, \"" +
                        a +
                        "\" and \"" +
                        b +
                        "\""
        );
    }
}
