package de.gnm.voxeldash.api.store;

import com.fasterxml.jackson.databind.JsonNode;
import de.gnm.voxeldash.api.entities.ResourceType;

import java.io.File;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class CurseForgeProvider extends AbstractStoreProvider {

    private static final String API_BASE = "https://api.curseforge.com";
    private static final int MINECRAFT_GAME_ID = 432;

    private static final int CLASS_MODS = 6;
    private static final int CLASS_PLUGINS = 5;
    private static final int CLASS_DATAPACKS = 6945;

    private static final int LOADER_ANY = 0;
    private static final int LOADER_FORGE = 1;
    private static final int LOADER_FABRIC = 4;
    private static final int LOADER_QUILT = 5;
    private static final int LOADER_NEOFORGE = 6;

    /**
     * CurseForge API provider for Minecraft mods, plugins, and datapacks.
     * Requires an API key from https://console.curseforge.com/
     *
     * @see <a href="https://docs.curseforge.com/rest-api/">CurseForge API Documentation</a>
     */
    public CurseForgeProvider() {
        super();
    }

    @Override
    public String getId() {
        return "curseforge";
    }

    @Override
    public String getDisplayName() {
        return "CurseForge";
    }

    @Override
    public String getLogoPath() {
        return "/assets/images/curseforge-logo.png";
    }

    @Override
    public boolean requiresApiKey() {
        return true;
    }

    @Override
    protected String getApiKeyHeaderName() {
        return "x-api-key";
    }

    @Override
    public StoreSearchResult search(String query, ResourceType resourceType, String gameVersion,
                                    String loader, int page, int pageSize) {
        if (!isConfigured()) {
            return new StoreSearchResult(new StoreProject[0], 0, page, pageSize);
        }

        try {
            StringBuilder url = new StringBuilder(API_BASE + "/v1/mods/search?");
            url.append("gameId=").append(MINECRAFT_GAME_ID);

            int classId = mapResourceTypeToClassId(resourceType);
            if (classId > 0) {
                url.append("&classId=").append(classId);
            }

            if (query != null && !query.isEmpty()) {
                url.append("&searchFilter=").append(URLEncoder.encode(query, "UTF-8"));
            }

            if (gameVersion != null && !gameVersion.isEmpty()) {
                url.append("&gameVersion=").append(URLEncoder.encode(gameVersion, "UTF-8"));
            }

            if (loader != null && !loader.isEmpty() && resourceType != ResourceType.DATAPACK) {
                int modLoaderType = mapLoaderToModLoaderType(loader);
                if (modLoaderType > 0) {
                    url.append("&modLoaderType=").append(modLoaderType);
                }
            }

            url.append("&index=").append(page * pageSize);
            url.append("&pageSize=").append(Math.min(pageSize, 50));
            url.append("&sortField=2");
            url.append("&sortOrder=desc");

            JsonNode response = makeAuthenticatedRequest(url.toString());
            if (response == null || !response.has("data")) {
                return new StoreSearchResult(new StoreProject[0], 0, page, pageSize);
            }

            List<StoreProject> projects = new ArrayList<>();
            JsonNode data = response.get("data");
            for (JsonNode mod : data) {
                StoreProject project = parseProject(mod);
                if (project != null) {
                    projects.add(project);
                }
            }

            int totalHits = 0;
            if (response.has("pagination") && response.get("pagination").has("totalCount")) {
                totalHits = response.get("pagination").get("totalCount").asInt();
            }

            return new StoreSearchResult(
                    projects.toArray(new StoreProject[0]),
                    totalHits,
                    page,
                    pageSize
            );
        } catch (Exception e) {
            e.printStackTrace();
            return new StoreSearchResult(new StoreProject[0], 0, page, pageSize);
        }
    }

    @Override
    public StoreProject getProject(String projectId) {
        if (!isConfigured()) {
            return null;
        }

        try {
            String url = API_BASE + "/v1/mods/" + projectId;
            JsonNode response = makeAuthenticatedRequest(url);
            if (response == null || !response.has("data")) {
                return null;
            }
            return parseProject(response.get("data"));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public StoreVersion[] getVersions(String projectId, String gameVersion, String loader) {
        if (!isConfigured()) {
            return new StoreVersion[0];
        }

        try {
            StringBuilder url = new StringBuilder(API_BASE + "/v1/mods/" + projectId + "/files?");
            url.append("pageSize=20");

            if (gameVersion != null && !gameVersion.isEmpty()) {
                url.append("&gameVersion=").append(URLEncoder.encode(gameVersion, "UTF-8"));
            }

            if (loader != null && !loader.isEmpty()) {
                int modLoaderType = mapLoaderToModLoaderType(loader);
                if (modLoaderType > 0) {
                    url.append("&modLoaderType=").append(modLoaderType);
                }
            }

            JsonNode response = makeAuthenticatedRequest(url.toString());
            if (response == null || !response.has("data")) {
                return new StoreVersion[0];
            }

            List<StoreVersion> versions = new ArrayList<>();
            for (JsonNode file : response.get("data")) {
                StoreVersion version = parseVersion(file, projectId);
                if (version != null) {
                    versions.add(version);
                }
            }

            return versions.toArray(new StoreVersion[0]);
        } catch (Exception e) {
            e.printStackTrace();
            return new StoreVersion[0];
        }
    }

    @Override
    public StoreDownloadResult download(String projectId, String versionId) {
        if (!isConfigured()) {
            return StoreDownloadResult.failure("API key not configured");
        }

        try {
            String fileUrl = API_BASE + "/v1/mods/" + projectId + "/files/" + versionId;
            JsonNode response = makeAuthenticatedRequest(fileUrl);
            if (response == null || !response.has("data")) {
                return StoreDownloadResult.failure("File not found");
            }

            JsonNode fileData = response.get("data");
            String downloadUrl = getTextOrNull(fileData, "downloadUrl");
            String fileName = getTextOrNull(fileData, "fileName");

            if (downloadUrl == null || downloadUrl.isEmpty()) {
                return StoreDownloadResult.failure("Download not available for this mod");
            }

            String originalName = sanitizeFilename(fileName != null ? fileName : "resource_" + projectId);
            String extension = "";
            String baseName = originalName;

            int lastDot = originalName.lastIndexOf('.');
            if (lastDot > 0) {
                extension = originalName.substring(lastDot);
                baseName = originalName.substring(0, lastDot);
            }

            String trackedFilename = baseName + "_[curseforge_" + projectId + "]" + extension;

            File downloadedFile = new File(tempDownloadDir, trackedFilename);
            downloadFile(downloadUrl, downloadedFile);

            return StoreDownloadResult.success(downloadedFile, trackedFilename, projectId, versionId);
        } catch (Exception e) {
            e.printStackTrace();
            return StoreDownloadResult.failure("Download failed: " + e.getMessage());
        }
    }

    @Override
    public boolean supportsResourceType(ResourceType type) {
        return type == ResourceType.PLUGIN || type == ResourceType.MOD || type == ResourceType.DATAPACK;
    }

    @Override
    public String mapServerSoftwareToLoader(String serverSoftware) {
        if (serverSoftware == null) return null;
        switch (serverSoftware.toLowerCase()) {
            case "fabric": return "fabric";
            case "forge": return "forge";
            case "neoforge": return "neoforge";
            case "quilt": return "quilt";
            case "paper": case "spigot": case "bukkit": case "purpur": case "pufferfish": return "bukkit";
            default: return null;
        }
    }

    @Override
    public String mapResourceTypeToProjectType(ResourceType resourceType) {
        switch (resourceType) {
            case MOD: return "mod";
            case PLUGIN: return "bukkit-plugin";
            case DATAPACK: return "datapack";
            case EXTENSION: return "bukkit-plugin";
            default: return null;
        }
    }

    private int mapResourceTypeToClassId(ResourceType resourceType) {
        if (resourceType == null) return CLASS_MODS;
        switch (resourceType) {
            case MOD: return CLASS_MODS;
            case PLUGIN: case EXTENSION: return CLASS_PLUGINS;
            case DATAPACK: return CLASS_DATAPACKS;
            default: return CLASS_MODS;
        }
    }

    private int mapLoaderToModLoaderType(String loader) {
        if (loader == null) return LOADER_ANY;
        switch (loader.toLowerCase()) {
            case "forge": return LOADER_FORGE;
            case "fabric": return LOADER_FABRIC;
            case "quilt": return LOADER_QUILT;
            case "neoforge": return LOADER_NEOFORGE;
            default: return LOADER_ANY;
        }
    }

    private StoreProject parseProject(JsonNode node) {
        try {
            String id = String.valueOf(node.get("id").asInt());
            String slug = getTextOrNull(node, "slug");
            String name = getTextOrNull(node, "name");
            String summary = getTextOrNull(node, "summary");

            String author = null;
            if (node.has("authors") && node.get("authors").isArray() && node.get("authors").size() > 0) {
                author = getTextOrNull(node.get("authors").get(0), "name");
            }

            String iconUrl = null;
            if (node.has("logo") && !node.get("logo").isNull()) {
                iconUrl = getTextOrNull(node.get("logo"), "thumbnailUrl");
            }

            int downloads = node.has("downloadCount") ? node.get("downloadCount").asInt() : 0;

            List<String> gameVersionsList = new ArrayList<>();
            String latestVersion = null;
            if (node.has("latestFilesIndexes") && node.get("latestFilesIndexes").isArray()) {
                for (JsonNode index : node.get("latestFilesIndexes")) {
                    String gv = getTextOrNull(index, "gameVersion");
                    if (gv != null && !gameVersionsList.contains(gv)) {
                        gameVersionsList.add(gv);
                    }
                    if (latestVersion == null) {
                        latestVersion = getTextOrNull(index, "filename");
                    }
                }
            }
            String[] gameVersions = gameVersionsList.toArray(new String[0]);

            String projectType = "mod";
            if (node.has("classId")) {
                int classId = node.get("classId").asInt();
                if (classId == CLASS_PLUGINS) projectType = "bukkit-plugin";
                else if (classId == CLASS_DATAPACKS) projectType = "datapack";
                else projectType = "mod";
            }

            String dateCreated = getTextOrNull(node, "dateCreated");
            String dateModified = getTextOrNull(node, "dateModified");

            return new StoreProject(id, slug, name, summary, author, iconUrl, downloads, gameVersions, projectType,
                    dateCreated, dateModified, latestVersion);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private StoreVersion parseVersion(JsonNode node, String projectId) {
        try {
            String id = String.valueOf(node.get("id").asInt());
            String displayName = getTextOrNull(node, "displayName");
            String fileName = getTextOrNull(node, "fileName");
            int downloads = node.has("downloadCount") ? node.get("downloadCount").asInt() : 0;
            String datePublished = getTextOrNull(node, "fileDate");

            String[] gameVersions = new String[0];
            if (node.has("gameVersions") && node.get("gameVersions").isArray()) {
                gameVersions = parseStringArray(node.get("gameVersions"));
            }

            List<String> loadersList = new ArrayList<>();
            if (node.has("sortableGameVersions") && node.get("sortableGameVersions").isArray()) {
                for (JsonNode sgv : node.get("sortableGameVersions")) {
                    String gvName = getTextOrNull(sgv, "gameVersionName");
                    if (gvName != null) {
                        String lower = gvName.toLowerCase();
                        if (lower.contains("forge") && !loadersList.contains("forge")) {
                            loadersList.add("forge");
                        } else if (lower.contains("fabric") && !loadersList.contains("fabric")) {
                            loadersList.add("fabric");
                        } else if (lower.contains("quilt") && !loadersList.contains("quilt")) {
                            loadersList.add("quilt");
                        } else if (lower.contains("neoforge") && !loadersList.contains("neoforge")) {
                            loadersList.add("neoforge");
                        } else if (lower.contains("bukkit") && !loadersList.contains("bukkit")) {
                            loadersList.add("bukkit");
                        }
                    }
                }
            }
            String[] loaders = loadersList.toArray(new String[0]);

            String versionType = "release";
            if (node.has("releaseType")) {
                int releaseType = node.get("releaseType").asInt();
                switch (releaseType) {
                    case 2: versionType = "beta"; break;
                    case 3: versionType = "alpha"; break;
                    default: versionType = "release"; break;
                }
            }

            String downloadUrl = getTextOrNull(node, "downloadUrl");
            long fileSize = node.has("fileLength") ? node.get("fileLength").asLong() : 0;

            StoreFile[] files = new StoreFile[]{
                    new StoreFile(
                            downloadUrl != null ? downloadUrl : "",
                            fileName != null ? fileName : "file.jar",
                            true,
                            fileSize,
                            null
                    )
            };

            return new StoreVersion(
                    id,
                    projectId,
                    displayName != null ? displayName : fileName,
                    fileName,
                    null,
                    gameVersions,
                    loaders,
                    versionType,
                    downloads,
                    datePublished,
                    files
            );
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
