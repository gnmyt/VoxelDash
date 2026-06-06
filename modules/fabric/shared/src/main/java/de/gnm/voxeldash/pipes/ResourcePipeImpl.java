package de.gnm.voxeldash.pipes;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import de.gnm.voxeldash.api.entities.ConfigFile;
import de.gnm.voxeldash.api.entities.Resource;
import de.gnm.voxeldash.api.entities.ResourceType;
import de.gnm.voxeldash.api.pipes.resources.ResourcePipe;
import de.gnm.voxeldash.util.FabricCompat;
import de.gnm.voxeldash.util.FabricUtil;

import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipFile;

public class ResourcePipeImpl implements ResourcePipe {

    private static final Gson GSON = new Gson();
    private static final Type MAP_TYPE = new TypeToken<Map<String, Object>>() {}.getType();
    private static final Set<String> BUILTIN_MODS = new HashSet<>(Arrays.asList("java", "minecraft", "fabricloader", "fabric-api", "fabric", "voxeldash"));
    private static final int MAX_CONFIG_FILES = 10;
    private static final String[] CONFIG_EXTENSIONS = {".yml", ".yaml", ".json", ".toml", ".properties", ".conf", ".cfg"};

    private static FabricCompat compat() {
        return FabricUtil.compat();
    }

    @Override
    public List<ResourceType> getSupportedResourceTypes() {
        if (compat().supportsDatapacks()) {
            return Arrays.asList(ResourceType.MOD, ResourceType.DATAPACK);
        }
        return Collections.singletonList(ResourceType.MOD);
    }

    @Override
    public List<Resource> getResources(ResourceType type) {
        List<Resource> resources;
        if (type == ResourceType.MOD) {
            resources = getMods();
        } else if (type == ResourceType.DATAPACK && compat().supportsDatapacks()) {
            resources = getDatapacks();
        } else {
            resources = new ArrayList<>();
        }
        resources.sort(Comparator.comparing(Resource::getName, String.CASE_INSENSITIVE_ORDER));
        return resources;
    }

    @Override
    public Resource getResource(String fileName, ResourceType type) {
        return getResources(type).stream().filter(r -> r.getFileName().equals(fileName)).findFirst().orElse(null);
    }

    @Override
    public boolean enableResource(String fileName, ResourceType type) {
        if (type == ResourceType.MOD) return enableMod(fileName);
        return type == ResourceType.DATAPACK && compat().supportsDatapacks() && enableDatapack(fileName);
    }

    @Override
    public boolean disableResource(String fileName, ResourceType type) {
        if (type == ResourceType.MOD) return disableMod(fileName);
        return type == ResourceType.DATAPACK && compat().supportsDatapacks() && disableDatapack(fileName);
    }

    @Override
    public boolean deleteResource(String fileName, ResourceType type) {
        if (type == ResourceType.MOD) return deleteMod(fileName);
        return type == ResourceType.DATAPACK && compat().supportsDatapacks() && deleteDatapack(fileName);
    }

    private List<Resource> getMods() {
        List<Resource> resources = new ArrayList<>();
        File modsFolder = getModsFolder();

        Map<String, FabricCompat.ModEntry> loadedByFile = new HashMap<>();
        Map<String, FabricCompat.ModEntry> loadedById = new HashMap<>();
        for (FabricCompat.ModEntry info : compat().loadedMods()) {
            if (info.modId == null || BUILTIN_MODS.contains(info.modId.toLowerCase())) continue;
            loadedById.put(info.modId.toLowerCase(), info);
            if (info.sourceFileName != null) loadedByFile.put(info.sourceFileName, info);
        }

        File[] jarFiles = modsFolder.listFiles((d, n) -> n.endsWith(".jar"));
        if (jarFiles != null) {
            for (File jarFile : jarFiles) {
                if (jarFile.getName().toLowerCase().contains("voxeldash")) continue;

                FabricCompat.ModEntry loaded = loadedByFile.get(jarFile.getName());
                FabricCompat.ModEntry meta = loaded == null ? compat().parseJarMeta(jarFile) : null;
                if (loaded == null && meta != null && meta.modId != null) {
                    loaded = loadedById.get(meta.modId.toLowerCase());
                }

                if (loaded != null) {
                    resources.add(new Resource.Builder()
                            .name(loaded.name != null ? loaded.name : loaded.modId)
                            .fileName(jarFile.getName())
                            .type(ResourceType.MOD)
                            .version(loaded.version)
                            .description(loaded.description)
                            .enabled(true)
                            .fileSize(jarFile.length())
                            .build());
                } else if (meta != null) {
                    resources.add(new Resource.Builder()
                            .name(meta.name != null ? meta.name : meta.modId)
                            .fileName(jarFile.getName())
                            .type(ResourceType.MOD)
                            .version(meta.version)
                            .description(meta.description)
                            .authors(meta.authors)
                            .enabled(false)
                            .fileSize(jarFile.length())
                            .build());
                } else {
                    resources.add(new Resource.Builder()
                            .name(jarFile.getName().replace(".jar", ""))
                            .fileName(jarFile.getName())
                            .type(ResourceType.MOD)
                            .enabled(false)
                            .fileSize(jarFile.length())
                            .build());
                }
            }
        }

        File[] disabledFiles = modsFolder.listFiles((d, n) -> n.endsWith(".jar.disabled"));
        if (disabledFiles != null) {
            for (File disabledFile : disabledFiles) {
                String baseName = disabledFile.getName().replace(".disabled", "");
                FabricCompat.ModEntry meta = compat().parseJarMeta(disabledFile);
                resources.add(new Resource.Builder()
                        .name(meta != null && meta.name != null ? meta.name : baseName.replace(".jar", ""))
                        .fileName(baseName)
                        .type(ResourceType.MOD)
                        .version(meta != null ? meta.version : null)
                        .description(meta != null ? meta.description : null)
                        .authors(meta != null ? meta.authors : null)
                        .enabled(false)
                        .fileSize(disabledFile.length())
                        .build());
            }
        }

        return resources;
    }

    private boolean enableMod(String fileName) {
        File modsFolder = getModsFolder();
        File disabledFile = new File(modsFolder, fileName + ".disabled");
        File enabledFile = new File(modsFolder, fileName);
        if (disabledFile.exists()) return disabledFile.renameTo(enabledFile);
        return enabledFile.exists();
    }

    private boolean disableMod(String fileName) {
        File modsFolder = getModsFolder();
        File enabledFile = new File(modsFolder, fileName);
        File disabledFile = new File(modsFolder, fileName + ".disabled");
        if (enabledFile.exists() && !fileName.toLowerCase().contains("voxeldash")) {
            return enabledFile.renameTo(disabledFile);
        }
        return disabledFile.exists();
    }

    private boolean deleteMod(String fileName) {
        File modsFolder = getModsFolder();
        File enabledFile = new File(modsFolder, fileName);
        File disabledFile = new File(modsFolder, fileName + ".disabled");
        if (enabledFile.exists()) return enabledFile.delete();
        if (disabledFile.exists()) return disabledFile.delete();
        return false;
    }

    private File getModsFolder() {
        return compat().modsFolder();
    }

    @Override
    public File getResourceFolder(ResourceType type) {
        File folder = null;
        if (type == ResourceType.MOD) {
            folder = getModsFolder();
        } else if (type == ResourceType.DATAPACK && compat().supportsDatapacks()) {
            folder = compat().datapacksFolder();
        }
        if (folder != null && !folder.exists()) folder.mkdirs();
        return folder;
    }

    @Override
    public boolean loadAndEnableResource(File file, ResourceType type) {
        if (type == ResourceType.MOD) {
            return true;
        }
        if (type == ResourceType.DATAPACK && compat().supportsDatapacks()) {
            compat().runCommand("datapack enable \"file/" + file.getName() + "\"");
            return true;
        }
        return false;
    }

    private List<Resource> getDatapacks() {
        List<Resource> resources = new ArrayList<>();
        File folder = compat().datapacksFolder();
        if (folder == null || !folder.exists() || !folder.isDirectory()) return resources;

        File[] entries = folder.listFiles();
        if (entries == null) return resources;
        for (File entry : entries) {
            Resource r = parseDatapack(entry);
            if (r != null) resources.add(r);
        }
        return resources;
    }

    private Resource parseDatapack(File entry) {
        boolean enabled = !entry.getName().endsWith(".disabled");
        String baseName = entry.getName().replace(".disabled", "");

        Map<String, Object> meta = null;
        if (entry.isDirectory()) {
            File packFile = new File(entry, "pack.mcmeta");
            if (packFile.exists()) meta = readJson(packFile);
        } else if (baseName.endsWith(".zip")) {
            meta = readJsonFromZip(entry, "pack.mcmeta");
        } else {
            return null;
        }

        String desc = null;
        if (meta != null && meta.get("pack") instanceof Map) {
            Object d = ((Map<?, ?>) meta.get("pack")).get("description");
            if (d != null) desc = d.toString();
        }

        return new Resource.Builder()
                .name(baseName.replace(".zip", ""))
                .fileName(baseName)
                .type(ResourceType.DATAPACK)
                .description(desc)
                .enabled(enabled)
                .fileSize(entry.isDirectory() ? folderSize(entry) : entry.length())
                .build();
    }

    private File findDatapackFile(String fileName) {
        File folder = compat().datapacksFolder();
        if (folder == null) return null;
        String clean = fileName.replace(".disabled", "");
        File enabled = new File(folder, clean);
        if (enabled.exists()) return enabled;
        File disabled = new File(folder, clean + ".disabled");
        return disabled.exists() ? disabled : null;
    }

    private boolean enableDatapack(String fileName) {
        File file = findDatapackFile(fileName);
        if (file == null || !file.getName().endsWith(".disabled")) return file != null;
        String newName = file.getName().replace(".disabled", "");
        boolean ok = file.renameTo(new File(file.getParentFile(), newName));
        if (ok) compat().runCommand("datapack enable \"file/" + newName + "\"");
        return ok;
    }

    private boolean disableDatapack(String fileName) {
        File file = findDatapackFile(fileName);
        if (file == null || file.getName().endsWith(".disabled")) return file != null;
        String name = file.getName();
        compat().runCommand("datapack disable \"file/" + name + "\"");
        return file.renameTo(new File(file.getParentFile(), name + ".disabled"));
    }

    private boolean deleteDatapack(String fileName) {
        File file = findDatapackFile(fileName);
        if (file == null) return false;
        if (!file.getName().endsWith(".disabled")) {
            compat().runCommand("datapack disable \"file/" + file.getName() + "\"");
        }
        return file.isDirectory() ? deleteDir(file) : file.delete();
    }

    @Override
    public List<ConfigFile> getConfigFiles(String fileName, ResourceType type) {
        if (type == ResourceType.MOD) {
            return getModConfigFiles(fileName);
        }
        if (type == ResourceType.DATAPACK && compat().supportsDatapacks()) {
            File datapackFile = findDatapackFile(fileName);
            if (datapackFile == null || !datapackFile.isDirectory()) return Collections.emptyList();
            return scanConfigFiles(datapackFile, datapackFile, MAX_CONFIG_FILES);
        }
        return Collections.emptyList();
    }

    private List<ConfigFile> getModConfigFiles(String fileName) {
        File jarFile = new File(getModsFolder(), fileName);
        if (!jarFile.exists()) jarFile = new File(getModsFolder(), fileName + ".disabled");

        FabricCompat.ModEntry meta = compat().parseJarMeta(jarFile);
        String modId = meta != null && meta.modId != null ? meta.modId
                : fileName.replace(".jar", "").replace(".disabled", "");

        File configFolder = compat().configDir();
        List<ConfigFile> files = new ArrayList<>();
        if (configFolder == null || !configFolder.isDirectory()) return files;

        final String modPrefix = modId.toLowerCase();
        File[] candidates = configFolder.listFiles((d, n) -> {
            String lower = n.toLowerCase();
            return isConfigFile(lower) && (lower.equals(modPrefix + ".toml") || lower.equals(modPrefix + ".cfg") || lower.startsWith(modPrefix + "-") || lower.startsWith(modPrefix + "."));
        });
        if (candidates != null) {
            Arrays.sort(candidates, Comparator.comparing(File::getName));
            for (File candidate : candidates) {
                if (files.size() >= MAX_CONFIG_FILES) break;
                files.add(new ConfigFile(candidate.getName(), candidate.getName(), candidate.length(), candidate));
            }
        }

        File modConfigFolder = new File(configFolder, modId);
        if (modConfigFolder.exists() && modConfigFolder.isDirectory()) {
            files.addAll(scanConfigFiles(modConfigFolder, modConfigFolder, MAX_CONFIG_FILES - files.size()));
        }
        return files;
    }

    private List<ConfigFile> scanConfigFiles(File baseFolder, File folder, int limit) {
        List<ConfigFile> files = new ArrayList<>();
        File[] entries = folder.listFiles();
        if (entries == null) return files;
        Arrays.sort(entries, Comparator.comparing(File::getName));
        for (File entry : entries) {
            if (files.size() >= limit) break;
            if (entry.getName().startsWith(".")) continue;
            if (entry.isDirectory()) {
                files.addAll(scanConfigFiles(baseFolder, entry, limit - files.size()));
            } else if (isConfigFile(entry.getName())) {
                String relativePath = baseFolder.toPath().relativize(entry.toPath()).toString();
                files.add(new ConfigFile(entry.getName(), relativePath, entry.length(), entry));
            }
        }
        return files;
    }

    private boolean isConfigFile(String name) {
        String lower = name.toLowerCase();
        for (String ext : CONFIG_EXTENSIONS) {
            if (lower.endsWith(ext)) return true;
        }
        return false;
    }

    private Map<String, Object> readJson(File file) {
        try (FileReader r = new FileReader(file)) {
            return GSON.fromJson(r, MAP_TYPE);
        } catch (Exception e) {
            return null;
        }
    }

    private Map<String, Object> readJsonFromZip(File zipFile, String entry) {
        try (ZipFile zip = new ZipFile(zipFile)) {
            java.util.zip.ZipEntry e = zip.getEntry(entry);
            if (e == null) return null;
            try (InputStreamReader r = new InputStreamReader(zip.getInputStream(e))) {
                return GSON.fromJson(r, MAP_TYPE);
            }
        } catch (Exception e) {
            return null;
        }
    }

    private long folderSize(File folder) {
        long size = 0;
        File[] files = folder.listFiles();
        if (files != null) for (File f : files) size += f.isFile() ? f.length() : folderSize(f);
        return size;
    }

    private boolean deleteDir(File dir) {
        File[] files = dir.listFiles();
        if (files != null) for (File f : files) {
            if (f.isDirectory()) deleteDir(f);
            else f.delete();
        }
        return dir.delete();
    }
}
