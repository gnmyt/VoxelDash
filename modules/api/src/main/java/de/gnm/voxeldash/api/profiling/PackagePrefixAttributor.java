package de.gnm.voxeldash.api.profiling;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class PackagePrefixAttributor implements ResourceAttributor {

    private static final Map<String, ResourceRef> BUILTIN = new LinkedHashMap<>();
    /**
     * Package roots that are commonly shaded into many plugins/mods and would
     * otherwise be mis-attributed. Registration of these is ignored.
     */
    private static final String[] SHADED_DENYLIST = {
            "com.google.", "org.apache.", "org.json.", "com.fasterxml.",
            "org.yaml.", "kotlin.", "org.slf4j.", "org.intellij.",
            "org.jetbrains.", "net.kyori.", "co.aikar.", "org.objectweb.",
            "io.netty.", "it.unimi.dsi.", "org.checkerframework."
    };

    static {
        BUILTIN.put("net.minecraft.", ResourceRef.MINECRAFT);
        BUILTIN.put("com.mojang.", ResourceRef.MINECRAFT);
        BUILTIN.put("org.bukkit.", ResourceRef.MINECRAFT);
        BUILTIN.put("org.spigotmc.", ResourceRef.MINECRAFT);
        BUILTIN.put("io.papermc.", ResourceRef.MINECRAFT);
        BUILTIN.put("com.destroystokyo.", ResourceRef.MINECRAFT);
        BUILTIN.put("net.minecraftforge.", ResourceRef.MINECRAFT);
        BUILTIN.put("net.neoforged.", ResourceRef.MINECRAFT);
        BUILTIN.put("cpw.mods.", ResourceRef.MINECRAFT);
        BUILTIN.put("net.fabricmc.", ResourceRef.MINECRAFT);
        BUILTIN.put("org.spongepowered.asm.", ResourceRef.MINECRAFT); // mixin runtime
        BUILTIN.put("java.", ResourceRef.JVM);
        BUILTIN.put("javax.", ResourceRef.JVM);
        BUILTIN.put("jdk.", ResourceRef.JVM);
        BUILTIN.put("sun.", ResourceRef.JVM);
        BUILTIN.put("com.sun.", ResourceRef.JVM);
    }

    private final Map<String, ResourceRef> registered = new HashMap<>();
    private final Map<String, ResourceRef> cache = new HashMap<>();

    /**
     * Registers a package prefix as owned by the given resource. If the prefix
     * is already owned by a different resource it collapses to
     * {@link ResourceRef#SHARED}. Generic shaded roots are ignored.
     *
     * @param prefix package prefix, with or without trailing dot
     * @param name   resource display name
     * @param type   resource type ({@code plugin} or {@code mod})
     */
    public void register(String prefix, String name, String type) {
        if (prefix == null || prefix.isEmpty() || name == null) {
            return;
        }
        if (!prefix.endsWith(".")) {
            prefix = prefix + ".";
        }
        for (String denied : SHADED_DENYLIST) {
            if (prefix.startsWith(denied) || denied.startsWith(prefix)) {
                return;
            }
        }
        ResourceRef existing = registered.get(prefix);
        if (existing != null && !existing.name.equals(name)) {
            registered.put(prefix, ResourceRef.SHARED);
            return;
        }
        registered.put(prefix, new ResourceRef(name, type));
    }

    @Override
    public ResourceRef attribute(String className) {
        if (className == null || className.isEmpty()) {
            return ResourceRef.UNKNOWN;
        }
        ResourceRef cached = cache.get(className);
        if (cached != null) {
            return cached;
        }
        ResourceRef ref = resolve(className);
        cache.put(className, ref);
        return ref;
    }

    private ResourceRef resolve(String className) {
        ResourceRef best = null;
        int bestLen = -1;
        for (Map.Entry<String, ResourceRef> entry : registered.entrySet()) {
            String prefix = entry.getKey();
            if (className.startsWith(prefix) && prefix.length() > bestLen) {
                best = entry.getValue();
                bestLen = prefix.length();
            }
        }
        if (best != null) {
            return best;
        }
        for (Map.Entry<String, ResourceRef> entry : BUILTIN.entrySet()) {
            String prefix = entry.getKey();
            if (className.startsWith(prefix) && prefix.length() > bestLen) {
                best = entry.getValue();
                bestLen = prefix.length();
            }
        }
        return best != null ? best : ResourceRef.UNKNOWN;
    }
}
