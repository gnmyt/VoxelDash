package de.gnm.voxeldash.api.routes;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.gnm.voxeldash.api.annotations.ApiDoc;
import de.gnm.voxeldash.api.annotations.AuthenticatedRoute;
import de.gnm.voxeldash.api.annotations.Method;
import de.gnm.voxeldash.api.annotations.Path;
import de.gnm.voxeldash.api.controller.AccountController;
import de.gnm.voxeldash.api.controller.PermissionController;
import de.gnm.voxeldash.api.entities.Feature;
import de.gnm.voxeldash.api.entities.ResourceType;
import de.gnm.voxeldash.api.http.JSONResponse;
import de.gnm.voxeldash.api.http.RawRequest;
import de.gnm.voxeldash.api.pipes.ServerInfoPipe;
import de.gnm.voxeldash.api.pipes.resources.ResourcePipe;

import java.util.List;

import static de.gnm.voxeldash.api.http.HTTPMethod.GET;

public class InfoRouter extends BaseRoute {

    @ApiDoc(summary = "Get server info", description = "Returns general server information for the authenticated user, including account name, server software and version, port, the features accessible to the user, supported resource types and whether the user is an admin.", tag = "Info")
    @AuthenticatedRoute
    @Path("/info")
    @Method(GET)
    public JSONResponse getServerInfo(RawRequest request) {
        AccountController accountController = loader.getController(AccountController.class);
        PermissionController permissionController = loader.getController(PermissionController.class);
        ServerInfoPipe serverInfoPipe = loader.getPipe(ServerInfoPipe.class);

        // Filter features based on user permissions
        List<Feature> accessibleFeatures = permissionController.getAccessibleFeatures(
            request.getUserId(), 
            loader.getAvailableFeatures()
        );
        
        boolean isAdmin = permissionController.isAdmin(request.getUserId());

        ArrayNode resourceTypes = getMapper().createArrayNode();
        if (accessibleFeatures.contains(Feature.Resources)) {
            try {
                ResourcePipe resourcePipe = loader.getPipe(ResourcePipe.class);
                List<ResourceType> types = resourcePipe.getSupportedResourceTypes();
                for (ResourceType type : types) {
                    ObjectNode typeNode = getMapper().createObjectNode();
                    typeNode.put("identifier", type.getIdentifier());
                    typeNode.put("folderName", type.getFolderName());
                    resourceTypes.add(typeNode);
                }
            } catch (Exception ignored) {
            }
        }

        return new JSONResponse()
                .add("accountName", accountController.getUsernameById(request.getUserId()))
                .add("serverSoftware", serverInfoPipe.getServerSoftware())
                .add("serverVersion", serverInfoPipe.getServerVersion())
                .add("serverPort", serverInfoPipe.getServerPort())
                .add("availableFeatures", accessibleFeatures)
                .add("resourceTypes", resourceTypes)
                .add("isAdmin", isAdmin);
    }

}
