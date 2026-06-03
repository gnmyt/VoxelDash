package de.gnm.voxeldash.api.routes;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.gnm.voxeldash.api.annotations.ApiDoc;
import de.gnm.voxeldash.api.annotations.ApiField;
import de.gnm.voxeldash.api.annotations.AuthenticatedRoute;
import de.gnm.voxeldash.api.annotations.FieldType;
import de.gnm.voxeldash.api.annotations.Method;
import de.gnm.voxeldash.api.annotations.ParamLocation;
import de.gnm.voxeldash.api.annotations.Path;
import de.gnm.voxeldash.api.annotations.RequiresFeatures;
import de.gnm.voxeldash.api.entities.BackupPart;
import de.gnm.voxeldash.api.helper.BackupHelper;
import de.gnm.voxeldash.api.entities.Feature;
import de.gnm.voxeldash.api.entities.PermissionLevel;
import de.gnm.voxeldash.api.http.JSONRequest;
import de.gnm.voxeldash.api.http.JSONResponse;
import de.gnm.voxeldash.api.http.RawRequest;
import de.gnm.voxeldash.api.http.Response;

import java.io.BufferedInputStream;
import java.io.File;
import java.nio.file.Files;

import static de.gnm.voxeldash.api.http.HTTPMethod.*;

public class BackupRouter extends BaseRoute {

    private BackupHelper backupHelper;

    @Override
    public void setServerRoot(File serverRoot) {
        super.setServerRoot(serverRoot);

        backupHelper = new BackupHelper(new File(serverRoot, "backups"));
    }

    @ApiDoc(summary = "List backups", description = "Returns all available backups including their ID, size and included backup modes.", tag = "Backups")
    @AuthenticatedRoute
    @RequiresFeatures(Feature.Backups)
    @Path("/backups/list")
    @Method(GET)
    public Response listBackups() {
        ArrayNode backups = getMapper().createArrayNode();
        try {
            for (File backup : backupHelper.getBackups()) { ObjectNode backupNode = getMapper().createObjectNode();
                String[] nameParts = backup.getName().replace(".zip", "").split("-");

                backupNode.put("id", Long.parseLong(nameParts[0]));
                backupNode.put("size", backup.length());

                ArrayNode partNames = getMapper().createArrayNode();
                for (BackupPart part : BackupPart.fromBackupBit(Integer.parseInt(nameParts[1]))) {
                    partNames.add(part.name());
                }
                backupNode.set("modes", partNames);

                backups.add(backupNode);
            }
        } catch (Exception e) {
            return new JSONResponse().error("Error listing backups: " + e.getMessage());
        }

        return new JSONResponse().add("backups", backups);
    }

    @ApiDoc(summary = "Download a backup", description = "Streams the requested backup archive as a downloadable file.", tag = "Backups")
    @ApiField(name = "backupName", in = ParamLocation.PATH, description = "Name/ID of the backup to download")
    @AuthenticatedRoute
    @RequiresFeatures(Feature.Backups)
    @Path("/backups/download/:backupName")
    @Method(GET)
    public Response downloadFile(RawRequest request) {
        String backupName = request.getParameter("backupName");

        if (!backupHelper.backupExists(backupName)) return new JSONResponse().error("Backup not found");

        File backupFile = backupHelper.getBackup(backupName);
        if (backupFile == null) return new JSONResponse().error("Backup not found");

        try {
            BufferedInputStream in = new BufferedInputStream(Files.newInputStream(backupFile.toPath()));
            return new Response()
                    .header("Content-Type", "application/octet-stream")
                    .header("Content-Disposition", "attachment; filename=\"" + backupFile.getName() + "\"")
                    .header("Content-Length", String.valueOf(backupFile.length()))
                    .stream(in);
        } catch (Exception e) {
            return new JSONResponse().error("Error downloading file: " + e.getMessage());
        }
    }

    @ApiDoc(summary = "Restore a backup", description = "Restores the given backup, optionally halting the server afterwards.", tag = "Backups")
    @ApiField(name = "backupName", description = "Name/ID of the backup to restore")
    @ApiField(name = "haltAfterRestore", type = FieldType.BOOLEAN, description = "Stop the server after the restore completes")
    @AuthenticatedRoute
    @RequiresFeatures(value = Feature.Backups, level = PermissionLevel.FULL)
    @Path("/backups/restore")
    @Method(POST)
    public Response restoreBackup(JSONRequest request) {
        request.checkFor("backupName", "haltAfterRestore");

        String backupName = request.get("backupName");
        boolean haltAfterRestore = request.getBoolean("haltAfterRestore");

        backupHelper.restoreBackup(backupName, haltAfterRestore);

        return new JSONResponse().message("Backup restored");
    }

    @ApiDoc(summary = "Delete a backup", description = "Permanently deletes the specified backup archive.", tag = "Backups")
    @ApiField(name = "backupName", in = ParamLocation.PATH, description = "Name/ID of the backup to delete")
    @AuthenticatedRoute
    @RequiresFeatures(value = Feature.Backups, level = PermissionLevel.FULL)
    @Path("/backups/:backupName")
    @Method(DELETE)
    public Response deleteBackup(RawRequest request) {
        String backupName = request.getParameter("backupName");

        if (!backupHelper.backupExists(backupName)) return new JSONResponse().error("Backup not found");

        try {
            backupHelper.deleteBackup(backupName);
        } catch (Exception e) {
            return new JSONResponse().error("Error deleting backup: " + e.getMessage());
        }

        return new JSONResponse().message("Backup deleted");
    }

    @ApiDoc(summary = "Create a backup", description = "Creates a new backup using the given backup mode bitmask, which selects the directories to include.", tag = "Backups")
    @ApiField(name = "backupMode", description = "Backup mode bitmask (as a string) selecting which parts to include")
    @AuthenticatedRoute
    @RequiresFeatures(value = Feature.Backups, level = PermissionLevel.FULL)
    @Path("/backups/create")
    @Method(POST)
    public Response createBackup(JSONRequest request) {
        try {
            request.checkFor("backupMode");
            String backupModeRaw = request.get("backupMode");

            int backupBit;
            try {
                backupBit = Integer.parseInt(backupModeRaw);
            } catch (NumberFormatException e) {
                return new JSONResponse().error("Invalid backup mode format");
            }

            if (!BackupPart.isValidBackupBit(backupBit)) {
                return new JSONResponse().error("Invalid backup mode");
            }

            backupHelper.createBackup(backupModeRaw, backupHelper.getBackupDirectories(backupBit).toArray(new File[0]));
            return new JSONResponse().message("Backup created");
        } catch (Exception e) {
            return new JSONResponse().error("Error creating backup: " + e.getMessage());
        }
    }

}
