package eu.fordo.mailadmin.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Single source of truth for /etc/mailadmin/config.properties — DB, RabbitMQ, and Sentry
 * settings all live in this one file. DB settings are edited directly on the host only
 * (the app can't rewrite its own DB credentials before it can even connect). RabbitMQ
 * settings can also be updated from the admin UI via update(), which rewrites just the
 * matching lines in place — preserving comments and untouched keys — rather than using
 * Properties.store(), which would silently drop the file's comments.
 */
public final class AppConfig {

    private static final Path CONFIG_PATH = Path.of("/etc/mailadmin/config.properties");
    private static volatile Properties props = load();

    private AppConfig() {}

    private static Properties load() {
        Properties p = new Properties();
        if (Files.isReadable(CONFIG_PATH)) {
            try (InputStream in = Files.newInputStream(CONFIG_PATH)) {
                p.load(in);
            } catch (IOException e) {
                throw new RuntimeException("Failed to read " + CONFIG_PATH, e);
            }
        }
        return p;
    }

    public static synchronized void reload() {
        props = load();
    }

    public static String get(String key, String defaultValue) {
        return props.getProperty(key, defaultValue);
    }

    public static int getInt(String key, int defaultValue) {
        String v = props.getProperty(key);
        return v == null || v.isBlank() ? defaultValue : Integer.parseInt(v.trim());
    }

    public static boolean isConfigFilePresent() {
        return Files.isReadable(CONFIG_PATH);
    }

    /** Drives the setup-wizard gate: true only once real DB credentials have been written. */
    public static boolean hasDbCredentials() {
        return !get("db.url", "").isBlank() && !get("db.user", "").isBlank() && !get("db.password", "").isBlank();
    }

    public static String configFilePath() {
        return CONFIG_PATH.toString();
    }

    /**
     * Rewrites only the given keys, line by line, leaving comments, blank lines, and every
     * other key untouched. Keys not already present in the file are appended at the end.
     * Writes to a temp file in the same directory and renames atomically.
     */
    public static synchronized void update(Map<String, String> updates) throws IOException {
        List<String> existingLines = Files.isReadable(CONFIG_PATH)
                ? Files.readAllLines(CONFIG_PATH, StandardCharsets.UTF_8)
                : new ArrayList<>();

        Map<String, String> remaining = new LinkedHashMap<>(updates);
        List<String> newLines = new ArrayList<>();
        for (String line : existingLines) {
            String trimmed = line.trim();
            boolean replaced = false;
            if (!trimmed.isEmpty() && !trimmed.startsWith("#")) {
                int eq = trimmed.indexOf('=');
                if (eq > 0) {
                    String key = trimmed.substring(0, eq).trim();
                    if (remaining.containsKey(key)) {
                        newLines.add(key + "=" + remaining.remove(key));
                        replaced = true;
                    }
                }
            }
            if (!replaced) {
                newLines.add(line);
            }
        }
        for (Map.Entry<String, String> leftover : remaining.entrySet()) {
            newLines.add(leftover.getKey() + "=" + leftover.getValue());
        }

        if (CONFIG_PATH.getParent() != null && !Files.isDirectory(CONFIG_PATH.getParent())) {
            Files.createDirectories(CONFIG_PATH.getParent()); // best-effort; setup wizard instructs creating this ahead of time too
        }
        Path tmp = CONFIG_PATH.resolveSibling(CONFIG_PATH.getFileName() + ".tmp");
        Files.write(tmp, newLines, StandardCharsets.UTF_8);
        Files.move(tmp, CONFIG_PATH, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        reload();
    }
}
