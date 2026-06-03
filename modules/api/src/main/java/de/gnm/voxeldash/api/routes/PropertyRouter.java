package de.gnm.voxeldash.api.routes;

import de.gnm.voxeldash.api.annotations.ApiDoc;
import de.gnm.voxeldash.api.annotations.ApiField;
import de.gnm.voxeldash.api.annotations.AuthenticatedRoute;
import de.gnm.voxeldash.api.annotations.Method;
import de.gnm.voxeldash.api.annotations.ParamLocation;
import de.gnm.voxeldash.api.annotations.Path;
import de.gnm.voxeldash.api.annotations.RequiresFeatures;
import de.gnm.voxeldash.api.entities.Feature;
import de.gnm.voxeldash.api.entities.PermissionLevel;
import de.gnm.voxeldash.api.helper.PropertyHelper;
import de.gnm.voxeldash.api.http.JSONRequest;
import de.gnm.voxeldash.api.http.JSONResponse;

import static de.gnm.voxeldash.api.http.HTTPMethod.*;

public class PropertyRouter extends BaseRoute {

    @ApiDoc(summary = "Get a server property", description = "Returns the value of a single server property from server.properties.", tag = "Server Properties")
    @ApiField(name = "property", in = ParamLocation.PATH, description = "Name of the property to read")
    @AuthenticatedRoute
    @Path("/properties/:property")
    @RequiresFeatures(Feature.Properties)
    @Method(GET)
    public JSONResponse getProperty(JSONRequest request) {
        String property = request.getParameter("property");

        return new JSONResponse().add("value", PropertyHelper.getProperty(property));
    }

    @ApiDoc(summary = "Set a server property", description = "Updates a single server property in server.properties. The server must be restarted for the change to take effect.", tag = "Server Properties")
    @ApiField(name = "property", in = ParamLocation.PATH, description = "Name of the property to update")
    @ApiField(name = "value", description = "New value for the property")
    @AuthenticatedRoute
    @Path("/properties/:property")
    @RequiresFeatures(value = Feature.Properties, level = PermissionLevel.FULL)
    @Method(PATCH)
    public JSONResponse setProperty(JSONRequest request) {
        request.checkFor("value");
        String property = request.getParameter("property");
        String value = request.get("value");

        PropertyHelper.setProperty(property, value);
        return new JSONResponse().message("Property set successfully. Restart the server to apply the changes.");
    }

    @ApiDoc(summary = "List server properties", description = "Returns all server properties defined in server.properties.", tag = "Server Properties")
    @AuthenticatedRoute
    @RequiresFeatures(Feature.Properties)
    @Path("/properties")
    @Method(GET)
    public JSONResponse getProperties() {
        return new JSONResponse().add("properties", PropertyHelper.getProperties());
    }

}
