package de.gnm.voxeldash.api.store;

import com.fasterxml.jackson.databind.JsonNode;
import de.gnm.voxeldash.api.entities.ResourceType;

import java.io.File;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;


public class ModrinthProvider extends AbstractStoreProvider {
    
    private static final String API_BASE = "https://api.modrinth.com/v2";

    /**
     * Modrinth store provider implementation.
     *
     * @see <a href="https://docs.modrinth.com/api/">Modrinth API Documentation</a>
     */
    public ModrinthProvider() {
        super();
    }
    
    @Override
    public String getId() {
        return "modrinth";
    }
    
    @Override
    public String getDisplayName() {
        return "Modrinth";
    }
    
    @Override
    public String getLogoPath() {
        return "/assets/images/modrinth-logo.webp";
    }
    
    @Override
    public StoreSearchResult search(String query, ResourceType resourceType, String gameVersion,
                                    String loader, int page, int pageSize) {
        try {
            StringBuilder url = new StringBuilder(API_BASE + "/search?");

            if (query != null && !query.isEmpty()) {
                url.append("query=").append(URLEncoder.encode(query, "UTF-8")).append("&");
            }
            
            List<String> facetGroups = new ArrayList<>();
            
            String projectType = mapResourceTypeToProjectType(resourceType);
            if (projectType != null) {
                facetGroups.add("[\"project_type:" + projectType + "\"]");
            }
            
            if (gameVersion != null && !gameVersion.isEmpty()) {
                facetGroups.add("[\"versions:" + gameVersion + "\"]");
            }
            
            if (loader != null && !loader.isEmpty() && resourceType != ResourceType.DATAPACK) {
                facetGroups.add("[\"categories:" + loader + "\"]");
            }

            if (resourceType != ResourceType.DATAPACK) {
                facetGroups.add("[\"server_side:required\",\"server_side:optional\"]");
            }

            if (!facetGroups.isEmpty()) {
                String facetsJson = "[" + String.join(",", facetGroups) + "]";
                url.append("facets=").append(URLEncoder.encode(facetsJson, "UTF-8")).append("&");
            }

            url.append("offset=").append(page * pageSize).append("&");
            url.append("limit=").append(pageSize);
            
            JsonNode response = makeRequest(url.toString());
            if (response == null) {
                return new StoreSearchResult(new StoreProject[0], 0, page, pageSize);
            }
            
            JsonNode hits = response.get("hits");
            int totalHits = response.has("total_hits") ? response.get("total_hits").asInt() : 0;
            
            List<StoreProject> projects = new ArrayList<>();
            if (hits != null && hits.isArray()) {
                for (JsonNode hit : hits) {
                    projects.add(parseProject(hit));
                }
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
        try {
            JsonNode response = makeRequest(API_BASE + "/project/" + projectId);
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
            StringBuilder url = new StringBuilder(API_BASE + "/project/" + projectId + "/version?");
            
            if (gameVersion != null && !gameVersion.isEmpty()) {
                String gameVersionsParam = "[\"" + gameVersion + "\"]";
                url.append("game_versions=").append(URLEncoder.encode(gameVersionsParam, "UTF-8")).append("&");
            }
            
            if (loader != null && !loader.isEmpty()) {
                String loadersParam = "[\"" + loader + "\"]";
                url.append("loaders=").append(URLEncoder.encode(loadersParam, "UTF-8"));
            }
            
            JsonNode response = makeRequest(url.toString());
            if (response == null || !response.isArray()) {
                return new StoreVersion[0];
            }
            
            List<StoreVersion> versions = new ArrayList<>();
            for (JsonNode versionNode : response) {
                versions.add(parseVersion(versionNode));
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
            JsonNode versionNode = makeRequest(API_BASE + "/version/" + versionId);
            if (versionNode == null) {
                return StoreDownloadResult.failure("Version not found");
            }
            
            StoreVersion version = parseVersion(versionNode);
            StoreFile primaryFile = version.getPrimaryFile();
            
            if (primaryFile == null) {
                return StoreDownloadResult.failure("No downloadable file found");
            }
            
            String originalName = primaryFile.getFilename();
            String extension = "";
            String baseName = originalName;
            
            int lastDot = originalName.lastIndexOf('.');
            if (lastDot > 0) {
                extension = originalName.substring(lastDot);
                baseName = originalName.substring(0, lastDot);
            }

            String trackedFilename = baseName + "_[modrinth_" + projectId + "]" + extension;

            File downloadedFile = new File(tempDownloadDir, trackedFilename);
            downloadFile(primaryFile.getUrl(), downloadedFile);
            
            return StoreDownloadResult.success(downloadedFile, trackedFilename, projectId, versionId);
        } catch (Exception e) {
            e.printStackTrace();
            return StoreDownloadResult.failure("Download failed: " + e.getMessage());
        }
    }
    
    @Override
    public boolean supportsResourceType(ResourceType type) {
        return type == ResourceType.PLUGIN || 
               type == ResourceType.MOD || 
               type == ResourceType.DATAPACK;
    }
    
    @Override
    public String mapServerSoftwareToLoader(String serverSoftware) {
        if (serverSoftware == null) return null;
        
        String lower = serverSoftware.toLowerCase();

        if (lower.contains("paper") || lower.contains("spigot") || lower.contains("bukkit") ||
            lower.contains("purpur") || lower.contains("pufferfish")) {
            return "paper";
        }
        
        if (lower.contains("fabric")) {
            return "fabric";
        }
        
        if (lower.contains("forge")) {
            return "forge";
        }
        
        if (lower.contains("neoforge")) {
            return "neoforge";
        }

        if (lower.contains("quilt")) {
            return "quilt";
        }

        if (lower.contains("bungee") || lower.contains("waterfall")) {
            return "bungeecord";
        }

        if (lower.contains("velocity")) {
            return "velocity";
        }

        if (lower.contains("folia")) {
            return "folia";
        }

        if (lower.contains("sponge")) {
            return "sponge";
        }
        
        return null;
    }
    
    @Override
    public String mapResourceTypeToProjectType(ResourceType resourceType) {
        if (resourceType == null) return null;
        
        switch (resourceType) {
            case PLUGIN:
            case EXTENSION:
                return "plugin";
            case MOD:
                return "mod";
            case DATAPACK:
                return "datapack";
            default:
                return null;
        }
    }
    
    private StoreProject parseProject(JsonNode node) {
        String id = getTextOrNull(node, "project_id");
        if (id == null) id = getTextOrNull(node, "id");
        
        String author = getTextOrNull(node, "author");
        if (author == null && node.has("team")) {
            author = getTextOrNull(node, "team");
        }
        
        String[] gameVersions = parseStringArray(node.get("versions"));
        if (gameVersions.length == 0) {
            gameVersions = parseStringArray(node.get("game_versions"));
        }
        
        String latestVersion = null;
        if (node.has("latest_version")) {
            latestVersion = getTextOrNull(node, "latest_version");
        }
        
        return new StoreProject(
            id,
            getTextOrNull(node, "slug"),
            getTextOrNull(node, "title"),
            getTextOrNull(node, "description"),
            author,
            getTextOrNull(node, "icon_url"),
            node.has("downloads") ? node.get("downloads").asInt() : 0,
            gameVersions,
            getTextOrNull(node, "project_type"),
            getTextOrNull(node, "date_created"),
            getTextOrNull(node, "date_modified"),
            latestVersion
        );
    }
    
    private StoreVersion parseVersion(JsonNode node) {
        String[] gameVersions = parseStringArray(node.get("game_versions"));
        String[] loaders = parseStringArray(node.get("loaders"));

        List<StoreFile> files = new ArrayList<>();
        JsonNode filesNode = node.get("files");
        if (filesNode != null && filesNode.isArray()) {
            for (JsonNode fileNode : filesNode) {
                String sha512 = null;
                if (fileNode.has("hashes") && fileNode.get("hashes").has("sha512")) {
                    sha512 = fileNode.get("hashes").get("sha512").asText();
                }
                
                files.add(new StoreFile(
                    getTextOrNull(fileNode, "url"),
                    getTextOrNull(fileNode, "filename"),
                    fileNode.has("primary") && fileNode.get("primary").asBoolean(),
                    fileNode.has("size") ? fileNode.get("size").asLong() : 0,
                    sha512
                ));
            }
        }
        
        return new StoreVersion(
            getTextOrNull(node, "id"),
            getTextOrNull(node, "project_id"),
            getTextOrNull(node, "name"),
            getTextOrNull(node, "version_number"),
            getTextOrNull(node, "changelog"),
            gameVersions,
            loaders,
            getTextOrNull(node, "version_type"),
            node.has("downloads") ? node.get("downloads").asInt() : 0,
            getTextOrNull(node, "date_published"),
            files.toArray(new StoreFile[0])
        );
    }
}
