package de.gnm.voxeldash.api.routes.service;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.gnm.voxeldash.api.annotations.ApiDoc;
import de.gnm.voxeldash.api.annotations.ApiField;
import de.gnm.voxeldash.api.annotations.AuthenticatedRoute;
import de.gnm.voxeldash.api.annotations.Method;
import de.gnm.voxeldash.api.annotations.ParamLocation;
import de.gnm.voxeldash.api.annotations.Path;
import de.gnm.voxeldash.api.annotations.RequiresFeatures;
import de.gnm.voxeldash.api.controller.SSHController;
import de.gnm.voxeldash.api.entities.Feature;
import de.gnm.voxeldash.api.entities.PermissionLevel;
import de.gnm.voxeldash.api.http.JSONRequest;
import de.gnm.voxeldash.api.http.JSONResponse;
import de.gnm.voxeldash.api.http.Response;
import de.gnm.voxeldash.api.routes.BaseRoute;
import org.apache.sshd.common.session.helpers.AbstractSession;

import static de.gnm.voxeldash.api.http.HTTPMethod.PATCH;
import static de.gnm.voxeldash.api.http.HTTPMethod.POST;

public class SSHRouter extends BaseRoute {

    @ApiDoc(summary = "Get SSH configuration", description = "Returns the current SSH/SFTP configuration including whether it is enabled, the port, SFTP and console flags, and the list of active client sessions.", tag = "SSH & SFTP")
    @AuthenticatedRoute
    @RequiresFeatures(Feature.SSH)
    @Path("/service/ssh")
    public Response sshConfiguration() {
        SSHController controller = getController(SSHController.class);

        boolean isEnabled = controller.isEnabled();
        ArrayNode activeClients = getMapper().createArrayNode();
        if (isEnabled) {
            for (AbstractSession client : controller.getActiveSessions()) {
                if (client.getUsername() == null) continue;
                ObjectNode clientNode = getMapper().createObjectNode();
                clientNode.put("username", client.getUsername());
                clientNode.put("address", client.getIoSession().getRemoteAddress().toString().replace("/", ""));
                clientNode.put("sessionId", client.getSessionId());

                clientNode.put("isSFTP", client.getAttribute(controller.getIsSFTP()) != null && client.getAttribute(controller.getIsSFTP()));
                activeClients.add(clientNode);
            }
        }

        return new JSONResponse()
                .add("enabled", isEnabled)
                .add("port", controller.getPort())
                .add("sftpEnabled", controller.isSFTPEnabled())
                .add("consoleEnabled", controller.isConsoleEnabled())
                .add("activeClients", activeClients);
    }

    @ApiDoc(summary = "Disconnect an SSH client", description = "Closes the active SSH/SFTP session identified by the given session id.", tag = "SSH & SFTP")
    @ApiField(name = "sessionId", description = "Id of the session to disconnect")
    @AuthenticatedRoute
    @RequiresFeatures(value = Feature.SSH, level = PermissionLevel.FULL)
    @Method(POST)
    @Path("/service/ssh/disconnect")
    public Response disconnectClient(JSONRequest request) {
        request.checkFor("sessionId");
        SSHController controller = getController(SSHController.class);

        String sessionId = request.get("sessionId");

        AbstractSession session = controller.getSessionById(sessionId);
        if (session == null) {
            return new JSONResponse().add("error", "Session not found");
        }

        session.close(false);

        return new JSONResponse().add("success", "Session closed");
    }

    @ApiDoc(summary = "Update SSH configuration", description = "Updates a single SSH configuration value. The `configKey` path parameter selects the setting (enabled, sftpEnabled, consoleEnabled or port) and `value` carries the new value.", tag = "SSH & SFTP")
    @ApiField(name = "configKey", in = ParamLocation.PATH, description = "Configuration key to update (enabled, sftpEnabled, consoleEnabled or port)")
    @ApiField(name = "value", description = "New value for the configuration key")
    @AuthenticatedRoute
    @RequiresFeatures(value = Feature.SSH, level = PermissionLevel.FULL)
    @Method(PATCH)
    @Path("/service/ssh/:configKey")
    public Response setSSHConfiguration(JSONRequest request) {
        request.checkFor("value");
        SSHController controller = getController(SSHController.class);

        String configKey = request.getParameter("configKey");
        String configValue = request.get("value");

        switch (configKey) {
            case "enabled":
                controller.setEnabled(Boolean.parseBoolean(configValue));
                break;
            case "sftpEnabled":
                controller.setSFTPEnabled(Boolean.parseBoolean(configValue));
                break;
            case "consoleEnabled":
                controller.setConsoleEnabled(Boolean.parseBoolean(configValue));
                break;
            case "port":
                controller.setPort(Integer.parseInt(configValue));
                break;
            default:
                return new JSONResponse().add("error", "Unknown config key");
        }

        return new JSONResponse().add("success", "Configuration updated");
    }


}
