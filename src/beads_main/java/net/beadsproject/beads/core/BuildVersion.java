package net.beadsproject.beads.core;

public final class BuildVersion {
    static final int MAJOR = 1;
    static final int MINOR = 0;
    static final int BUILD = 3;

    public static int getMajor() {
        return MAJOR;
    }

    public static int getMinor() {
        return MINOR;
    }

    public static int getBuild() {
        return BUILD;
    }


    /**
     * Gets the text to display minimum compatibility between device and plugin
     *
     * @return Minimum device compatibility
     */
    public static String getMinimumCompatibilityVersion() {
        String ret = MAJOR + "." + MINOR + ".X.X";
        return ret;

    }


    /**
     * Get The full version info as a string
     *
     * @return version details as a string
     */
    public static String getVersionText() {
        String ret = MAJOR + "." + MINOR + "." + BUILD + "." + getDate();
        return ret;
    }

    /**
     * Get the date that the class was actually compiled
     *
     * @return n integer representing date - not implemented yet
     */
    public static int getDate() {
        int ret = 0;

        return ret;
    }
}
