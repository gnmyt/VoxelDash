package de.gnm.loader.helper;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;

public class ManifestHelper {

    private static final String GAME_MANIFEST = "https://launchermeta.mojang.com/mc/game/version_manifest.json";
    private final ObjectMapper mapper = new ObjectMapper();
    private final OkHttpClient client = new OkHttpClient();

    /**
     * Gets the version manifest from the given version url
     * @param versionUrl The version url
     * @return the version manifest
     */
    public ObjectNode getVersionManifest(String versionUrl) {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(versionUrl).build();

        try {
            Response response = client.newCall(request).execute();

            if (response.isSuccessful()) {
                ObjectNode jsonNodes = (ObjectNode) mapper.readTree(response.body().string());
                response.close();

                return jsonNodes;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Gets the latest release from the manifest
     * @return the latest release
     */
    public JsonNode getLatestRelease() {
        ArrayNode versions = loadManifest();
        ArrayList<JsonNode> releases = new ArrayList<>();
        for (JsonNode version : versions) {
            if (version.get("type").asText().equals("release")) {
                releases.add(version);
            }
        }

        return releases.get(0);
    }

    /**
     * Gets a specific version entry from the manifest by its id (e.g. {@code 1.21.8}).
     * @param id the Minecraft version id
     * @return the matching version entry, or {@code null} if it is not listed
     */
    public JsonNode getVersion(String id) {
        ArrayNode versions = loadManifest();
        if (versions == null) {
            return null;
        }

        for (JsonNode version : versions) {
            if (version.get("id").asText().equals(id)) {
                return version;
            }
        }

        return null;
    }

    /**
     * Loads the manifest from the given url
     * @return the manifest
     */
    public ArrayNode loadManifest() {
        Request request = new Request.Builder().url(GAME_MANIFEST).build();

        try {
            Response response = client.newCall(request).execute();

            if (response.isSuccessful()) {
                JsonNode node = mapper.readTree(response.body().string());
                JsonNode versions = node.get("versions");

                response.close();

                if (versions.isArray()) {
                    return (ArrayNode) versions;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

}
