package de.gnm.voxeldash.pipes;

import de.gnm.voxeldash.VoxelDashBungee;
import de.gnm.voxeldash.api.entities.ConfigFile;
import de.gnm.voxeldash.api.entities.Resource;
import de.gnm.voxeldash.api.entities.ResourceType;
import de.gnm.voxeldash.api.pipes.resources.ResourcePipe;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.plugin.PluginDescription;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;

public class ResourcePipeImpl implements ResourcePipe {

    private static final List<ResourceType> SUPPORTED_TYPES = Collections.singletonList(ResourceType.PLUGIN);
    private static final int MAX_CONFIG_FILES = 10;
    private static final String[] CONFIG_EXTENSIONS = {".yml", ".yaml", ".json"};

    @Override
    public List<ResourceType> getSupportedResourceTypes() {
        return SUPPORTED_TYPES;
    }

    @Override
    public List<Resource> getResources(ResourceType type) {
        if (type != ResourceType.PLUGIN) {
            return new ArrayList<>();
        }
        List<Resource> resources = getPlugins();
        resources.sort(Comparator.comparing(Resource::getName, String.CASE_INSENSITIVE_ORDER));
        return resources;
    }

    @Override
    public Resource getResource(String fileName, ResourceType type) {
        return getResources(type).stream()
                .filter(r -> r.getFileName().equals(fileName))
                .findFirst()
                .orElse(null);
    }

    @Override
    public boolean enableResource(String fileName, ResourceType type) {
        if (type != ResourceType.PLUGIN) {
            return false;
        }
        File file = findPluginFile(fileName);
        if (file != null && file.getName().endsWith(".disabled")) {
            String newName = file.getName().replace(".disabled", "");
            return file.renameTo(new File(file.getParentFile(), newName));
        }
        return file != null;
    }

    @Override
    public boolean disableResource(String fileName, ResourceType type) {
        if (type != ResourceType.PLUGIN) {
            return false;
        }
        File file = findPluginFile(fileName);
        if (file != null && !file.getName().endsWith(".disabled")) {
            return file.renameTo(new File(file.getParentFile(), file.getName() + ".disabled"));
        }
        return file != null;
    }

    @Override
    public boolean deleteResource(String fileName, ResourceType type) {
        if (type != ResourceType.PLUGIN) {
            return false;
        }
        File file = findPluginFile(fileName);
        if (file == null) {
            return false;
        }
        return file.delete();
    }

    @Override
    public boolean loadAndEnableResource(File file, ResourceType type) {
        if (type != ResourceType.PLUGIN) {
            return false;
        }
        return file.exists();
    }

    @Override
    public List<ConfigFile> getConfigFiles(String fileName, ResourceType type) {
        if (type != ResourceType.PLUGIN) {
            return Collections.emptyList();
        }
        return getPluginConfigFiles(fileName);
    }

    @Override
    public File getResourceFolder(ResourceType type) {
        if (type == ResourceType.PLUGIN) {
            File folder = getPluginsFolder();
            if (!folder.exists()) {
                folder.mkdirs();
            }
            return folder;
        }
        return null;
    }

    private List<Resource> getPlugins() {
        List<Resource> resources = new ArrayList<>();

        Map<String, Plugin> loadedPluginsByName = new HashMap<>();
        for (Plugin plugin : ProxyServer.getInstance().getPluginManager().getPlugins()) {
            if (plugin.getDescription().getName().equals("VoxelDash")) {
                continue;
            }
            loadedPluginsByName.put(plugin.getDescription().getName().toLowerCase(), plugin);
        }

        File[] jarFiles = getPluginsFolder().listFiles((d, n) -> n.endsWith(".jar"));
        if (jarFiles != null) {
            for (File jarFile : jarFiles) {
                if (jarFile.getName().toLowerCase().contains("voxeldash")) {
                    continue;
                }

                PluginInfo info = getPluginInfo(jarFile);
                String pluginName = info != null ? info.name : jarFile.getName().replace(".jar", "");

                Plugin loadedPlugin = loadedPluginsByName.get(pluginName.toLowerCase());
                boolean isEnabled = loadedPlugin != null;

                if (loadedPlugin != null) {
                    PluginDescription desc = loadedPlugin.getDescription();
                    resources.add(new Resource.Builder()
                            .name(desc.getName())
                            .fileName(jarFile.getName())
                            .type(ResourceType.PLUGIN)
                            .version(desc.getVersion())
                            .description(desc.getDescription())
                            .authors(new String[]{desc.getAuthor()})
                            .enabled(isEnabled)
                            .fileSize(jarFile.length())
                            .build());
                } else if (info != null) {
                    resources.add(new Resource.Builder()
                            .name(info.name)
                            .fileName(jarFile.getName())
                            .type(ResourceType.PLUGIN)
                            .version(info.version)
                            .description(info.description)
                            .authors(info.author != null ? new String[]{info.author} : new String[0])
                            .enabled(false)
                            .fileSize(jarFile.length())
                            .build());
                } else {
                    resources.add(new Resource.Builder()
                            .name(jarFile.getName().replace(".jar", ""))
                            .fileName(jarFile.getName())
                            .type(ResourceType.PLUGIN)
                            .enabled(false)
                            .fileSize(jarFile.length())
                            .build());
                }
            }
        }

        return resources;
    }

    private File findPluginFile(String fileName) {
        File folder = getPluginsFolder();
        File file = new File(folder, fileName);
        if (file.exists()) {
            return file;
        }
        File disabled = new File(folder, fileName + ".disabled");
        if (disabled.exists()) {
            return disabled;
        }
        String baseName = fileName.replace(".disabled", "");
        File base = new File(folder, baseName);
        return base.exists() ? base : null;
    }

    private File getPluginsFolder() {
        return ProxyServer.getInstance().getPluginsFolder();
    }

    private List<ConfigFile> getPluginConfigFiles(String fileName) {
        String pluginName = getPluginName(fileName);
        File pluginFolder = new File(getPluginsFolder(), pluginName);

        if (!pluginFolder.exists() || !pluginFolder.isDirectory()) {
            return Collections.emptyList();
        }

        return scanConfigFiles(pluginFolder, pluginFolder, MAX_CONFIG_FILES);
    }

    private List<ConfigFile> scanConfigFiles(File baseFolder, File folder, int limit) {
        List<ConfigFile> files = new ArrayList<>();
        File[] entries = folder.listFiles();
        if (entries == null) {
            return files;
        }

        Arrays.sort(entries, Comparator.comparing(File::getName));

        for (File entry : entries) {
            if (files.size() >= limit) {
                break;
            }
            if (entry.getName().startsWith(".")) {
                continue;
            }

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
            if (lower.endsWith(ext)) {
                return true;
            }
        }
        return false;
    }

    private String getPluginName(String fileName) {
        PluginInfo info = getPluginInfo(findPluginFile(fileName));
        if (info != null) {
            return info.name;
        }
        return fileName.replace(".jar", "");
    }

    private PluginInfo getPluginInfo(File file) {
        if (file == null || !file.exists()) {
            return null;
        }
        try (JarFile jar = new JarFile(file)) {
            JarEntry entry = jar.getJarEntry("bungee.yml");
            if (entry == null) {
                entry = jar.getJarEntry("plugin.yml");
            }
            if (entry != null) {
                try (InputStream is = jar.getInputStream(entry)) {
                    Yaml yaml = new Yaml();
                    Map<String, Object> data = yaml.load(is);
                    if (data != null) {
                        PluginInfo info = new PluginInfo();
                        info.name = (String) data.get("name");
                        info.version = data.get("version") != null ? data.get("version").toString() : null;
                        info.description = (String) data.get("description");
                        info.author = (String) data.get("author");
                        return info;
                    }
                }
            }
        } catch (Exception e) {
            VoxelDashBungee.getInstance().getLogger().log(Level.WARNING,
                    "Failed to read plugin info from: " + file.getName(), e);
        }
        return null;
    }

    private static class PluginInfo {
        String name;
        String version;
        String description;
        String author;
    }
}
