package org.babyfish.jimmer;

import java.util.Objects;

public class JimmerVersion {

    public static final JimmerVersion CURRENT =
            new JimmerVersion(0, 7, 61);

    private final int major;

    private final int minor;

    private final int patch;

    private JimmerVersion(int major, int minor, int patch) {
        this.major = major;
        this.minor = minor;
        this.patch = patch;
    }

    public int getMajor() {
        return major;
    }

    public int getMinor() {
        return minor;
    }

    public int getPatch() {
        return patch;
    }

    @Override
    public int hashCode() {
        return Objects.hash(major, minor, patch);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof JimmerVersion)) return false;
        JimmerVersion that = (JimmerVersion) o;
        return major == that.major && minor == that.minor && patch == that.patch;
    }

    @Override
    public String toString() {
        return major + "." + minor + "." + patch;
    }
}
