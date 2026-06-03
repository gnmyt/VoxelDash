package de.gnm.voxeldash.api.store;

import com.fasterxml.jackson.databind.JsonNode;
import de.gnm.voxeldash.api.entities.ResourceType;

import java.io.File;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class SpigotProvider extends AbstractStoreProvider {

    private static final String API_BASE = "https://api.spiget.org/v2";

    /**
     * Spiget API provider for SpigotMC resources.
     *
     * @see <a href="https://spiget.org/documentation">Spiget API Documentation</a>
     */
    public SpigotProvider() {
        super();
    }

    @Override
    public String getId() {
        return "spigot";
    }

    @Override
    public String getDisplayName() {
        return "Spigot";
    }

    @Override
    public String getLogoPath() {
        return "/assets/images/spigot-logo.png";
    }

    @Override
    public StoreSearchResult search(String query, ResourceType resourceType, String gameVersion,
                                    String loader, int page, int pageSize) {
        try {
            if (resourceType != null && resourceType != ResourceType.PLUGIN) {
                return new StoreSearchResult(new StoreProject[0], 0, page, pageSize);
            }

            int requestSize = Math.min(pageSize * 3, 100);

            StringBuilder url = new StringBuilder();

            if (query != null && !query.isEmpty()) {
                url.append(API_BASE).append("/search/resources/")
                        .append(URLEncoder.encode(query, "UTF-8")).append("?");
            } else {
                url.append(API_BASE).append("/resources/free?");
            }

            url.append("size=").append(requestSize).append("&");
            url.append("page=").append(page + 1);
            url.append("&sort=-downloads");

            JsonNode response = makeRequest(url.toString());
            if (response == null || !response.isArray()) {
                return new StoreSearchResult(new StoreProject[0], 0, page, pageSize);
            }

            List<StoreProject> projects = new ArrayList<>();
            for (JsonNode node : response) {
                if (node.has("premium") && node.get("premium").asBoolean()) {
                    continue;
                }
                if (node.has("external") && node.get("external").asBoolean()) {
                    continue;
                }

                StoreProject project = parseProject(node);
                if (project != null) {
                    projects.add(project);
                    if (projects.size() >= pageSize) {
                        break;
                    }
                }
            }

            int totalHits = projects.size() < pageSize ? (page * pageSize) + projects.size() : 10000;

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
        try {
            JsonNode response = makeRequest(API_BASE + "/resources/" + projectId);
            if (response == null) return null;
            return parseProject(response);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public StoreVersion[] getVersions(String projectId, String gameVersion, String loader) {
        try {
            String url = API_BASE + "/resources/" + projectId + "/versions?size=20&sort=-releaseDate";

            JsonNode response = makeRequest(url);
            if (response == null || !response.isArray()) {
                return new StoreVersion[0];
            }

            List<StoreVersion> versions = new ArrayList<>();
            for (JsonNode versionNode : response) {
                versions.add(parseVersion(versionNode, projectId));
            }

            return versions.toArray(new StoreVersion[0]);
        } catch (Exception e) {
            e.printStackTrace();
            return new StoreVersion[0];
        }
    }

    @Override
    public StoreDownloadResult download(String projectId, String versionId) {
        try {
            JsonNode resourceNode = makeRequest(API_BASE + "/resources/" + projectId);
            if (resourceNode == null) {
                return StoreDownloadResult.failure("Resource not found");
            }

            String resourceName = resourceNode.has("name") ? resourceNode.get("name").asText() : projectId;
            String fileType = ".jar";
            if (resourceNode.has("file") && resourceNode.get("file").has("type")) {
                fileType = resourceNode.get("file").get("type").asText();
                if (!fileType.startsWith(".")) {
                    fileType = "." + fileType;
                }
            }

            String downloadUrl = API_BASE + "/resources/" + projectId + "/download";

            String baseName = resourceName.replaceAll("[^a-zA-Z0-9.-]", "_");
            String trackedFilename = baseName + "_[spigot_" + projectId + "]" + fileType;

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
        return type == ResourceType.PLUGIN;
    }

    @Override
    public String mapServerSoftwareToLoader(String serverSoftware) {
        return null;
    }

    @Override
    public String mapResourceTypeToProjectType(ResourceType resourceType) {
        return resourceType == ResourceType.PLUGIN ? "plugin" : null;
    }

    private StoreProject parseProject(JsonNode node) {
        try {
            String id = node.has("id") ? String.valueOf(node.get("id").asInt()) : null;
            if (id == null) return null;

            String name = getTextOrNull(node, "name");
            String description = getTextOrNull(node, "tag");

            String author = null;
            if (node.has("author")) {
                JsonNode authorNode = node.get("author");
                if (authorNode.has("name")) {
                    author = authorNode.get("name").asText();
                } else if (authorNode.has("id")) {
                    author = "Author #" + authorNode.get("id").asInt();
                }
            }

            String iconUrl = null;
            if (node.has("icon") && node.get("icon").has("url")) {
                String iconPath = node.get("icon").get("url").asText();
                if (iconPath != null && !iconPath.isEmpty()) {
                    int queryIndex = iconPath.indexOf('?');
                    if (queryIndex > 0) {
                        iconPath = iconPath.substring(0, queryIndex);
                    }
                    iconUrl = "https://www.spigotmc.org/" + iconPath;
                }
            }

            int downloads = node.has("downloads") ? node.get("downloads").asInt() : 0;

            String[] gameVersions = parseStringArray(node.get("testedVersions"));

            String dateCreated = null;
            String dateModified = null;
            if (node.has("releaseDate")) {
                long releaseDate = node.get("releaseDate").asLong();
                dateCreated = Instant.ofEpochSecond(releaseDate).toString();
            }
            if (node.has("updateDate")) {
                long updateDate = node.get("updateDate").asLong();
                dateModified = Instant.ofEpochSecond(updateDate).toString();
            }

            return new StoreProject(id, id, name, description, author, iconUrl, downloads, gameVersions, "plugin",
                    dateCreated, dateModified, null);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private StoreVersion parseVersion(JsonNode node, String projectId) {
        String id = node.has("id") ? String.valueOf(node.get("id").asInt()) : null;
        String name = getTextOrNull(node, "name");

        int downloads = node.has("downloads") ? node.get("downloads").asInt() : 0;

        String datePublished = null;
        if (node.has("releaseDate")) {
            long releaseDate = node.get("releaseDate").asLong();
            datePublished = Instant.ofEpochSecond(releaseDate).toString();
        }

        StoreFile[] files = new StoreFile[]{new StoreFile(API_BASE + "/resources/" + projectId + "/download", "plugin.jar", true, 0, null)};

        return new StoreVersion(
                id,
                projectId,
                name != null ? name : "Version " + id,
                name,
                null,
                new String[0],
                new String[]{"spigot", "paper", "bukkit"},
                "release",
                downloads,
                datePublished,
                files
        );
    }
}
