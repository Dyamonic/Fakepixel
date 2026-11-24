package club.abelix.games.fakevival.smpworlds;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.File;

public final class SMPWorldContainer {
    private static final String WORLD_PREFIX = "world/";

    public SMPWorldData pack(String worldId, Path sourceWorldFolder) throws IOException {
        if (!Files.isDirectory(sourceWorldFolder)) {
            throw new IOException("World folder does not exist: " + sourceWorldFolder);
        }
        byte[] bytes;
        try (ByteArrayOutputStream output = new ByteArrayOutputStream();
             ZipOutputStream zip = new ZipOutputStream(output, StandardCharsets.UTF_8)) {
            try (var stream = Files.walk(sourceWorldFolder)) {
                for (Path path : stream.filter(Files::isRegularFile).toList()) {
                    String name = sourceWorldFolder.relativize(path).toString().replace('\\', '/');
                    if (skipRuntimeFile(name)) continue;
                    zip.putNextEntry(new ZipEntry(WORLD_PREFIX + name));
                    Files.copy(path, zip);
                    zip.closeEntry();
                }
            }
            zip.finish();
            bytes = output.toByteArray();
        }
        return new SMPWorldData(worldId, 1, "ZIP", bytes, sha256(bytes), bytes.length, System.currentTimeMillis());
    }

    /** Reads an uploaded SMP archive without extracting it. */
    public ImportedContainer inspect(String worldId, Path source) throws IOException {
        byte[] bytes = Files.readAllBytes(source);
        String metaJson = null;
        boolean containsSlime = false;
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(bytes), StandardCharsets.UTF_8)) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                String name = entry.getName().replace('\\', '/');
                if (name.equalsIgnoreCase("meta.json") || name.endsWith("/meta.json")) {
                    metaJson = new String(zip.readAllBytes(), StandardCharsets.UTF_8);
                }
                if (name.equalsIgnoreCase("source.slime") || name.endsWith("/source.slime")) containsSlime = true;
            }
        } catch (IOException exception) {
            throw new IOException("Invalid .smp container: " + source.getFileName(), exception);
        }

        String sourceFormat = jsonString(metaJson, "source_format", "sourceFormat");
        String sourceFile = jsonString(metaJson, "source_file", "sourceFile", "source");
        boolean slime = containsSlime || "slime".equalsIgnoreCase(sourceFormat)
                || (sourceFile != null && sourceFile.toLowerCase(java.util.Locale.ROOT).endsWith(".slime"));
        String displayName = jsonString(metaJson, "display_name", "displayName", "name");
        String worldType = jsonString(metaJson, "world_type", "worldType", "type");
        String environment = jsonString(metaJson, "environment");
        boolean dungeonTemplate = "DUNGEON_TEMPLATE".equalsIgnoreCase(worldType)
                || Boolean.TRUE.equals(jsonBoolean(metaJson, "dungeon_template", "dungeonTemplate"));
        return new ImportedContainer(new SMPWorldData(worldId, 1, "SMP", bytes, sha256(bytes), bytes.length,
                System.currentTimeMillis()), displayName, dungeonTemplate ? "DUNGEON_TEMPLATE" : worldType,
                environment, slime ? "SLIME" : "SMP", slime);
    }

    /** Creates an SMP zip container around a Slime world without writing any temporary files. */
    public SMPWorldData wrapSlime(String worldId, File slimeFile, boolean template) throws IOException {
        byte[] slimeBytes = Files.readAllBytes(slimeFile.toPath());
        long now = System.currentTimeMillis();
        String worldType = template ? "DUNGEON_TEMPLATE" : "NORMAL";
        String metaJson = "{\n"
                + "  \"world_id\": \"" + jsonEscape(worldId) + "\",\n"
                + "  \"display_name\": \"" + jsonEscape(worldId) + "\",\n"
                + "  \"format_version\": 1,\n"
                + "  \"source_format\": \"SLIME\",\n"
                + "  \"requires_bridge\": true,\n"
                + "  \"template\": " + template + ",\n"
                + "  \"world_type\": \"" + worldType + "\",\n"
                + "  \"original_file_name\": \"" + jsonEscape(slimeFile.getName()) + "\",\n"
                + "  \"original_size_bytes\": " + slimeBytes.length + ",\n"
                + "  \"created_at\": " + now + ",\n"
                + "  \"updated_at\": " + now + "\n"
                + "}\n";
        String readme = "This SMP container wraps a Slime world.\n"
                + "source_format=SLIME\nrequires_bridge=true\n"
                + "Load it through the SlimeWorldManager/AdvancedSlimePaper bridge, not as a Paper world folder.\n";
        byte[] bytes;
        try (ByteArrayOutputStream output = new ByteArrayOutputStream();
             ZipOutputStream zip = new ZipOutputStream(output, StandardCharsets.UTF_8)) {
            putEntry(zip, "meta.json", metaJson.getBytes(StandardCharsets.UTF_8));
            putEntry(zip, "data/source.slime", slimeBytes);
            putEntry(zip, "README.txt", readme.getBytes(StandardCharsets.UTF_8));
            zip.finish();
            bytes = output.toByteArray();
        }
        return new SMPWorldData(worldId, 1, "SMP", bytes, sha256(bytes), bytes.length, now);
    }

    private void putEntry(ZipOutputStream zip, String name, byte[] bytes) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(bytes);
        zip.closeEntry();
    }

    private String jsonEscape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private String jsonString(String json, String... keys) {
        if (json == null) return null;
        for (String key : keys) {
            Matcher matcher = Pattern.compile("\\\"" + Pattern.quote(key) + "\\\"\\s*:\\s*\\\"([^\\\"]*)\\\"",
                    Pattern.CASE_INSENSITIVE).matcher(json);
            if (matcher.find()) return matcher.group(1);
        }
        return null;
    }

    private Boolean jsonBoolean(String json, String... keys) {
        if (json == null) return null;
        for (String key : keys) {
            Matcher matcher = Pattern.compile("\\\"" + Pattern.quote(key) + "\\\"\\s*:\\s*(true|false)",
                    Pattern.CASE_INSENSITIVE).matcher(json);
            if (matcher.find()) return Boolean.parseBoolean(matcher.group(1));
        }
        return null;
    }

    public record ImportedContainer(SMPWorldData data, String displayName, String worldType, String environment,
                                    String sourceFormat, boolean requiresBridge) {
    }

    public void extract(SMPWorldData data, Path runtimeFolder) throws IOException {
        deleteDirectory(runtimeFolder);
        Files.createDirectories(runtimeFolder);
        try (InputStream input = new ByteArrayInputStream(data.data());
             ZipInputStream zip = new ZipInputStream(input, StandardCharsets.UTF_8)) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                String name = entry.getName();
                if (!name.startsWith(WORLD_PREFIX) || entry.isDirectory()) continue;
                String relative = name.substring(WORLD_PREFIX.length());
                if (skipRuntimeFile(relative)) continue;
                Path target = runtimeFolder.resolve(relative).normalize();
                if (!target.startsWith(runtimeFolder.normalize())) {
                    throw new IOException("Unsafe path inside SMP world container: " + name);
                }
                Files.createDirectories(target.getParent());
                Files.copy(zip, target, StandardCopyOption.REPLACE_EXISTING);
            }
        }
        Files.deleteIfExists(runtimeFolder.resolve("uid.dat"));
        Files.deleteIfExists(runtimeFolder.resolve("session.lock"));
    }

    public void deleteDirectory(Path folder) throws IOException {
        if (folder == null || !Files.exists(folder)) return;
        try (var stream = Files.walk(folder)) {
            for (Path path : stream.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(path);
            }
        }
    }

    private boolean skipRuntimeFile(String relative) {
        String normalized = relative.replace('\\', '/');
        return normalized.equals("session.lock") || normalized.equals("uid.dat")
                || normalized.startsWith("playerdata/") || normalized.startsWith("players/")
                || normalized.startsWith("advancements/") || normalized.startsWith("stats/");
    }

    private String sha256(byte[] bytes) throws IOException {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException exception) {
            throw new IOException("SHA-256 is unavailable", exception);
        }
    }
}
