package de.gnm.voxeldash.util;

public final class ForgeUtil {

    private static ForgeCompat compat;

    private ForgeUtil() {
    }

    public static void setCompat(ForgeCompat compat) {
        ForgeUtil.compat = compat;
    }

    public static ForgeCompat compat() {
        return compat;
    }
}
