package de.gnm.voxeldash.api.routes.files;

import de.gnm.voxeldash.api.annotations.ApiDoc;
import de.gnm.voxeldash.api.annotations.ApiField;
import de.gnm.voxeldash.api.annotations.AuthenticatedRoute;
import de.gnm.voxeldash.api.annotations.Method;
import de.gnm.voxeldash.api.annotations.Path;
import de.gnm.voxeldash.api.annotations.RequiresFeatures;
import de.gnm.voxeldash.api.entities.Feature;
import de.gnm.voxeldash.api.entities.PermissionLevel;
import de.gnm.voxeldash.api.helper.ArchiveHelper;
import de.gnm.voxeldash.api.helper.FileHelper;
import de.gnm.voxeldash.api.http.JSONRequest;
import de.gnm.voxeldash.api.http.JSONResponse;
import de.gnm.voxeldash.api.http.Response;
import de.gnm.voxeldash.api.routes.BaseRoute;
import org.apache.commons.io.FileUtils;

import java.io.*;

import static de.gnm.voxeldash.api.http.HTTPMethod.*;

public class FolderRouter extends BaseRoute {

    @ApiDoc(summary = "Create a folder", description = "Creates a new directory (including any missing parent directories) at the given path relative to the server root.", tag = "File Manager")
    @ApiField(name = "path", description = "Path of the directory to create, relative to the server root")
    @AuthenticatedRoute
    @RequiresFeatures(value = Feature.FileManager, level = PermissionLevel.FULL)
    @Path("/folder")
    @Method(PUT)
    public Response createFolder(JSONRequest request) {
        request.checkFor("path");
        File serverRoot = getServerRoot();
        String path = request.get("path");

        try {
            File directory = FileHelper.getNormalizedPath(serverRoot, path);

            if (directory.exists()) {
                return new JSONResponse().error("The directory already exists");
            }

            if (!directory.mkdirs()) {
                return new JSONResponse().error("Error creating directory");
            }

            return new JSONResponse().message("Directory created");
        } catch (Exception e) {
            return new JSONResponse().error("Error creating directory: " + e.getMessage());
        }
    }

    @ApiDoc(summary = "Delete a folder", description = "Recursively deletes the directory at the given path. The server root directory cannot be deleted.", tag = "File Manager")
    @ApiField(name = "path", description = "Path of the directory to delete, relative to the server root")
    @AuthenticatedRoute
    @RequiresFeatures(value = Feature.FileManager, level = PermissionLevel.FULL)
    @Path("/folder")
    @Method(DELETE)
    public Response deleteFolder(JSONRequest request) {
        request.checkFor("path");
        File serverRoot = getServerRoot();
        String path = request.get("path");

        try {
            File directory = FileHelper.getNormalizedPath(serverRoot, path);

            if (!directory.exists() || !directory.isDirectory()) {
                return new JSONResponse().error("The directory does not exist");
            }

            if (directory.getCanonicalPath().equals(serverRoot.getCanonicalPath())) {
                return new JSONResponse().error("Cannot delete the server root directory");
            }

            FileUtils.deleteDirectory(directory);

            return new JSONResponse().message("Directory deleted");
        } catch (Exception e) {
            return new JSONResponse().error("Error deleting directory: " + e.getMessage());
        }
    }

    @ApiDoc(summary = "Rename or move a folder", description = "Renames the directory at the given path to a new path. The new path must not already exist.", tag = "File Manager")
    @ApiField(name = "path", description = "Current path of the directory, relative to the server root")
    @ApiField(name = "newPath", description = "New path for the directory, relative to the server root")
    @AuthenticatedRoute
    @RequiresFeatures(value = Feature.FileManager, level = PermissionLevel.FULL)
    @Path("/folder/rename")
    @Method(PATCH)
    public Response renameFolder(JSONRequest request) {
        request.checkFor("path", "newPath");
        File serverRoot = getServerRoot();
        String path = request.get("path");
        String newPath = request.get("newPath");

        try {
            File directory = FileHelper.getNormalizedPath(serverRoot, path);
            File newDirectory = FileHelper.getNormalizedPath(serverRoot, newPath);

            if (!directory.exists() || !directory.isDirectory()) {
                return new JSONResponse().error("The directory does not exist");
            }

            if (newDirectory.exists()) {
                return new JSONResponse().error("The new directory already exists");
            }

            if (!directory.renameTo(newDirectory)) {
                return new JSONResponse().error("Error renaming directory");
            }

            return new JSONResponse().message("Directory renamed");
        } catch (Exception e) {
            return new JSONResponse().error("Error renaming directory: " + e.getMessage());
        }
    }

    @ApiDoc(summary = "Download a folder as ZIP", description = "Streams the directory at the given path as a ZIP archive attachment. The server root directory cannot be downloaded.", tag = "File Manager")
    @ApiField(name = "path", description = "Path of the directory to download, relative to the server root")
    @AuthenticatedRoute
    @RequiresFeatures(Feature.FileManager)
    @Path("/folder/download")
    @Method(GET)
    public Response downloadFolder(JSONRequest request) {
        request.checkFor("path");
        File serverRoot = getServerRoot();
        String path = request.get("path");

        try {
            File directory = FileHelper.getNormalizedPath(serverRoot, path);

            if (!directory.exists() || !directory.isDirectory()) {
                return new JSONResponse().error("The directory does not exist");
            }

            if (directory.getCanonicalPath().equals(serverRoot.getCanonicalPath())) {
                return new JSONResponse().error("Cannot download the server root directory");
            }

            InputStream zipStream = ArchiveHelper.createZipStream(directory);

            return new Response()
                    .header("Content-Type", "application/zip")
                    .header("Content-Disposition", "attachment; filename=\"" + directory.getName() + ".zip\"")
                    .stream(zipStream);
        } catch (Exception e) {
            return new JSONResponse().error("Error downloading folder: " + e.getMessage());
        }
    }

    @ApiDoc(summary = "Copy a folder", description = "Recursively copies a directory from a source path to a destination path. The destination must not already exist and the server root cannot be copied.", tag = "File Manager")
    @ApiField(name = "sourcePath", description = "Path of the directory to copy, relative to the server root")
    @ApiField(name = "destinationPath", description = "Destination path for the copied directory, relative to the server root")
    @AuthenticatedRoute
    @RequiresFeatures(value = Feature.FileManager, level = PermissionLevel.FULL)
    @Path("/folder/copy")
    @Method(POST)
    public Response copyFolder(JSONRequest request) {
        request.checkFor("sourcePath", "destinationPath");
        File serverRoot = getServerRoot();
        String sourcePath = request.get("sourcePath");
        String destinationPath = request.get("destinationPath");

        try {
            File sourceDirectory = FileHelper.getNormalizedPath(serverRoot, sourcePath);
            File destinationDirectory = FileHelper.getNormalizedPath(serverRoot, destinationPath);

            if (!sourceDirectory.exists() || !sourceDirectory.isDirectory()) {
                return new JSONResponse().error("The source directory does not exist");
            }

            if (sourceDirectory.getCanonicalPath().equals(serverRoot.getCanonicalPath())) {
                return new JSONResponse().error("Cannot copy the server root directory");
            }

            if (destinationDirectory.exists()) {
                return new JSONResponse().error("A directory already exists at the destination path");
            }

            FileUtils.copyDirectory(sourceDirectory, destinationDirectory);

            return new JSONResponse().message("Directory copied successfully");
        } catch (Exception e) {
            return new JSONResponse().error("Error copying directory: " + e.getMessage());
        }
    }

    @ApiDoc(summary = "Move a folder", description = "Moves a directory from a source path to a destination path. The destination must not already exist and the server root cannot be moved.", tag = "File Manager")
    @ApiField(name = "sourcePath", description = "Path of the directory to move, relative to the server root")
    @ApiField(name = "destinationPath", description = "Destination path for the moved directory, relative to the server root")
    @AuthenticatedRoute
    @RequiresFeatures(value = Feature.FileManager, level = PermissionLevel.FULL)
    @Path("/folder/move")
    @Method(POST)
    public Response moveFolder(JSONRequest request) {
        request.checkFor("sourcePath", "destinationPath");
        File serverRoot = getServerRoot();
        String sourcePath = request.get("sourcePath");
        String destinationPath = request.get("destinationPath");

        try {
            File sourceDirectory = FileHelper.getNormalizedPath(serverRoot, sourcePath);
            File destinationDirectory = FileHelper.getNormalizedPath(serverRoot, destinationPath);

            if (!sourceDirectory.exists() || !sourceDirectory.isDirectory()) {
                return new JSONResponse().error("The source directory does not exist");
            }

            if (sourceDirectory.getCanonicalPath().equals(serverRoot.getCanonicalPath())) {
                return new JSONResponse().error("Cannot move the server root directory");
            }

            if (destinationDirectory.exists()) {
                return new JSONResponse().error("A directory already exists at the destination path");
            }

            FileUtils.moveDirectory(sourceDirectory, destinationDirectory);

            return new JSONResponse().message("Directory moved successfully");
        } catch (Exception e) {
            return new JSONResponse().error("Error moving directory: " + e.getMessage());
        }
    }

}
