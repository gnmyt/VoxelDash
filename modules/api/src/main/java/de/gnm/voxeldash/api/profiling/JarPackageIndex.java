package de.gnm.voxeldash.api.profiling;

import java.io.File;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public final class JarPackageIndex {

    /**
     * First segments that are too generic to claim on their own, so a second
     * segment is included (e.g. {@code com.foo} rather than {@code com}).
     */
    private static final Set<String> TLD_ROOTS = new HashSet<>(Arrays.asList(
            "com", "net", "org", "io", "dev", "me", "xyz", "eu", "fr", "de",
            "gg", "app", "club", "co", "uk", "us", "tk", "ru", "pl", "nl",
            "info", "fun", "tech", "site", "online", "space", "world", "moe",
            "to", "cc", "ovh", "li", "id"
    ));

    private static final int MAX_ENTRIES = 20000;

    private JarPackageIndex() {
    }

    /**
     * Derives package prefixes from a jar's class entries.
     *
     * @param jar the jar file to scan
     * @return distinct claim prefixes (each without a trailing dot); empty on error
     */
    public static Set<String> derivePrefixes(File jar) {
        Set<String> prefixes = new HashSet<>();
        if (jar == null || !jar.isFile()) {
            return prefixes;
        }
        try (JarFile jarFile = new JarFile(jar)) {
            Enumeration<JarEntry> entries = jarFile.entries();
            int scanned = 0;
            while (entries.hasMoreElements() && scanned < MAX_ENTRIES) {
                JarEntry entry = entries.nextElement();
                String path = entry.getName();
                if (entry.isDirectory() || !path.endsWith(".class")) {
                    continue;
                }
                scanned++;
                String prefix = claimPrefix(path);
                if (prefix != null) {
                    prefixes.add(prefix);
                }
            }
        } catch (Exception ignored) {
        }
        return prefixes;
    }

    /**
     * Converts a class entry path (e.g. {@code com/foo/bar/Baz.class}) to a
     * claim prefix (e.g. {@code com.foo}). Default-package classes are ignored.
     */
    private static String claimPrefix(String classPath) {
        int lastSlash = classPath.lastIndexOf('/');
        if (lastSlash <= 0) {
            return null; // default package
        }
        String pkgPath = classPath.substring(0, lastSlash);
        String[] segments = pkgPath.split("/");
        if (segments.length == 0) {
            return null;
        }
        int depth = TLD_ROOTS.contains(segments[0]) ? 2 : 1;
        depth = Math.min(depth, segments.length);
        StringBuilder prefix = new StringBuilder();
        for (int i = 0; i < depth; i++) {
            if (i > 0) {
                prefix.append('.');
            }
            prefix.append(segments[i]);
        }
        return prefix.toString();
    }

    /**
     * Derives the package of a fully-qualified main class name, trimmed to a
     * claim prefix (e.g. {@code com.foo.MyPlugin} → {@code com.foo}).
     *
     * @param mainClass fully-qualified class name, may be {@code null}
     * @return the claim prefix, or {@code null} if not derivable
     */
    public static String mainClassPrefix(String mainClass) {
        if (mainClass == null || mainClass.isEmpty()) {
            return null;
        }
        int lastDot = mainClass.lastIndexOf('.');
        if (lastDot <= 0) {
            return null;
        }
        String pkg = mainClass.substring(0, lastDot);
        String[] segments = pkg.split("\\.");
        int depth = segments.length > 0 && TLD_ROOTS.contains(segments[0]) ? 2 : 1;
        depth = Math.min(depth, segments.length);
        StringBuilder prefix = new StringBuilder();
        for (int i = 0; i < depth; i++) {
            if (i > 0) {
                prefix.append('.');
            }
            prefix.append(segments[i]);
        }
        return prefix.toString();
    }
}
