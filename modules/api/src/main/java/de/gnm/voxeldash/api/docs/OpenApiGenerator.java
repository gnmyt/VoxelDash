package de.gnm.voxeldash.api.docs;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.gnm.voxeldash.VoxelDashLoader;
import de.gnm.voxeldash.api.annotations.ApiDoc;
import de.gnm.voxeldash.api.annotations.ApiField;
import de.gnm.voxeldash.api.annotations.AuthenticatedRoute;
import de.gnm.voxeldash.api.annotations.Method;
import de.gnm.voxeldash.api.annotations.ParamLocation;
import de.gnm.voxeldash.api.annotations.Path;
import de.gnm.voxeldash.api.annotations.RequiresFeatures;
import de.gnm.voxeldash.api.entities.Feature;
import de.gnm.voxeldash.api.entities.PermissionLevel;
import de.gnm.voxeldash.api.http.HTTPMethod;
import de.gnm.voxeldash.api.routes.BaseRoute;
import org.reflections.Reflections;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OpenApiGenerator {

    private static final String API_PREFIX = "/api";
    private static final Pattern PATH_PARAM = Pattern.compile(":(\\w+)");

    private final ObjectMapper mapper = new ObjectMapper();
    private final String version;

    public OpenApiGenerator(String version) {
        this.version = version;
    }

    /**
     * Entry point. Arguments: [0] output file path, [1] API version.
     *
     * @param args command-line arguments
     * @throws Exception if the spec cannot be written
     */
    public static void main(String[] args) throws Exception {
        String output = args.length > 0 && !args[0].isBlank() ? args[0] : "docs/public/openapi.json";
        String version = args.length > 1 && !args[1].isBlank() ? args[1] : "dev";

        ObjectNode spec = new OpenApiGenerator(version).generate();

        File outputFile = new File(output);
        if (outputFile.getParentFile() != null) {
            outputFile.getParentFile().mkdirs();
        }

        ObjectMapper writer = new ObjectMapper();
        byte[] json = writer.writerWithDefaultPrettyPrinter().writeValueAsBytes(spec);
        Files.write(outputFile.toPath(), json);

        System.out.println("[OpenApiGenerator] Wrote " + json.length + " bytes to " + outputFile.getAbsolutePath());
    }

    /**
     * Builds the full OpenAPI document.
     *
     * @return the OpenAPI document as a Jackson {@link ObjectNode}
     */
    public ObjectNode generate() {
        ObjectNode root = mapper.createObjectNode();
        root.put("openapi", "3.1.0");

        ObjectNode info = root.putObject("info");
        info.put("title", "VoxelDash API");
        info.put("version", version);
        info.put("description", "HTTP API for the VoxelDash Minecraft server dashboard. "
                + "Authenticated routes expect an `Authorization: Bearer <sessionToken>` header, "
                + "obtained from `POST /api/session/create`.");

        ArrayNode servers = root.putArray("servers");
        ObjectNode server = servers.addObject();
        server.put("url", "/");
        server.put("description", "The VoxelDash server (default port 7867)");

        ObjectNode components = root.putObject("components");
        ObjectNode securitySchemes = components.putObject("securitySchemes");
        ObjectNode bearer = securitySchemes.putObject("bearerAuth");
        bearer.put("type", "http");
        bearer.put("scheme", "bearer");
        bearer.put("description", "Session token returned by `POST /api/session/create`.");

        TreeMap<String, ObjectNode> paths = new TreeMap<>();
        Set<String> tags = new TreeSet<>();

        Reflections reflections = new Reflections(VoxelDashLoader.getRoutePackageName());
        List<Class<? extends BaseRoute>> routeClasses = new ArrayList<>(reflections.getSubTypesOf(BaseRoute.class));
        routeClasses.sort((a, b) -> a.getSimpleName().compareToIgnoreCase(b.getSimpleName()));

        int operationCount = 0;
        for (Class<? extends BaseRoute> routeClass : routeClasses) {
            for (java.lang.reflect.Method method : routeClass.getDeclaredMethods()) {
                Path path = method.getAnnotation(Path.class);
                if (path == null) continue;

                Method httpMethodAnnotation = method.getAnnotation(Method.class);
                HTTPMethod httpMethod = httpMethodAnnotation != null ? httpMethodAnnotation.value() : HTTPMethod.GET;

                String fullPath = toOpenApiPath(path.value());
                ObjectNode pathItem = paths.computeIfAbsent(fullPath, k -> mapper.createObjectNode());

                String tag = resolveTag(routeClass, method);
                tags.add(tag);

                ObjectNode operation = buildOperation(method, httpMethod, path.value(), tag);
                pathItem.set(httpMethod.name().toLowerCase(), operation);
                operationCount++;
            }
        }

        ObjectNode pathsNode = root.putObject("paths");
        paths.forEach(pathsNode::set);

        ArrayNode tagsNode = root.putArray("tags");
        for (String tag : tags) {
            tagsNode.addObject().put("name", tag);
        }

        System.out.println("[OpenApiGenerator] Discovered " + routeClasses.size() + " routers, "
                + operationCount + " operations across " + paths.size() + " paths.");

        return root;
    }

    /**
     * Builds a single OpenAPI operation object for a route method.
     */
    private ObjectNode buildOperation(java.lang.reflect.Method method, HTTPMethod httpMethod, String rawPath, String tag) {
        ObjectNode operation = mapper.createObjectNode();

        operation.putArray("tags").add(tag);
        operation.put("operationId", method.getName());

        ApiDoc doc = method.getAnnotation(ApiDoc.class);
        boolean authenticated = method.getAnnotation(AuthenticatedRoute.class) != null;
        RequiresFeatures requiresFeatures = method.getAnnotation(RequiresFeatures.class);

        if (doc != null) {
            operation.put("summary", doc.summary());
            if (doc.deprecated()) operation.put("deprecated", true);
        } else {
            operation.put("summary", method.getName());
        }

        String description = buildDescription(doc, requiresFeatures);
        if (!description.isEmpty()) operation.put("description", description);

        ArrayNode parameters = mapper.createArrayNode();
        addPathParameters(parameters, rawPath, method);
        addQueryParameters(parameters, method, httpMethod);
        if (!parameters.isEmpty()) operation.set("parameters", parameters);

        if (httpMethod != HTTPMethod.GET) {
            ObjectNode requestBody = buildRequestBody(method);
            if (requestBody != null) operation.set("requestBody", requestBody);
        }

        if (authenticated) {
            ArrayNode security = operation.putArray("security");
            security.addObject().putArray("bearerAuth");
        }

        operation.set("responses", buildResponses(authenticated, requiresFeatures));

        return operation;
    }

    /**
     * Assembles the Markdown description, appending auth/feature requirements.
     */
    private String buildDescription(ApiDoc doc, RequiresFeatures requiresFeatures) {
        StringBuilder sb = new StringBuilder();
        if (doc != null && !doc.description().isBlank()) {
            sb.append(doc.description().trim());
        }
        if (requiresFeatures != null) {
            if (sb.length() > 0) sb.append("\n\n");
            List<String> featureNames = new ArrayList<>();
            for (Feature feature : requiresFeatures.value()) {
                featureNames.add("`" + feature.name() + "`");
            }
            PermissionLevel level = requiresFeatures.level();
            sb.append("**Requires feature(s):** ").append(String.join(", ", featureNames))
                    .append(" with ").append(level == PermissionLevel.FULL ? "**FULL**" : "**READ**")
                    .append(" permission.");
        }
        return sb.toString();
    }

    /**
     * Adds path parameters discovered from the raw route path (e.g. {@code :backupName}).
     */
    private void addPathParameters(ArrayNode parameters, String rawPath, java.lang.reflect.Method method) {
        Matcher matcher = PATH_PARAM.matcher(rawPath);
        while (matcher.find()) {
            String name = matcher.group(1);
            ApiField field = findField(method, name, ParamLocation.PATH);

            ObjectNode parameter = parameters.addObject();
            parameter.put("name", name);
            parameter.put("in", "path");
            parameter.put("required", true);
            if (field != null && !field.description().isBlank()) parameter.put("description", field.description());
            ObjectNode schema = parameter.putObject("schema");
            schema.put("type", field != null ? field.type().getSchemaType() : "string");
        }
    }

    /**
     * Adds query parameters. For GET routes, BODY-located fields are exposed as query
     * parameters (the server parses GET parameters from the query string).
     */
    private void addQueryParameters(ArrayNode parameters, java.lang.reflect.Method method, HTTPMethod httpMethod) {
        for (ApiField field : method.getAnnotationsByType(ApiField.class)) {
            boolean isQuery = field.in() == ParamLocation.QUERY
                    || (field.in() == ParamLocation.BODY && httpMethod == HTTPMethod.GET);
            if (!isQuery) continue;

            ObjectNode parameter = parameters.addObject();
            parameter.put("name", field.name());
            parameter.put("in", "query");
            parameter.put("required", field.required());
            if (!field.description().isBlank()) parameter.put("description", field.description());
            parameter.putObject("schema").put("type", field.type().getSchemaType());
        }
    }

    /**
     * Builds the request body schema from BODY-located fields (non-GET routes only).
     *
     * @return the requestBody node, or {@code null} if there are no body fields
     */
    private ObjectNode buildRequestBody(java.lang.reflect.Method method) {
        ObjectNode properties = mapper.createObjectNode();
        Set<String> required = new LinkedHashSet<>();

        for (ApiField field : method.getAnnotationsByType(ApiField.class)) {
            if (field.in() != ParamLocation.BODY) continue;
            ObjectNode property = properties.putObject(field.name());
            property.put("type", field.type().getSchemaType());
            if (!field.description().isBlank()) property.put("description", field.description());
            if (field.required()) required.add(field.name());
        }

        if (properties.isEmpty()) return null;

        ObjectNode requestBody = mapper.createObjectNode();
        requestBody.put("required", true);
        ObjectNode schema = requestBody.putObject("content").putObject("application/json").putObject("schema");
        schema.put("type", "object");
        schema.set("properties", properties);
        if (!required.isEmpty()) {
            ArrayNode requiredNode = schema.putArray("required");
            required.forEach(requiredNode::add);
        }
        return requestBody;
    }

    /**
     * Builds the responses object with the common status codes for the route.
     */
    private ObjectNode buildResponses(boolean authenticated, RequiresFeatures requiresFeatures) {
        ObjectNode responses = mapper.createObjectNode();
        responses.putObject("200").put("description", "Successful response");
        responses.putObject("400").put("description", "Bad request (e.g. missing or invalid body field)");
        if (authenticated) {
            responses.putObject("401").put("description", "Unauthorized - missing or invalid session token");
        }
        if (requiresFeatures != null) {
            responses.putObject("403").put("description", "Access denied - insufficient permissions");
            responses.putObject("501").put("description", "Feature not available on this server");
        }
        responses.putObject("500").put("description", "Internal server error");
        return responses;
    }

    /**
     * Finds an {@link ApiField} matching the given name and location, or {@code null}.
     */
    private ApiField findField(java.lang.reflect.Method method, String name, ParamLocation location) {
        for (ApiField field : method.getAnnotationsByType(ApiField.class)) {
            if (field.in() == location && field.name().equals(name)) return field;
        }
        return null;
    }

    /**
     * Converts a router path ({@code /backups/:backupName}) to an OpenAPI path
     * ({@code /api/backups/{backupName}}).
     */
    private String toOpenApiPath(String routePath) {
        String converted = PATH_PARAM.matcher(routePath).replaceAll("{$1}");
        if (!converted.startsWith("/")) converted = "/" + converted;
        return API_PREFIX + converted;
    }

    /**
     * Resolves the tag for a route: explicit {@code @ApiDoc(tag=...)} wins, otherwise the
     * router class name with a trailing {@code Router} stripped.
     */
    private String resolveTag(Class<?> routeClass, java.lang.reflect.Method method) {
        ApiDoc doc = method.getAnnotation(ApiDoc.class);
        if (doc != null && !doc.tag().isBlank()) return doc.tag();

        String name = routeClass.getSimpleName();
        if (name.endsWith("Router")) name = name.substring(0, name.length() - "Router".length());
        return name;
    }
}
