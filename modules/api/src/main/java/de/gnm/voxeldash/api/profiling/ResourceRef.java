package de.gnm.voxeldash.api.profiling;

public final class ResourceRef {

    /**
     * The kind of resource: {@code plugin}, {@code mod}, {@code minecraft},
     * {@code jvm}, {@code shared} or {@code unknown}.
     */
    public static final String TYPE_PLUGIN = "plugin";
    public static final String TYPE_MOD = "mod";
    public static final String TYPE_MINECRAFT = "minecraft";
    public static final String TYPE_JVM = "jvm";
    public static final String TYPE_SHARED = "shared";
    public static final String TYPE_UNKNOWN = "unknown";

    public static final ResourceRef MINECRAFT = new ResourceRef("Minecraft", TYPE_MINECRAFT);
    public static final ResourceRef JVM = new ResourceRef("JVM", TYPE_JVM);
    public static final ResourceRef SHARED = new ResourceRef("(shared)", TYPE_SHARED);
    public static final ResourceRef UNKNOWN = new ResourceRef("(unknown)", TYPE_UNKNOWN);

    public final String name;
    public final String type;

    public ResourceRef(String name, String type) {
        this.name = name;
        this.type = type;
    }
}
