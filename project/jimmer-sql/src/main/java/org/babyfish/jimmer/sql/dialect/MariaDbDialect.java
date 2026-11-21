package org.babyfish.jimmer.sql.dialect;

import org.babyfish.jimmer.sql.ast.impl.query.ForUpdate;
import org.babyfish.jimmer.sql.ast.impl.render.AbstractSqlBuilder;
import org.babyfish.jimmer.sql.ast.query.LockWait;

import java.util.function.IntSupplier;

/**
 * MariaDB supports MySQL-style upserts and InnoDB cache tables.
 * Shared locking reads use {@code LOCK IN SHARE MODE}.
 */
public class MariaDbDialect extends MySqlDialect {

    private final boolean supportsLockTimeout;
    private final boolean supportsSkipLocked;

    /** Use the locking syntax available across MariaDB versions. */
    public MariaDbDialect() {
        this(0, 0);
    }

    /**
     * Enable {@code NOWAIT} and {@code WAIT n} from MariaDB 10.3,
     * and {@code SKIP LOCKED} from MariaDB 10.6 (InnoDB).
     */
    public MariaDbDialect(int majorVersion, int minorVersion) {
        supportsLockTimeout = majorVersion > 10 || (majorVersion == 10 && minorVersion >= 3);
        supportsSkipLocked = majorVersion > 10 || (majorVersion == 10 && minorVersion >= 6);
    }

    @Override
    public void renderForUpdate(AbstractSqlBuilder<?> builder, ForUpdate forUpdate) {
        switch (forUpdate.getLockMode()) {
            case UPDATE:
                builder.sql(" for update");
                break;
            case SHARE:
                builder.sql(" lock in share mode");
                break;
            default:
                throw new IllegalArgumentException("MariaDB does not support " + forUpdate.getLockMode());
        }
        LockWait wait = forUpdate.getLockWait();
        if (wait == LockWait.DEFAULT) {
            return;
        }
        if (wait == LockWait.NO_WAIT && supportsLockTimeout) {
            builder.sql(" nowait");
        } else if (wait instanceof IntSupplier && supportsLockTimeout) {
            builder.sql(" wait ").sql(Integer.toString(((IntSupplier) wait).getAsInt()));
        } else if (wait == LockWait.SKIP_LOCKED && supportsSkipLocked) {
            builder.sql(" skip locked");
        } else {
            throw new IllegalArgumentException("The configured MariaDB version does not support " + wait);
        }
    }
}
