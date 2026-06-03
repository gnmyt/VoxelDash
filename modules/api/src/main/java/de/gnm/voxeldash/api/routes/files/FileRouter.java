package de.gnm.voxeldash.api.routes.files;

import com.fasterxml.jackson.databind.node.ArrayNode;
import de.gnm.voxeldash.api.annotations.ApiDoc;
import de.gnm.voxeldash.api.annotations.ApiField;
import de.gnm.voxeldash.api.annotations.AuthenticatedRoute;
import de.gnm.voxeldash.api.annotations.Method;
import de.gnm.voxeldash.api.annotations.ParamLocation;
import de.gnm.voxeldash.api.annotations.Path;
import de.gnm.voxeldash.api.annotations.RequiresFeatures;
import de.gnm.voxeldash.api.entities.Feature;
import de.gnm.voxeldash.api.entities.PermissionLevel;
import de.gnm.voxeldash.api.http.JSONRequest;
import de.gnm.voxeldash.api.http.JSONResponse;
import de.gnm.voxeldash.api.http.RawRequest;
import de.gnm.voxeldash.api.http.Response;
import de.gnm.voxeldash.api.routes.BaseRoute;
import de.gnm.voxeldash.api.helper.FileHelper;

import java.io.*;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;

import static de.gnm.voxeldash.api.http.HTTPMethod.*;

public class FileRouter extends BaseRoute {

    @ApiDoc(summary = "List files in a directory", description = "Lists the files and folders contained in the given directory (relative to the server root) and the total count.", tag = "File Manager")
    @ApiField(name = "path", description = "Directory path relative to the server root to list")
    @AuthenticatedRoute
    @RequiresFeatures(Feature.FileManager)
    @Path("/files/list")
    @Method(GET)
    public Response listFiles(JSONRequest request) {
        request.checkFor("path");
        File serverRoot = getServerRoot();
        String path = request.get("path");

        try {
            File directory = FileHelper.getNormalizedPath(serverRoot, path);

            if (!directory.exists() || !directory.isDirectory()) {
                return new JSONResponse().error("The directory does not exist");
            }

            ArrayNode files = FileHelper.getFilesAsJsonArray(Objects.requireNonNull(directory.listFiles()));
            JSONResponse response = new JSONResponse();
            response.add("files", files);
            response.add("count", files.size());

            return response;
        } catch (Exception e) {
            return new JSONResponse().error("Error accessing directory: " + e.getMessage());
        }
    }

    @ApiDoc(summary = "Delete a file", description = "Deletes the file at the given path relative to the server root.", tag = "File Manager")
    @ApiField(name = "path", description = "Path of the file to delete, relative to the server root")
    @AuthenticatedRoute
    @RequiresFeatures(value = Feature.FileManager, level = PermissionLevel.FULL)
    @Path("/files")
    @Method(DELETE)
    public Response deleteFile(JSONRequest request) {
        request.checkFor("path");
        File serverRoot = getServerRoot();
        String path = request.get("path");

        try {
            File file = FileHelper.getNormalizedPath(serverRoot, path);

            if (!file.exists() || file.isDirectory()) {
                return new JSONResponse().error("The file does not exist");
            }

            if (!file.delete()) {
                return new JSONResponse().error("Error deleting file");
            }

            return new JSONResponse().message("File deleted");
        } catch (Exception e) {
            return new JSONResponse().error("Error deleting file: " + e.getMessage());
        }
    }

    @ApiDoc(summary = "Download a file", description = "Streams the file at the given path as a binary attachment. The internal `voxeldash.db` database file cannot be downloaded.", tag = "File Manager")
    @ApiField(name = "path", description = "Path of the file to download, relative to the server root")
    @AuthenticatedRoute
    @RequiresFeatures(Feature.FileManager)
    @Path("/files/download")
    @Method(GET)
    public Response downloadFile(JSONRequest request) {
        request.checkFor("path");
        File serverRoot = getServerRoot();
        String path = request.get("path");

        try {
            File file = FileHelper.getNormalizedPath(serverRoot, path);

            if (!file.exists() || file.isDirectory()) {
                return new JSONResponse().error("The file does not exist");
            }

            if (file.getName().equals("voxeldash.db")) {
                return new JSONResponse().error("You are not allowed to download the database file");
            }

            BufferedInputStream in = new BufferedInputStream(Files.newInputStream(file.toPath()));

            return new Response()
                    .header("Content-Type", "application/octet-stream")
                    .header("Content-Disposition", "attachment; filename=\"" + file.getName() + "\"")
                    .header("Content-Length", String.valueOf(file.length()))
                    .stream(in);
        } catch (Exception e) {
            return new JSONResponse().error("Error downloading file: " + e.getMessage());
        }
    }

    @ApiDoc(summary = "Rename a file", description = "Renames the file at the given path to a new name within the same directory.", tag = "File Manager")
    @ApiField(name = "path", description = "Path of the file to rename, relative to the server root")
    @ApiField(name = "newName", description = "New name for the file")
    @AuthenticatedRoute
    @RequiresFeatures(value = Feature.FileManager, level = PermissionLevel.FULL)
    @Path("/files/rename")
    @Method(PATCH)
    public Response renameFile(JSONRequest request) {
        request.checkFor("path", "newName");
        File serverRoot = getServerRoot();
        String path = request.get("path");
        String newName = request.get("newName");

        try {
            File file = FileHelper.getNormalizedPath(serverRoot, path);

            if (!file.exists() || file.isDirectory()) {
                return new JSONResponse().error("The file does not exist");
            }

            File newFile = new File(file.getParentFile(), newName);

            if (!file.renameTo(newFile)) {
                return new JSONResponse().error("Error renaming file");
            }

            return new JSONResponse().message("File renamed");
        } catch (Exception e) {
            return new JSONResponse().error("Error renaming file: " + e.getMessage());
        }
    }

    @ApiDoc(summary = "Update file content", description = "Overwrites the content of the file at the given path with the provided text content.", tag = "File Manager")
    @ApiField(name = "path", description = "Path of the file to update, relative to the server root")
    @ApiField(name = "content", description = "New textual content to write to the file")
    @AuthenticatedRoute
    @RequiresFeatures(value = Feature.FileManager, level = PermissionLevel.FULL)
    @Path("/files/content")
    @Method(PATCH)
    public Response updateFileContent(JSONRequest request) {
        request.checkFor("path", "content");
        File serverRoot = getServerRoot();
        String path = request.get("path");
        String content = request.get("content");

        try {
            File file = FileHelper.getNormalizedPath(serverRoot, path);

            if (!file.exists() || file.isDirectory()) {
                return new JSONResponse().error("The file does not exist");
            }

            try (PrintWriter writer = new PrintWriter(file)) {
                writer.write(content);
            }

            return new JSONResponse().message("File content updated");
        } catch (Exception e) {
            return new JSONResponse().error("Error updating file content: " + e.getMessage());
        }
    }

    @ApiDoc(summary = "Initialize a chunked upload", description = "Creates a temporary upload session and returns its `uuid`, which is used to upload chunks and finalize the upload.", tag = "File Manager")
    @AuthenticatedRoute
    @RequiresFeatures(value = Feature.FileManager, level = PermissionLevel.FULL)
    @Path("/files/upload/init")
    @Method(POST)
    public Response initUpload() {
        File serverRoot = getServerRoot();
        String tempDirectoryName = UUID.randomUUID().toString();
        File tempDirectory = new File(serverRoot, "uploads/" + tempDirectoryName);

        try {
            if (!tempDirectory.mkdirs()) {
                return new JSONResponse().error("Error creating temporary upload directory");
            }
            return new JSONResponse().add("uuid", tempDirectoryName).message("Upload initialized");
        } catch (Exception e) {
            return new JSONResponse().error("Error initializing upload: " + e.getMessage());
        }
    }

    @ApiDoc(summary = "Upload a file chunk", description = "Uploads a single chunk of a file for the given upload session. The raw chunk bytes are sent as the binary request body.", tag = "File Manager")
    @ApiField(name = "uuid", in = ParamLocation.PATH, description = "UUID of the upload session returned by the init endpoint")
    @ApiField(name = "id", in = ParamLocation.PATH, description = "Zero-based index of the chunk being uploaded")
    @AuthenticatedRoute
    @RequiresFeatures(value = Feature.FileManager, level = PermissionLevel.FULL)
    @Path("/files/upload/chunk/:uuid/:id")
    @Method(PUT)
    public Response uploadChunk(RawRequest request) {
        String uuid = request.getParameter("uuid");
        String chunkId = request.getParameter("id");
        File serverRoot = getServerRoot();
        File tempDirectory = new File(serverRoot, "uploads/" + uuid);

        if (!tempDirectory.exists() || !tempDirectory.isDirectory()) {
            return new JSONResponse().error("Upload session does not exist");
        }

        File chunkFile = new File(tempDirectory, "chunk_" + chunkId);

        try (InputStream input = request.getRequestBody();
             OutputStream output = Files.newOutputStream(chunkFile.toPath())) {

            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = input.read(buffer)) != -1) {
                output.write(buffer, 0, bytesRead);
            }
            return new JSONResponse().message("Chunk " + chunkId + " uploaded");
        } catch (Exception e) {
            return new JSONResponse().error("Error uploading chunk: " + e.getMessage());
        }
    }

    @ApiDoc(summary = "Finalize a chunked upload", description = "Merges all uploaded chunks of the given session into the destination file and cleans up the temporary upload directory.", tag = "File Manager")
    @ApiField(name = "uuid", description = "UUID of the upload session to finalize")
    @ApiField(name = "destinationPath", description = "Destination path for the assembled file, relative to the server root")
    @AuthenticatedRoute
    @RequiresFeatures(value = Feature.FileManager, level = PermissionLevel.FULL)
    @Path("/files/upload/stop")
    @Method(POST)
    public Response finalizeUpload(JSONRequest request) {
        request.checkFor("uuid", "destinationPath");
        File serverRoot = getServerRoot();
        String uuid = request.get("uuid");
        String destinationPath = request.get("destinationPath");

        File tempDirectory = new File(serverRoot, "uploads/" + uuid);
        File destinationFile;
        try {
            destinationFile = FileHelper.getNormalizedPath(serverRoot, destinationPath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if (!tempDirectory.exists() || !tempDirectory.isDirectory()) {
            return new JSONResponse().error("Upload session does not exist");
        }

        try (OutputStream output = Files.newOutputStream(destinationFile.toPath())) {
            File[] chunks = tempDirectory.listFiles((dir, name) -> name.startsWith("chunk_"));
            if (chunks == null || chunks.length == 0) {
                return new JSONResponse().error("No chunks found for upload");
            }

            Arrays.sort(chunks, (a, b) -> {
                int aId = Integer.parseInt(a.getName().split("_")[1]);
                int bId = Integer.parseInt(b.getName().split("_")[1]);
                return Integer.compare(aId, bId);
            });

            byte[] buffer = new byte[8192];
            for (File chunk : chunks) {
                try (InputStream input = Files.newInputStream(chunk.toPath())) {
                    int bytesRead;
                    while ((bytesRead = input.read(buffer)) != -1) {
                        output.write(buffer, 0, bytesRead);
                    }
                }
                if (!chunk.delete()) {
                    return new JSONResponse().error("Failed to delete chunk: " + chunk.getName());
                }
            }

            if (!tempDirectory.delete()) {
                return new JSONResponse().error("Failed to delete temporary upload directory");
            }

            return new JSONResponse().message("File uploaded successfully");
        } catch (Exception e) {
            return new JSONResponse().error("Error finalizing upload: " + e.getMessage());
        }
    }

    @ApiDoc(summary = "Copy a file", description = "Copies a file from a source path to a destination path. The destination must not already exist and the internal `voxeldash.db` file cannot be copied.", tag = "File Manager")
    @ApiField(name = "sourcePath", description = "Path of the file to copy, relative to the server root")
    @ApiField(name = "destinationPath", description = "Destination path for the copied file, relative to the server root")
    @AuthenticatedRoute
    @RequiresFeatures(value = Feature.FileManager, level = PermissionLevel.FULL)
    @Path("/files/copy")
    @Method(POST)
    public Response copyFile(JSONRequest request) {
        request.checkFor("sourcePath", "destinationPath");
        File serverRoot = getServerRoot();
        String sourcePath = request.get("sourcePath");
        String destinationPath = request.get("destinationPath");

        try {
            File sourceFile = FileHelper.getNormalizedPath(serverRoot, sourcePath);
            File destinationFile = FileHelper.getNormalizedPath(serverRoot, destinationPath);

            if (!sourceFile.exists() || sourceFile.isDirectory()) {
                return new JSONResponse().error("The source file does not exist");
            }

            if (sourceFile.getName().equals("voxeldash.db")) {
                return new JSONResponse().error("You are not allowed to copy the database file");
            }

            if (destinationFile.exists()) {
                return new JSONResponse().error("A file already exists at the destination path");
            }

            Files.copy(sourceFile.toPath(), destinationFile.toPath());

            return new JSONResponse().message("File copied successfully");
        } catch (Exception e) {
            return new JSONResponse().error("Error copying file: " + e.getMessage());
        }
    }

    @ApiDoc(summary = "Move a file", description = "Moves a file from a source path to a destination path. The destination must not already exist and the internal `voxeldash.db` file cannot be moved.", tag = "File Manager")
    @ApiField(name = "sourcePath", description = "Path of the file to move, relative to the server root")
    @ApiField(name = "destinationPath", description = "Destination path for the moved file, relative to the server root")
    @AuthenticatedRoute
    @RequiresFeatures(value = Feature.FileManager, level = PermissionLevel.FULL)
    @Path("/files/move")
    @Method(POST)
    public Response moveFile(JSONRequest request) {
        request.checkFor("sourcePath", "destinationPath");
        File serverRoot = getServerRoot();
        String sourcePath = request.get("sourcePath");
        String destinationPath = request.get("destinationPath");

        try {
            File sourceFile = FileHelper.getNormalizedPath(serverRoot, sourcePath);
            File destinationFile = FileHelper.getNormalizedPath(serverRoot, destinationPath);

            if (!sourceFile.exists() || sourceFile.isDirectory()) {
                return new JSONResponse().error("The source file does not exist");
            }

            if (sourceFile.getName().equals("voxeldash.db")) {
                return new JSONResponse().error("You are not allowed to move the database file");
            }

            if (destinationFile.exists()) {
                return new JSONResponse().error("A file already exists at the destination path");
            }

            Files.move(sourceFile.toPath(), destinationFile.toPath());

            return new JSONResponse().message("File moved successfully");
        } catch (Exception e) {
            return new JSONResponse().error("Error moving file: " + e.getMessage());
        }
    }


}