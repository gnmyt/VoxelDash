package de.gnm.voxeldash.api.routes;

import de.gnm.voxeldash.api.annotations.*;
import de.gnm.voxeldash.api.entities.Feature;
import de.gnm.voxeldash.api.entities.PermissionLevel;
import de.gnm.voxeldash.api.entities.motd.Motd;
import de.gnm.voxeldash.api.entities.motd.MotdCapabilities;
import de.gnm.voxeldash.api.helper.MotdHelper;
import de.gnm.voxeldash.api.http.JSONRequest;
import de.gnm.voxeldash.api.http.JSONResponse;
import de.gnm.voxeldash.api.pipes.MotdPipe;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.Base64;

import static de.gnm.voxeldash.api.http.HTTPMethod.*;

public class MotdRouter extends BaseRoute {

    @ApiDoc(summary = "Get the MOTD", description = "Returns the canonical MOTD model, the current server icon (base64 PNG, if any) and the platform's MOTD capabilities.", tag = "MOTD")
    @AuthenticatedRoute
    @Path("/motd")
    @RequiresFeatures(Feature.Motd)
    @Method(GET)
    public JSONResponse getMotd() {
        MotdCapabilities capabilities = capabilities();

        return new JSONResponse()
                .add("motd", MotdHelper.getMotd())
                .add("icon", MotdHelper.getIconBase64())
                .add("capabilities", capabilities);
    }

    @ApiDoc(summary = "Set the MOTD", description = "Stores the canonical MOTD model and applies it to the server. Depending on the platform the change may take effect live or require a restart (see capabilities).", tag = "MOTD")
    @ApiField(name = "motd", description = "The canonical MOTD model (lines of styled segments)")
    @AuthenticatedRoute
    @Path("/motd")
    @RequiresFeatures(value = Feature.Motd, level = PermissionLevel.FULL)
    @Method(PUT)
    public JSONResponse setMotd(JSONRequest request) {
        request.checkFor("motd");

        Motd motd;
        try {
            motd = getMapper().treeToValue(request.getJson("motd"), Motd.class);
        } catch (Exception e) {
            return new JSONResponse().error("Invalid MOTD model: " + e.getMessage());
        }

        MotdHelper.saveMotd(motd);

        MotdPipe pipe = getPipe(MotdPipe.class);
        if (pipe != null) {
            pipe.apply(motd);
        }

        return new JSONResponse().message("MOTD saved successfully.");
    }

    @ApiDoc(summary = "Set the server icon", description = "Uploads the 64x64 server-list icon (server-icon.png).", tag = "MOTD")
    @ApiField(name = "image", description = "The icon as a base64-encoded PNG (optionally a data: URL). Must be 64x64.")
    @AuthenticatedRoute
    @Path("/motd/icon")
    @RequiresFeatures(value = Feature.Motd, level = PermissionLevel.FULL)
    @Method(POST)
    public JSONResponse setIcon(JSONRequest request) {
        request.checkFor("image");

        byte[] png;
        try {
            String raw = request.get("image");
            int comma = raw.indexOf(',');
            if (raw.startsWith("data:") && comma >= 0) {
                raw = raw.substring(comma + 1);
            }
            png = Base64.getDecoder().decode(raw.trim());
        } catch (Exception e) {
            return new JSONResponse().error("Could not decode the image.");
        }

        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(png));
            if (image == null) {
                return new JSONResponse().error("The uploaded file is not a valid image.");
            }
            if (image.getWidth() != 64 || image.getHeight() != 64) {
                return new JSONResponse().error("The server icon must be exactly 64x64 pixels.");
            }
        } catch (Exception e) {
            return new JSONResponse().error("The uploaded file is not a valid image.");
        }

        MotdHelper.saveIcon(png);

        MotdPipe pipe = getPipe(MotdPipe.class);
        if (pipe != null) {
            pipe.applyIcon(png);
        }

        return new JSONResponse().message("Server icon updated successfully.");
    }

    @ApiDoc(summary = "Remove the server icon", description = "Deletes the custom server-list icon.", tag = "MOTD")
    @AuthenticatedRoute
    @Path("/motd/icon")
    @RequiresFeatures(value = Feature.Motd, level = PermissionLevel.FULL)
    @Method(DELETE)
    public JSONResponse deleteIcon() {
        MotdHelper.clearIcon();

        MotdPipe pipe = getPipe(MotdPipe.class);
        if (pipe != null) {
            pipe.applyIcon(null);
        }

        return new JSONResponse().message("Server icon removed.");
    }

    private MotdCapabilities capabilities() {
        MotdPipe pipe = getPipe(MotdPipe.class);
        return pipe != null ? pipe.getCapabilities() : new MotdCapabilities();
    }
}
