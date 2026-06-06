package de.gnm.voxeldash.api.routes.resources;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.gnm.voxeldash.api.annotations.ApiDoc;
import de.gnm.voxeldash.api.annotations.ApiField;
import de.gnm.voxeldash.api.annotations.AuthenticatedRoute;
import de.gnm.voxeldash.api.annotations.Method;
import de.gnm.voxeldash.api.annotations.Path;
import de.gnm.voxeldash.api.annotations.RequiresFeatures;
import de.gnm.voxeldash.api.controller.ApiKeyController;
import de.gnm.voxeldash.api.entities.Feature;
import de.gnm.voxeldash.api.entities.PermissionLevel;
import de.gnm.voxeldash.api.entities.ResourceType;
import de.gnm.voxeldash.api.http.JSONRequest;
import de.gnm.voxeldash.api.http.JSONResponse;
import de.gnm.voxeldash.api.pipes.ServerInfoPipe;
import de.gnm.voxeldash.api.pipes.resources.ResourcePipe;
import de.gnm.voxeldash.api.routes.BaseRoute;
import de.gnm.voxeldash.api.store.*;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static de.gnm.voxeldash.api.http.HTTPMethod.*;

public class StoreRouter extends BaseRoute {

    @ApiDoc(summary = "List store providers", description = "Returns all registered store providers, optionally filtered to those supporting a given resource type. Each provider includes its id, display name, logo, API key requirements and supported resource types.", tag = "Resource Store")
    @ApiField(name = "type", required = false, description = "Optional resource type identifier to filter providers by supported type")
    @AuthenticatedRoute
    @RequiresFeatures(Feature.Resources)
    @Path("/store/providers")
    @Method(GET)
    public JSONResponse getProviders(JSONRequest request) {
        String typeId = request.has("type") ? request.get("type") : null;
        ResourceType filterType = typeId != null ? ResourceType.fromIdentifier(typeId) : null;
        
        List<StoreProvider> providers = StoreProviderRegistry.getInstance().getAllProviders();
        ArrayNode array = getMapper().createArrayNode();

        for (StoreProvider provider : providers) {
            if (filterType != null && !provider.supportsResourceType(filterType)) {
                continue;
            }
            
            ObjectNode node = getMapper().createObjectNode();
            node.put("id", provider.getId());
            node.put("name", provider.getDisplayName());
            node.put("logoPath", provider.getLogoPath());
            node.put("requiresApiKey", provider.requiresApiKey());
            node.put("isConfigured", provider.isConfigured());
            
            ArrayNode supportedTypes = getMapper().createArrayNode();
            for (ResourceType type : ResourceType.values()) {
                if (provider.supportsResourceType(type)) {
                    supportedTypes.add(type.getIdentifier());
                }
            }
            node.set("supportedTypes", supportedTypes);
            
            array.add(node);
        }

        return new JSONResponse().add("providers", array);
    }

    @ApiDoc(summary = "Get API key status for a provider", description = "Returns whether the given store provider requires an API key and whether one is currently configured.", tag = "Resource Store")
    @ApiField(name = "provider", description = "Provider id to check the API key status for")
    @AuthenticatedRoute
    @RequiresFeatures(Feature.Resources)
    @Path("/store/apikey")
    @Method(GET)
    public JSONResponse getApiKeyStatus(JSONRequest request) {
        String providerId = request.has("provider") ? request.get("provider") : null;
        if (providerId == null) {
            return new JSONResponse().error("Provider ID required");
        }
        
        StoreProvider provider = StoreProviderRegistry.getInstance().getProvider(providerId);
        if (provider == null) {
            return new JSONResponse().error("Invalid provider: " + providerId);
        }
        
        return new JSONResponse()
            .add("requiresApiKey", provider.requiresApiKey())
            .add("isConfigured", provider.isConfigured());
    }

    @ApiDoc(summary = "Set API key for a provider", description = "Stores the given API key for a store provider that requires authentication.", tag = "Resource Store")
    @ApiField(name = "provider", description = "Provider id to set the API key for")
    @ApiField(name = "apiKey", description = "The API key to store for the provider")
    @AuthenticatedRoute
    @RequiresFeatures(Feature.Resources)
    @Path("/store/apikey")
    @Method(POST)
    public JSONResponse setApiKey(JSONRequest request) {
        String providerId = request.has("provider") ? request.get("provider") : null;
        String apiKey = request.has("apiKey") ? request.get("apiKey") : null;
        
        if (providerId == null) {
            return new JSONResponse().error("Provider ID required");
        }
        
        StoreProvider provider = StoreProviderRegistry.getInstance().getProvider(providerId);
        if (provider == null) {
            return new JSONResponse().error("Invalid provider: " + providerId);
        }
        
        if (!provider.requiresApiKey()) {
            return new JSONResponse().error("Provider does not require an API key");
        }
        
        ApiKeyController apiKeyController = getController(ApiKeyController.class);
        apiKeyController.setApiKey(providerId, apiKey);
        
        return new JSONResponse()
            .add("success", true)
            .add("isConfigured", provider.isConfigured());
    }

    @ApiDoc(summary = "Search the store", description = "Searches a store provider for projects of a given resource type, returning paginated results. Game version and loader default to the connected server's values but can be overridden.", tag = "Resource Store")
    @ApiField(name = "provider", required = false, description = "Provider id to search (defaults to 'modrinth')")
    @ApiField(name = "type", description = "Resource type identifier to search for")
    @ApiField(name = "query", required = false, description = "Search query string")
    @ApiField(name = "page", required = false, description = "Zero-based page index (defaults to 0)")
    @ApiField(name = "pageSize", required = false, description = "Results per page, capped at 100 (defaults to 20)")
    @ApiField(name = "gameVersion", required = false, description = "Override the Minecraft game version filter")
    @ApiField(name = "loader", required = false, description = "Override the mod/plugin loader filter")
    @AuthenticatedRoute
    @RequiresFeatures(Feature.Resources)
    @Path("/store/search")
    @Method(GET)
    public JSONResponse search(JSONRequest request) {
        String providerId = request.has("provider") ? request.get("provider") : "modrinth";
        StoreProvider provider = StoreProviderRegistry.getInstance().getProvider(providerId);
        if (provider == null) {
            return new JSONResponse().error("Invalid provider: " + providerId);
        }

        String typeId = request.has("type") ? request.get("type") : null;
        ResourceType resourceType = typeId != null ? ResourceType.fromIdentifier(typeId) : null;
        if (resourceType == null) {
            return new JSONResponse().error("Invalid or missing resource type");
        }

        if (!provider.supportsResourceType(resourceType)) {
            return new JSONResponse().error("Provider does not support resource type: " + typeId);
        }

        String query = request.has("query") ? request.get("query") : "";
        int page = request.has("page") ? Integer.parseInt(request.get("page")) : 0;
        int pageSize = request.has("pageSize") ? Integer.parseInt(request.get("pageSize")) : 20;

        if (pageSize > 100) pageSize = 100;
        if (pageSize < 1) pageSize = 20;

        String gameVersion = getGameVersion();
        String loader = getLoader(provider);

        if (request.has("gameVersion")) {
            gameVersion = request.get("gameVersion");
        }
        if (request.has("loader")) {
            loader = request.get("loader");
        }

        StoreSearchResult result = provider.search(query, resourceType, gameVersion, loader, page, pageSize);

        ObjectNode response = getMapper().createObjectNode();
        response.put("totalHits", result.getTotalHits());
        response.put("page", result.getPage());
        response.put("pageSize", result.getPageSize());
        response.put("totalPages", result.getTotalPages());
        response.put("gameVersion", gameVersion);
        response.put("loader", loader);

        ArrayNode projectsArray = getMapper().createArrayNode();
        for (StoreProject project : result.getProjects()) {
            projectsArray.add(projectToJson(project));
        }
        response.set("projects", projectsArray);

        return new JSONResponse().add("result", response);
    }

    @ApiDoc(summary = "Get a store project", description = "Returns the details of a single project from a store provider.", tag = "Resource Store")
    @ApiField(name = "provider", required = false, description = "Provider id to query (defaults to 'modrinth')")
    @ApiField(name = "projectId", description = "Identifier of the project to fetch")
    @AuthenticatedRoute
    @RequiresFeatures(Feature.Resources)
    @Path("/store/project")
    @Method(GET)
    public JSONResponse getProject(JSONRequest request) {
        String providerId = request.has("provider") ? request.get("provider") : "modrinth";
        StoreProvider provider = StoreProviderRegistry.getInstance().getProvider(providerId);
        if (provider == null) {
            return new JSONResponse().error("Invalid provider: " + providerId);
        }

        String projectId = request.has("projectId") ? request.get("projectId") : null;
        if (projectId == null || projectId.isEmpty()) {
            return new JSONResponse().error("Missing projectId");
        }

        StoreProject project = provider.getProject(projectId);
        if (project == null) {
            return new JSONResponse().error("Project not found");
        }

        return new JSONResponse().add("project", projectToJson(project));
    }

    @ApiDoc(summary = "List project versions", description = "Returns the available versions of a store project, filtered by game version and loader. Datapacks use the 'datapack' loader by default.", tag = "Resource Store")
    @ApiField(name = "provider", required = false, description = "Provider id to query (defaults to 'modrinth')")
    @ApiField(name = "projectId", description = "Identifier of the project to list versions for")
    @ApiField(name = "type", required = false, description = "Resource type identifier (affects the default loader)")
    @ApiField(name = "gameVersion", required = false, description = "Override the Minecraft game version filter")
    @ApiField(name = "loader", required = false, description = "Override the mod/plugin loader filter")
    @AuthenticatedRoute
    @RequiresFeatures(Feature.Resources)
    @Path("/store/versions")
    @Method(GET)
    public JSONResponse getVersions(JSONRequest request) {
        String providerId = request.has("provider") ? request.get("provider") : "modrinth";
        StoreProvider provider = StoreProviderRegistry.getInstance().getProvider(providerId);
        if (provider == null) {
            return new JSONResponse().error("Invalid provider: " + providerId);
        }

        String projectId = request.has("projectId") ? request.get("projectId") : null;
        if (projectId == null || projectId.isEmpty()) {
            return new JSONResponse().error("Missing projectId");
        }

        ResourceType resourceType = null;
        if (request.has("type")) {
            resourceType = ResourceType.fromIdentifier(request.get("type"));
        }

        String gameVersion = getGameVersion();
        String loader;
        
        if (resourceType == ResourceType.DATAPACK) {
            loader = "datapack";
        } else {
            loader = getLoader(provider);
        }

        if (request.has("gameVersion")) {
            gameVersion = request.get("gameVersion");
        }
        if (request.has("loader")) {
            loader = request.get("loader");
        }

        StoreVersion[] versions = provider.getVersions(projectId, gameVersion, loader);

        ArrayNode versionsArray = getMapper().createArrayNode();
        for (StoreVersion version : versions) {
            versionsArray.add(versionToJson(version));
        }

        return new JSONResponse()
                .add("versions", versionsArray)
                .add("gameVersion", gameVersion)
                .add("loader", loader);
    }

    @ApiDoc(summary = "Install a resource from the store", description = "Downloads the given project version from a store provider and installs it into the matching resource folder, enabling it when possible.", tag = "Resource Store")
    @ApiField(name = "type", description = "Resource type identifier of the resource to install")
    @ApiField(name = "projectId", description = "Identifier of the project to install")
    @ApiField(name = "versionId", description = "Identifier of the project version to install")
    @ApiField(name = "provider", required = false, description = "Provider id to download from (defaults to 'modrinth')")
    @AuthenticatedRoute
    @RequiresFeatures(value = Feature.Resources, level = PermissionLevel.FULL)
    @Path("/store/install")
    @Method(POST)
    public JSONResponse installResource(JSONRequest request) {
        request.checkFor("type", "projectId", "versionId");

        String providerId = request.has("provider") ? request.get("provider") : "modrinth";
        StoreProvider provider = StoreProviderRegistry.getInstance().getProvider(providerId);
        if (provider == null) {
            return new JSONResponse().error("Invalid provider: " + providerId);
        }

        ResourceType resourceType = ResourceType.fromIdentifier(request.get("type"));
        if (resourceType == null) {
            return new JSONResponse().error("Invalid resource type");
        }

        if (!provider.supportsResourceType(resourceType)) {
            return new JSONResponse().error("Provider does not support resource type");
        }

        String projectId = request.get("projectId");
        String versionId = request.get("versionId");

        StoreDownloadResult downloadResult = provider.download(projectId, versionId);
        if (!downloadResult.isSuccess()) {
            return new JSONResponse().error(downloadResult.getError());
        }

        ResourcePipe resourcePipe = getPipe(ResourcePipe.class);
        File downloadedFile = downloadResult.getDownloadedFile();
        File resourceFolder = getResourceFolder(resourceType);

        if (resourceFolder == null) {
            downloadedFile.delete();
            return new JSONResponse().error("Cannot determine resource folder");
        }

        File destinationFile = new File(resourceFolder, downloadResult.getFileName());

        try {
            Files.move(downloadedFile.toPath(), destinationFile.toPath(),
                    StandardCopyOption.REPLACE_EXISTING);

            boolean enabled = resourcePipe.loadAndEnableResource(destinationFile, resourceType);

            return new JSONResponse()
                    .message(enabled ? "Resource installed and enabled successfully" : "Resource installed (restart required to enable)")
                    .add("fileName", downloadResult.getFileName())
                    .add("projectId", projectId)
                    .add("versionId", versionId)
                    .add("enabled", enabled);

        } catch (Exception e) {
            downloadedFile.delete();
            return new JSONResponse().error("Failed to install resource: " + e.getMessage());
        }
    }

    @ApiDoc(summary = "List store-installed resources", description = "Returns resources of a given type that were installed from the store, including their originating provider and project id parsed from the file name.", tag = "Resource Store")
    @ApiField(name = "type", description = "Resource type identifier to list installed resources for")
    @AuthenticatedRoute
    @RequiresFeatures(Feature.Resources)
    @Path("/store/installed")
    @Method(GET)
    public JSONResponse getInstalledFromStore(JSONRequest request) {
        String typeId = request.has("type") ? request.get("type") : null;
        ResourceType resourceType = typeId != null ? ResourceType.fromIdentifier(typeId) : null;
        if (resourceType == null) {
            return new JSONResponse().error("Invalid or missing resource type");
        }

        ResourcePipe resourcePipe = getPipe(ResourcePipe.class);
        List<de.gnm.voxeldash.api.entities.Resource> resources = resourcePipe.getResources(resourceType);

        ArrayNode installedArray = getMapper().createArrayNode();
        for (de.gnm.voxeldash.api.entities.Resource resource : resources) {
            String fileName = resource.getFileName();
            int start = fileName.lastIndexOf("_[");
            int end = fileName.lastIndexOf("]");

            if (start > 0 && end > start) {
                String trackingInfo = fileName.substring(start + 2, end);
                String[] parts = trackingInfo.split("_", 2);
                if (parts.length == 2) {
                    ObjectNode node = getMapper().createObjectNode();
                    node.put("provider", parts[0]);
                    node.put("projectId", parts[1]);
                    node.put("fileName", fileName);
                    node.put("enabled", resource.isEnabled());
                    installedArray.add(node);
                }
            }
        }

        return new JSONResponse().add("installed", installedArray);
    }

    @ApiDoc(summary = "Get store context", description = "Returns the server context used to scope store queries, including the game version, the resolved loader for the given provider and the server software.", tag = "Resource Store")
    @ApiField(name = "provider", required = false, description = "Provider id used to resolve the loader (defaults to 'modrinth')")
    @AuthenticatedRoute
    @RequiresFeatures(Feature.Resources)
    @Path("/store/context")
    @Method(GET)
    public JSONResponse getStoreContext(JSONRequest request) {
        String providerId = request.has("provider") ? request.get("provider") : "modrinth";
        StoreProvider provider = StoreProviderRegistry.getInstance().getProvider(providerId);

        String gameVersion = getGameVersion();
        String loader = provider != null ? getLoader(provider) : null;
        String serverSoftware = getServerSoftware();

        ObjectNode context = getMapper().createObjectNode();
        context.put("gameVersion", gameVersion);
        context.put("loader", loader);
        context.put("serverSoftware", serverSoftware);

        return new JSONResponse().add("context", context);
    }

    private String getGameVersion() {
        ServerInfoPipe infoPipe = getPipe(ServerInfoPipe.class);
        if (infoPipe == null) return null;

        String fullVersion = infoPipe.getServerVersion();
        if (fullVersion == null) return null;

        return normalizeGameVersion(fullVersion);
    }

    /**
     * Pattern matching the version identifiers the stores (Modrinth/CurseForge) actually tag
     * releases with: plain releases ({@code 1.21.1}, {@code 26.1.2}, {@code 26.1}), weekly
     * snapshots ({@code 26w14a}) and Mojang's own pre-release/release-candidate/snapshot names
     * ({@code 26.2-pre-1}, {@code 1.21.11-rc1}, {@code 26.2-snapshot-8}).
     */
    private static final Pattern GAME_VERSION_PATTERN = Pattern.compile(
            "\\d+w\\d+[a-z]+" +
            "|\\d+\\.\\d+(?:\\.\\d+)?(?:-(?:pre|rc|snapshot)-?\\d*)?",
            Pattern.CASE_INSENSITIVE);

    /**
     * Normalizes a server-reported version string to the game version identifier the stores tag
     * releases with, by extracting the leading Minecraft version and discarding any server-software
     * build metadata appended to it.
     * <p>
     * Taking everything before the first {@code -} (the previous behaviour) is not enough: server
     * software appends arbitrary junk after the version, e.g. CraftBukkit's {@code 1.21.1-R0.1-SNAPSHOT}
     * or a fork's {@code 1.20.1-bukkit-<gitcommit>}, which would be searched verbatim and match
     * nothing. It also over-strips Mojang's own dash-separated names ({@code 26.2-pre-1}), which the
     * stores do tag, reducing them to non-existent releases like {@code 26.2}. Matching the known
     * version shapes instead keeps those intact while dropping the build metadata.
     */
    static String normalizeGameVersion(String version) {
        if (version == null) return null;
        String v = version.trim();
        if (v.isEmpty()) return v;
        Matcher matcher = GAME_VERSION_PATTERN.matcher(v);
        return matcher.find() ? matcher.group() : v;
    }

    private String getServerSoftware() {
        ServerInfoPipe infoPipe = getPipe(ServerInfoPipe.class);
        return infoPipe != null ? infoPipe.getServerSoftware() : null;
    }

    private String getLoader(StoreProvider provider) {
        String serverSoftware = getServerSoftware();
        return provider.mapServerSoftwareToLoader(serverSoftware);
    }

    private File getResourceFolder(ResourceType type) {
        ResourcePipe resourcePipe = getPipe(ResourcePipe.class);
        return resourcePipe.getResourceFolder(type);
    }

    private ObjectNode projectToJson(StoreProject project) {
        ObjectNode node = getMapper().createObjectNode();
        node.put("id", project.getId());
        node.put("slug", project.getSlug());
        node.put("name", project.getName());
        node.put("description", project.getDescription());
        node.put("author", project.getAuthor());
        node.put("iconUrl", project.getIconUrl());
        node.put("downloads", project.getDownloads());
        node.put("projectType", project.getProjectType());
        node.put("dateCreated", project.getDateCreated());
        node.put("dateModified", project.getDateModified());
        node.put("latestVersion", project.getLatestVersion());

        ArrayNode versionsArray = getMapper().createArrayNode();
        for (String version : project.getGameVersions()) {
            versionsArray.add(version);
        }
        node.set("gameVersions", versionsArray);

        return node;
    }

    private ObjectNode versionToJson(StoreVersion version) {
        ObjectNode node = getMapper().createObjectNode();
        node.put("id", version.getId());
        node.put("projectId", version.getProjectId());
        node.put("name", version.getName());
        node.put("versionNumber", version.getVersionNumber());
        node.put("changelog", version.getChangelog());
        node.put("versionType", version.getVersionType());
        node.put("downloads", version.getDownloads());
        node.put("datePublished", version.getDatePublished());

        ArrayNode gameVersionsArray = getMapper().createArrayNode();
        for (String gv : version.getGameVersions()) {
            gameVersionsArray.add(gv);
        }
        node.set("gameVersions", gameVersionsArray);

        ArrayNode loadersArray = getMapper().createArrayNode();
        for (String loader : version.getLoaders()) {
            loadersArray.add(loader);
        }
        node.set("loaders", loadersArray);

        ArrayNode filesArray = getMapper().createArrayNode();
        for (StoreFile file : version.getFiles()) {
            ObjectNode fileNode = getMapper().createObjectNode();
            fileNode.put("url", file.getUrl());
            fileNode.put("filename", file.getFilename());
            fileNode.put("primary", file.isPrimary());
            fileNode.put("size", file.getSize());
            filesArray.add(fileNode);
        }
        node.set("files", filesArray);

        return node;
    }
}
