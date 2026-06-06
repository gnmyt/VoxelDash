package de.gnm.voxeldash.util;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.fabricmc.loader.api.metadata.Person;

import java.io.File;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public final class FabricPlatform {

    private static final Gson GSON = new Gson();
    private static final Type MAP_TYPE = new TypeToken<Map<String, Object>>() {}.getType();

    private FabricPlatform() {
    }

    public static String minecraftVersion() {
        return FabricLoader.getInstance()
                .getModContainer("minecraft")
                .map(mod -> mod.getMetadata().getVersion().getFriendlyString())
                .orElse("Unknown");
    }

    public static File modsFolder() {
        return FabricLoader.getInstance().getGameDir().resolve("mods").toFile();
    }

    public static File configDir() {
        return FabricLoader.getInstance().getConfigDir().toFile();
    }

    public static List<FabricCompat.ModEntry> loadedMods() {
        List<FabricCompat.ModEntry> result = new ArrayList<FabricCompat.ModEntry>();
        for (ModContainer mod : FabricLoader.getInstance().getAllMods()) {
            ModMetadata meta = mod.getMetadata();
            String[] authors = null;
            try {
                List<String> names = new ArrayList<String>();
                for (Person person : meta.getAuthors()) names.add(person.getName());
                if (!names.isEmpty()) authors = names.toArray(new String[0]);
            } catch (Throwable ignored) {
            }
            result.add(new FabricCompat.ModEntry(
                    meta.getId(),
                    meta.getName(),
                    meta.getVersion().getFriendlyString(),
                    meta.getDescription(),
                    authors,
                    null));
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    public static FabricCompat.ModEntry parseJarMeta(File jarFile) {
        if (jarFile == null || !jarFile.exists()) return null;
        try (JarFile jar = new JarFile(jarFile)) {
            JarEntry entry = jar.getJarEntry("fabric.mod.json");
            if (entry == null) return null;
            try (InputStreamReader reader = new InputStreamReader(jar.getInputStream(entry))) {
                Map<String, Object> json = GSON.fromJson(reader, MAP_TYPE);
                if (json == null) return null;

                String id = (String) json.get("id");
                String name = (String) json.get("name");
                Object version = json.get("version");
                String description = (String) json.get("description");

                String[] authors = null;
                Object authorsObj = json.get("authors");
                if (authorsObj instanceof List) {
                    List<?> authorsList = (List<?>) authorsObj;
                    List<String> names = new ArrayList<String>();
                    for (Object a : authorsList) {
                        if (a instanceof String) names.add((String) a);
                        else if (a instanceof Map) names.add(String.valueOf(((Map<?, ?>) a).get("name")));
                        else names.add(String.valueOf(a));
                    }
                    if (!names.isEmpty()) authors = names.toArray(new String[0]);
                }

                return new FabricCompat.ModEntry(id, name,
                        version != null ? String.valueOf(version) : null,
                        description, authors, null);
            }
        } catch (Exception e) {
            return null;
        }
    }
}
