package org.babyfish.jimmer.sql.ddl;

/**
 * @author honhimW
 */

public class DatabaseVersion {

    public final int major;

    public final int minor;

    public final String productVersion;

    public static final DatabaseVersion LATEST = new DatabaseVersion(Integer.MAX_VALUE, Integer.MAX_VALUE, "");

    public DatabaseVersion(int major, int minor, String productVersion) {
        this.major = major;
        this.minor = minor;
        this.productVersion = productVersion;
    }

    public boolean isSameOrAfter(int major) {
        return this.major >= major;
    }

    public boolean isSameOrAfter(int major, int minor) {
        return this.major > major || (this.major == major && this.minor >= minor);
    }

}
