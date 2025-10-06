package vn.viettel.vds.promotion.validation.domain.valueobject;

import java.util.Objects;

/**
 * Value object representing a version number
 */
public class Version implements Comparable<Version> {
    private final int major;
    private final int minor;
    private final int patch;

    private Version(int major, int minor, int patch) {
        if (major < 0 || minor < 0 || patch < 0) {
            throw new IllegalArgumentException("Version numbers cannot be negative");
        }
        this.major = major;
        this.minor = minor;
        this.patch = patch;
    }

    public static Version of(int major, int minor, int patch) {
        return new Version(major, minor, patch);
    }

    public static Version initial() {
        return new Version(1, 0, 0);
    }

    public static Version parse(String versionString) {
        Objects.requireNonNull(versionString, "Version string cannot be null");
        String[] parts = versionString.split("\\.");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Version must be in format major.minor.patch");
        }
        try {
            return new Version(
                    Integer.parseInt(parts[0]),
                    Integer.parseInt(parts[1]),
                    Integer.parseInt(parts[2])
            );
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid version format: " + versionString);
        }
    }

    public Version incrementMajor() {
        return new Version(major + 1, 0, 0);
    }

    public Version incrementMinor() {
        return new Version(major, minor + 1, 0);
    }

    public Version incrementPatch() {
        return new Version(major, minor, patch + 1);
    }

    public boolean isNewerThan(Version other) {
        if (major != other.major) return major > other.major;
        if (minor != other.minor) return minor > other.minor;
        return patch > other.patch;
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
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Version version = (Version) o;
        return major == version.major && minor == version.minor && patch == version.patch;
    }

    @Override
    public int hashCode() {
        return Objects.hash(major, minor, patch);
    }

    @Override
    public String toString() {
        return String.format("%d.%d.%d", major, minor, patch);
    }

    @Override
    public int compareTo(Version other) {
        if (major != other.major) {
            return Integer.compare(major, other.major);
        }
        if (minor != other.minor) {
            return Integer.compare(minor, other.minor);
        }
        return Integer.compare(patch, other.patch);
    }
}