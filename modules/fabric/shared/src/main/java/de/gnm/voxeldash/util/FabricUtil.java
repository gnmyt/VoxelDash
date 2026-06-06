package de.gnm.voxeldash.util;

public final class FabricUtil {

    private static FabricCompat compat;

    private FabricUtil() {
    }

    public static void setCompat(FabricCompat compat) {
        FabricUtil.compat = compat;
    }

    public static FabricCompat compat() {
        return compat;
    }
}
