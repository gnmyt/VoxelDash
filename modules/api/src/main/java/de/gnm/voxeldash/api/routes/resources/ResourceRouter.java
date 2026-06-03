package de.gnm.voxeldash.api.routes.resources;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.gnm.voxeldash.api.annotations.ApiDoc;
import de.gnm.voxeldash.api.annotations.ApiField;
import de.gnm.voxeldash.api.annotations.AuthenticatedRoute;
import de.gnm.voxeldash.api.annotations.FieldType;
import de.gnm.voxeldash.api.annotations.Method;
import de.gnm.voxeldash.api.annotations.Path;
import de.gnm.voxeldash.api.annotations.RequiresFeatures;
import de.gnm.voxeldash.api.entities.ConfigFile;
import de.gnm.voxeldash.api.entities.Feature;
import de.gnm.voxeldash.api.entities.PermissionLevel;
import de.gnm.voxeldash.api.entities.Resource;
import de.gnm.voxeldash.api.entities.ResourceType;
import de.gnm.voxeldash.api.http.JSONRequest;
import de.gnm.voxeldash.api.http.JSONResponse;
import de.gnm.voxeldash.api.pipes.resources.ResourcePipe;
import de.gnm.voxeldash.api.routes.BaseRoute;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static de.gnm.voxeldash.api.http.HTTPMethod.*;

public class ResourceRouter extends BaseRoute {

    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();

    @ApiDoc(summary = "List resources", description = "Returns all installed resources of the given resource type, including their metadata and enabled state.", tag = "Resources")
    @ApiField(name = "type", description = "Resource type identifier to list resources for")
    @AuthenticatedRoute
    @RequiresFeatures(Feature.Resources)
    @Path("/resources/list")
    @Method(GET)
    public JSONResponse getResources(JSONRequest request) {
        ResourceType type = getResourceType(request);
        if (type == null) return new JSONResponse().error("Invalid or missing resource type");

        List<Resource> resources = getPipe(ResourcePipe.class).getResources(type);
        ArrayNode array = getMapper().createArrayNode();
        resources.forEach(r -> array.add(resourceToJson(r)));

        return new JSONResponse().add("resources", array);
    }

    @ApiDoc(summary = "Get a resource", description = "Returns the metadata for a single resource identified by its type and file name.", tag = "Resources")
    @ApiField(name = "type", description = "Resource type identifier of the resource")
    @ApiField(name = "fileName", description = "File name of the resource to fetch")
    @AuthenticatedRoute
    @RequiresFeatures(Feature.Resources)
    @Path("/resources/get")
    @Method(GET)
    public JSONResponse getResource(JSONRequest request) {
        ResourceType type = getResourceType(request);
        String fileName = getFileName(request);
        if (type == null || fileName == null) return new JSONResponse().error("Invalid or missing type/fileName");

        Resource resource = getPipe(ResourcePipe.class).getResource(fileName, type);
        if (resource == null) return new JSONResponse().error("Resource not found: " + fileName);

        return new JSONResponse().add("resource", resourceToJson(resource));
    }

    @ApiDoc(summary = "Enable a resource", description = "Enables the given resource so it is loaded by the server.", tag = "Resources")
    @ApiField(name = "type", description = "Resource type identifier of the resource")
    @ApiField(name = "fileName", description = "File name of the resource to enable")
    @AuthenticatedRoute
    @RequiresFeatures(value = Feature.Resources, level = PermissionLevel.FULL)
    @Path("/resources/enable")
    @Method(POST)
    public JSONResponse enableResource(JSONRequest request) {
        return toggleResource(request, true);
    }

    @ApiDoc(summary = "Disable a resource", description = "Disables the given resource so it is no longer loaded by the server.", tag = "Resources")
    @ApiField(name = "type", description = "Resource type identifier of the resource")
    @ApiField(name = "fileName", description = "File name of the resource to disable")
    @AuthenticatedRoute
    @RequiresFeatures(value = Feature.Resources, level = PermissionLevel.FULL)
    @Path("/resources/disable")
    @Method(POST)
    public JSONResponse disableResource(JSONRequest request) {
        return toggleResource(request, false);
    }

    @ApiDoc(summary = "Delete a resource", description = "Permanently deletes the given resource file from the server.", tag = "Resources")
    @ApiField(name = "type", description = "Resource type identifier of the resource")
    @ApiField(name = "fileName", description = "File name of the resource to delete")
    @AuthenticatedRoute
    @RequiresFeatures(value = Feature.Resources, level = PermissionLevel.FULL)
    @Path("/resources/delete")
    @Method(DELETE)
    public JSONResponse deleteResource(JSONRequest request) {
        request.checkFor("type", "fileName");
        ResourceType type = ResourceType.fromIdentifier(request.get("type"));
        if (type == null) return new JSONResponse().error("Invalid resource type");

        boolean success = getPipe(ResourcePipe.class).deleteResource(request.get("fileName"), type);
        return success ? new JSONResponse().message("Resource deleted successfully")
                       : new JSONResponse().error("Failed to delete resource");
    }

    @ApiDoc(summary = "List config files", description = "Returns the configuration files belonging to the given resource, including their name, path and size.", tag = "Resources")
    @ApiField(name = "type", description = "Resource type identifier of the resource")
    @ApiField(name = "fileName", description = "File name of the resource whose config files are listed")
    @AuthenticatedRoute
    @RequiresFeatures(Feature.Resources)
    @Path("/resources/config/list")
    @Method(GET)
    public JSONResponse getConfigFiles(JSONRequest request) {
        ResourceType type = getResourceType(request);
        String fileName = getFileName(request);
        if (type == null || fileName == null) return new JSONResponse().error("Invalid or missing type/fileName");

        List<ConfigFile> files = getPipe(ResourcePipe.class).getConfigFiles(fileName, type);
        ArrayNode array = getMapper().createArrayNode();
        for (ConfigFile file : files) {
            ObjectNode node = getMapper().createObjectNode();
            node.put("name", file.getName());
            node.put("path", file.getPath());
            node.put("size", file.getSize());
            array.add(node);
        }

        return new JSONResponse().add("files", array);
    }

    @ApiDoc(summary = "Get config file content", description = "Reads and returns the parsed content of a resource's configuration file. Supports JSON, properties and YAML files.", tag = "Resources")
    @ApiField(name = "type", description = "Resource type identifier of the resource")
    @ApiField(name = "fileName", description = "File name of the resource that owns the config file")
    @ApiField(name = "configPath", description = "Relative path of the config file to read")
    @AuthenticatedRoute
    @RequiresFeatures(Feature.Resources)
    @Path("/resources/config/get")
    @Method(GET)
    public JSONResponse getConfigContent(JSONRequest request) {
        ConfigFile configFile = resolveConfigFile(request);
        if (configFile == null) return new JSONResponse().error("Config file not found");

        Map<String, Object> content = readConfigFile(configFile.getAbsolutePath());
        if (content == null) return new JSONResponse().error("Failed to read config file");

        return new JSONResponse().add("content", getMapper().valueToTree(content));
    }

    @ApiDoc(summary = "Save config file content", description = "Writes the given content back to a resource's configuration file, serialized to its native format (JSON, properties or YAML).", tag = "Resources")
    @ApiField(name = "type", description = "Resource type identifier of the resource")
    @ApiField(name = "fileName", description = "File name of the resource that owns the config file")
    @ApiField(name = "configPath", description = "Relative path of the config file to write")
    @ApiField(name = "content", type = FieldType.OBJECT, description = "Configuration content to persist as a key/value object")
    @AuthenticatedRoute
    @RequiresFeatures(value = Feature.Resources, level = PermissionLevel.FULL)
    @Path("/resources/config/save")
    @Method(POST)
    public JSONResponse saveConfigContent(JSONRequest request) {
        request.checkFor("type", "fileName", "configPath", "content");

        ConfigFile configFile = resolveConfigFile(request);
        if (configFile == null) return new JSONResponse().error("Config file not found");

        Map<String, Object> content = jsonNodeToMap(request.getJson("content"));
        boolean success = writeConfigFile(configFile.getAbsolutePath(), content);

        return success ? new JSONResponse().message("Config saved successfully")
                       : new JSONResponse().error("Failed to save config");
    }

    private ResourceType getResourceType(JSONRequest request) {
        String typeId = request.has("type") ? request.get("type") : null;
        if (typeId == null || typeId.isEmpty()) return null;

        ResourceType type = ResourceType.fromIdentifier(typeId);
        if (type == null) return null;

        if (!getPipe(ResourcePipe.class).getSupportedResourceTypes().contains(type)) return null;
        return type;
    }

    private String getFileName(JSONRequest request) {
        String fileName = request.has("fileName") ? request.get("fileName") : null;
        return (fileName == null || fileName.isEmpty()) ? null : fileName;
    }

    private JSONResponse toggleResource(JSONRequest request, boolean enable) {
        request.checkFor("type", "fileName");
        ResourceType type = ResourceType.fromIdentifier(request.get("type"));
        if (type == null) return new JSONResponse().error("Invalid resource type");

        ResourcePipe pipe = getPipe(ResourcePipe.class);
        boolean success = enable ? pipe.enableResource(request.get("fileName"), type)
                                 : pipe.disableResource(request.get("fileName"), type);

        String action = enable ? "enabled" : "disabled";
        return success ? new JSONResponse().message("Resource " + action + " successfully")
                       : new JSONResponse().error("Failed to " + (enable ? "enable" : "disable") + " resource");
    }

    private ConfigFile resolveConfigFile(JSONRequest request) {
        ResourceType type = getResourceType(request);
        String fileName = getFileName(request);
        String configPath = request.has("configPath") ? request.get("configPath") : null;

        if (type == null || fileName == null || configPath == null) return null;

        List<ConfigFile> files = getPipe(ResourcePipe.class).getConfigFiles(fileName, type);
        for (ConfigFile file : files) {
            if (file.getPath().equals(configPath)) return file;
        }
        return null;
    }

    private ObjectNode resourceToJson(Resource resource) {
        ObjectNode node = getMapper().createObjectNode();
        node.put("name", resource.getName());
        node.put("fileName", resource.getFileName());
        node.put("type", resource.getType().getIdentifier());
        node.put("version", resource.getVersion());
        node.put("description", resource.getDescription());
        node.put("enabled", resource.isEnabled());
        node.put("iconPath", resource.getIconPath());
        node.put("fileSize", resource.getFileSize());

        ArrayNode authorsArray = getMapper().createArrayNode();
        for (String author : resource.getAuthors()) {
            authorsArray.add(author);
        }
        node.set("authors", authorsArray);

        return node;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> readConfigFile(File file) {
        String name = file.getName().toLowerCase();
        try {
            if (name.endsWith(".json")) {
                try (Reader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
                    return JSON_MAPPER.readValue(reader, Map.class);
                }
            } else if (name.endsWith(".properties")) {
                Properties props = new Properties();
                try (FileInputStream fis = new FileInputStream(file)) {
                    props.load(fis);
                }
                Map<String, Object> result = new HashMap<>();
                for (String key : props.stringPropertyNames()) {
                    result.put(key, props.getProperty(key));
                }
                return result;
            } else {
                try (Reader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
                    return new Yaml().load(reader);
                }
            }
        } catch (Exception e) {
            return null;
        }
    }

    private boolean writeConfigFile(File file, Map<String, Object> content) {
        String name = file.getName().toLowerCase();
        try {
            if (name.endsWith(".json")) {
                try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
                    JSON_MAPPER.writerWithDefaultPrettyPrinter().writeValue(writer, content);
                }
            } else if (name.endsWith(".properties")) {
                Properties props = new Properties();
                for (Map.Entry<String, Object> entry : content.entrySet()) {
                    if (entry.getValue() != null) {
                        props.setProperty(entry.getKey(), String.valueOf(entry.getValue()));
                    }
                }
                try (FileOutputStream fos = new FileOutputStream(file)) {
                    props.store(fos, null);
                }
            } else {
                DumperOptions options = new DumperOptions();
                options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
                options.setPrettyFlow(true);
                options.setIndent(2);
                try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
                    new Yaml(options).dump(content, writer);
                }
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> jsonNodeToMap(JsonNode node) {
        if (node == null || node.isNull()) return new HashMap<>();
        return getMapper().convertValue(node, Map.class);
    }
}
