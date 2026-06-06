package de.gnm.voxeldash.api.helper;

import de.gnm.voxeldash.api.entities.BackupPart;
import org.apache.commons.io.FileUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class BackupHelper {

    private final File backupFolder;

    /**
     * Basic constructor of the {@link BackupHelper}
     *
     * @param backupFolder The backup folder
     */
    public BackupHelper(File backupFolder) {
        this.backupFolder = backupFolder;
        if (!backupFolder.exists()) {
            backupFolder.mkdirs();
        }
    }

    /**
     * Gets a list of all directories that should be backed up
     *
     * @param backupBit The backup bit
     * @return the list of all directories that should be backed up
     */
    public ArrayList<File> getBackupDirectories(int backupBit) {
        ArrayList<File> directories = new ArrayList<>();

        for (BackupPart backupPart : BackupPart.fromBackupBit(backupBit)) {
            switch (backupPart) {
                case ROOT: {
                    File[] serverFolder = new File(".").listFiles();
                    if (serverFolder != null) {
                        directories.addAll(Arrays.asList(serverFolder));
                    }
                    break;
                }
                case WORLDS: {
                    File[] serverFolder = new File(".").listFiles();
                    if (serverFolder != null) {
                        for (File file : serverFolder) {
                            if (file.isDirectory() && new File(file, "level.dat").exists()) {
                                directories.add(file);
                            }
                        }
                    }
                    break;
                }
                case PLUGINS:
                    directories.add(new File("plugins"));
                    break;
                case MODS:
                    directories.add(new File("mods"));
                    break;
                case CONFIGS:
                    directories.addAll(FileUtils.listFiles(new File("."), new String[]{"yml", "properties", "json", "toml"}, false));
                    directories.addAll(Arrays.asList(new File("config"), new File("defaultconfigs")));
                    break;
                case LOGS:
                    directories.addAll(Arrays.asList(new File("logs"), new File("crash-reports")));
                    break;
            }
        }

        return directories;
    }

    /**
     * Checks if a backup exists
     *
     * @param name The time code of the backup
     * @return <code>true</code> if the backup exists, otherwise <code>false</code>
     */
    public boolean backupExists(String name) {
        return getBackup(name) != null;
    }

    /**
     * Zips a directory
     *
     * @param directory       The directory to zip
     * @param parent          The parent of the directory
     * @param zipOutputStream The zip output stream
     * @throws IOException An exception that can occur while executing the code
     */
    private void zipDirectory(File directory, String parent, ZipOutputStream zipOutputStream) throws IOException {
        if (directory.getName().equals(backupFolder.getName())) return;

        for (File file : Objects.requireNonNull(directory.listFiles())) {
            String entryName = parent + "/" + file.getName();
            if (file.isDirectory()) {
                zipDirectory(file, entryName, zipOutputStream);
            } else {
                zipFile(file, entryName, zipOutputStream);
            }
        }
    }

    /**
     * Writes a file to a zip output stream
     *
     * @param file            The file to write
     * @param entryName       The entry name of the file
     * @param zipOutputStream The zip output stream
     * @throws IOException An exception that can occur while executing the code
     */
    private void zipFile(File file, String entryName, ZipOutputStream zipOutputStream) throws IOException {
        try (InputStream inputStream = Files.newInputStream(file.toPath())) {
            zipOutputStream.putNextEntry(new ZipEntry(entryName));
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                zipOutputStream.write(buffer, 0, bytesRead);
            }
            zipOutputStream.closeEntry();
        }
    }

    /**
     * Creates a new backup
     *
     * @param modeSuffix The modes of the backup
     * @param name       An optional user-provided name (may contain placeholders such as {date}/{time});
     *                   may be <code>null</code> or empty for an unnamed backup
     * @param paths      The paths to back up
     * @throws IOException An exception that will be thrown if the backup could not be created
     */
    public void createBackup(String modeSuffix, String name, File... paths) throws IOException {
        if (isTempBackupCreated()) return;

        long timestamp = System.currentTimeMillis();
        String resolvedName = resolvePlaceholders(name, timestamp);
        String nameSuffix = resolvedName.isEmpty() ? "" : "-" + encodeName(resolvedName);
        String baseName = timestamp + "-" + modeSuffix + nameSuffix;

        File tempBackupFile = new File(backupFolder, baseName + "_tmp.zip");

        try (ZipOutputStream zipOutputStream = new ZipOutputStream(Files.newOutputStream(tempBackupFile.toPath()))) {
            for (File path : paths) {
                if (path.exists()) {
                    if (path.isDirectory()) {
                        zipDirectory(path, path.getName(), zipOutputStream);
                    } else {
                        zipFile(path, path.getName(), zipOutputStream);
                    }
                }
            }
        }

        File finalBackupFile = new File(backupFolder, baseName + ".zip");
        Files.move(tempBackupFile.toPath(), finalBackupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    /**
     * Creates a backup from a schedule action's metadata.
     * <p>
     * The metadata format is <code>bit</code> or <code>bit|namePattern</code>, where the optional name
     * pattern may contain placeholders (e.g. <code>{date}</code>, <code>{time}</code>) that are resolved
     * at creation time.
     *
     * @param metadata The schedule action metadata
     * @throws IOException An exception that will be thrown if the backup could not be created
     */
    public void createScheduledBackup(String metadata) throws IOException {
        int backupBit = 0;
        String namePattern = null;

        if (metadata != null && !metadata.isEmpty()) {
            String[] parts = metadata.split("\\|", 2);
            try {
                backupBit = Integer.parseInt(parts[0].trim());
            } catch (NumberFormatException ignored) {
            }
            if (parts.length > 1) {
                namePattern = parts[1];
            }
        }

        createBackup(String.valueOf(backupBit), namePattern, getBackupDirectories(backupBit).toArray(new File[0]));
    }

    /**
     * Resolves date/time placeholders in a backup name.
     *
     * @param name      The raw name (may be <code>null</code>)
     * @param timestamp The reference timestamp used to resolve the placeholders
     * @return the resolved name, or an empty string if no name was provided
     */
    private String resolvePlaceholders(String name, long timestamp) {
        if (name == null || name.isEmpty()) return "";

        Date date = new Date(timestamp);
        name = name.replace("{datetime}", new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(date));
        name = name.replace("{date}", new SimpleDateFormat("yyyy-MM-dd").format(date));
        name = name.replace("{time}", new SimpleDateFormat("HH-mm-ss").format(date));
        name = name.replace("{year}", new SimpleDateFormat("yyyy").format(date));
        name = name.replace("{month}", new SimpleDateFormat("MM").format(date));
        name = name.replace("{day}", new SimpleDateFormat("dd").format(date));
        name = name.replace("{hour}", new SimpleDateFormat("HH").format(date));
        name = name.replace("{minute}", new SimpleDateFormat("mm").format(date));
        return name.trim();
    }

    /**
     * Encodes a backup name as a hex string so it can be stored safely in the file name
     * (independent of the <code>-</code> separator and any OS-illegal characters).
     *
     * @param name The name to encode
     * @return the hex-encoded name
     */
    public static String encodeName(String name) {
        StringBuilder builder = new StringBuilder();
        for (byte b : name.getBytes(StandardCharsets.UTF_8)) {
            builder.append(String.format("%02x", b));
        }
        return builder.toString();
    }

    /**
     * Decodes a hex-encoded backup name produced by {@link #encodeName(String)}.
     *
     * @param hex The hex-encoded name
     * @return the decoded name, or an empty string if the input is not valid hex
     */
    public static String decodeName(String hex) {
        if (hex == null || hex.isEmpty() || hex.length() % 2 != 0) return "";
        try {
            byte[] bytes = new byte[hex.length() / 2];
            for (int i = 0; i < hex.length(); i += 2) {
                bytes[i / 2] = (byte) Integer.parseInt(hex.substring(i, i + 2), 16);
            }
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (NumberFormatException e) {
            return "";
        }
    }

    /**
     * Deletes a backup
     *
     * @param name The name of the backup
     * @throws IOException An exception that will be thrown if the backup could not be deleted
     */
    public void deleteBackup(String name) throws IOException {
        File backup = getBackup(name);
        if (backup != null) {
            Files.deleteIfExists(backup.toPath());
        }
    }

    /**
     * Checks if a temporary backup is created
     *
     * @return <code>true</code> if a temporary backup is created, otherwise <code>false</code>
     */
    public boolean isTempBackupCreated() {
        File[] tempBackupFiles = backupFolder.listFiles(file -> file.getName().endsWith("_tmp.zip"));
        return tempBackupFiles != null && tempBackupFiles.length > 0;
    }

    /**
     * Clears all old folders in order to prevent conflicts
     *
     * @param stream The stream of the backup
     * @throws IOException An exception that will be thrown if the old folders could not be cleared
     */
    private void clearOldFolders(FileInputStream stream) throws IOException {
        try (ZipInputStream zipInputStream = new ZipInputStream(stream)) {
            ZipEntry zipEntry;
            while ((zipEntry = zipInputStream.getNextEntry()) != null) {
                File parentDir = new File(zipEntry.getName()).getParentFile();
                if (parentDir != null && parentDir.exists() && !parentDir.equals(new File("."))) {
                    FileUtils.deleteDirectory(parentDir);
                }
            }
        }
    }

    /**
     * Restores a backup
     *
     * @param name             The name of the backup
     * @param haltAfterRestore <code>true</code> if the server should be halted after the restore, otherwise <code>false</code>
     */
    public void restoreBackup(String name, boolean haltAfterRestore) {
        if (!backupExists(name)) return;

        File backup = getBackup(name);
        if (backup == null) return;

        try (ZipInputStream zipInputStream = new ZipInputStream(Files.newInputStream(backup.toPath()))) {
            clearOldFolders(new FileInputStream(backup));

            ZipEntry zipEntry;
            byte[] buffer = new byte[4096];
            while ((zipEntry = zipInputStream.getNextEntry()) != null) {
                File newFile = new File(zipEntry.getName());
                if (newFile.getParentFile() != null) {
                    newFile.getParentFile().mkdirs();
                }
                try (FileOutputStream fos = new FileOutputStream(newFile)) {
                    int len;
                    while ((len = zipInputStream.read(buffer)) > 0) {
                        fos.write(buffer, 0, len);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (haltAfterRestore) {
            Runtime.getRuntime().halt(0);
        }
    }

    /**
     * Gets a list of all backups
     *
     * @return a list of all backups
     */
    public ArrayList<File> getBackups() {
        File[] backupFiles = FileUtils.listFiles(backupFolder, new String[]{"zip"}, true).toArray(new File[0]);
        Arrays.sort(backupFiles);
        return new ArrayList<>(Arrays.asList(backupFiles));
    }

    /**
     * Gets a backup
     *
     * @param name The name of the backup
     * @return the backup
     */
    public File getBackup(String name) {
        File[] backupFiles = backupFolder.listFiles(file -> file.getName().startsWith(name + "-") && file.getName().endsWith(".zip"));
        return (backupFiles == null || backupFiles.length == 0) ? null : backupFiles[0];
    }
}