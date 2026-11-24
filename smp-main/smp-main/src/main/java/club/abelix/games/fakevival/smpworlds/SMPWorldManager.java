package club.abelix.games.fakevival.smpworlds;

import club.abelix.games.fakevival.data.DataManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.entity.Player;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class SMPWorldManager {
    private static final Set<String> DEFAULT_WORLDS = Set.of("hub", "world", "world_nether", "world_the_end");
    private final JavaPlugin plugin;
    private final DataManager dataManager;
    private final SMPWorldRepository repository;
    private final SMPWorldContainer container = new SMPWorldContainer();
    private final SMPWorldTemplateService templateService;
    private final Path runtimeRoot;
    private final Path importRoot;
    private final ConcurrentMap<String, World> loadedWorlds = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, SMPWorldMetadata> loadedMetadata = new ConcurrentHashMap<>();

    public SMPWorldManager(JavaPlugin plugin, DataManager dataManager) {
        this.plugin = plugin;
        this.dataManager = dataManager;
        this.repository = new DatabaseSMPWorldRepository(dataManager);
        this.templateService = new SMPWorldTemplateService(repository);
        this.runtimeRoot = plugin.getServer().getWorldContainer().toPath().resolve("smp_runtime").normalize();
        this.importRoot = plugin.getServer().getWorldContainer().toPath().resolve("smp_imports").normalize();
    }

    public void enable() {
        try {
            Files.createDirectories(runtimeRoot);
            Files.createDirectories(importRoot);
            if (repository.isAvailable()) repository.initialize();
            plugin.getLogger().info("[SMPWorlds] Enabled. database=" + repository.isAvailable() + " runtime=" + runtimeRoot);
            if (repository.isAvailable()) async(repository::countWorlds).thenAccept(count ->
                    plugin.getLogger().info("[SMP] Loaded " + count + " SMPWorld templates from database."));
        } catch (Exception exception) {
            plugin.getLogger().severe("[SMPWorlds] Could not initialize database-backed storage: " + exception.getMessage());
        }
    }

    public void disable() {
        for (String worldId : new ArrayList<>(loadedWorlds.keySet())) {
            World world = loadedWorlds.remove(worldId);
            loadedMetadata.remove(worldId);
            if (world != null) Bukkit.unloadWorld(world, false);
            tryUpdateState(worldId, SMPWorldState.UNLOADED);
        }
    }

    public Path runtimePath() { return runtimeRoot; }
    public Path importPath() { return importRoot; }
    public boolean databaseAvailable() { return repository.isAvailable(); }

    public void ensureHubWorldLoaded() {
        if (Bukkit.getWorld("hub") != null) return;
        async(() -> repository.isAvailable() && repository.findMetadata("hub").isPresent()).thenAccept(databaseWorld -> {
            if (databaseWorld) {
                loadWorld("hub").exceptionally(error -> { plugin.getLogger().severe("[SMPWorlds] Failed to load database-backed hub world: " + error.getMessage()); return null; });
                return;
            }
            main(() -> {
                Path folder=plugin.getServer().getWorldContainer().toPath().resolve("hub").normalize();
                if (!folder.startsWith(plugin.getServer().getWorldContainer().toPath().normalize()) || !Files.isDirectory(folder)) {
                    plugin.getLogger().info("OVERWORLD server has no local hub world. This is okay; /warp hub will route players to hub servers.");
                    return null;
                }
                World world=Bukkit.createWorld(new WorldCreator("hub"));
                if(world==null)plugin.getLogger().severe("[SMP] Bukkit failed to load the normal hub world folder.");
                return world;
            });
        });
    }

    public CompletableFuture<String> dbStatus() {
        return async(() -> "Database=" + (repository.isAvailable() ? "connected" : "unavailable")
                + ", worlds=" + (repository.isAvailable() ? repository.countWorlds() : 0)
                + ", loadedRuntime=" + loadedWorlds.size());
    }

    public CompletableFuture<String> importWorld(String vanillaWorldFolder, String worldId, boolean template) {
        String validation = validateWorldId(worldId);
        if (validation != null) return failed(validation);
        Path source = resolveWorldFolder(vanillaWorldFolder);
        return async(() -> {
            requireDatabase();
            rejectExistingImport(worldId);
            SMPWorldData data = container.pack(worldId, source);
            repository.insertWorld(SMPWorldMetadata.create(worldId, template, null), data);
            logImportRows(worldId);
            plugin.getLogger().info("[SMPWorlds] Imported " + source + " into database world " + worldId);
            return "Imported " + worldId + " into the SMP database. template=" + template + ".";
        });
    }

    public CompletableFuture<String> importFile(String fileName, String worldId, boolean template) {
        String validation = validateWorldId(worldId);
        if (validation != null) return failed(validation);
        if (fileName == null || fileName.isBlank() || !fileName.matches("[A-Za-z0-9][A-Za-z0-9._-]*\\.smp")) {
            return failed("File not found: smp_imports/" + fileName);
        }
        Path source = importRoot.resolve(fileName).normalize();
        if (!source.startsWith(importRoot) || !Files.isRegularFile(source)) {
            return failed("File not found: smp_imports/" + fileName);
        }
        return async(() -> {
            requireDatabase();
            rejectExistingImport(worldId);
            SMPWorldContainer.ImportedContainer imported = container.inspect(worldId, source);
            World.Environment environment = parseEnvironment(imported.environment());
            String worldType = imported.worldType() == null || imported.worldType().isBlank()
                    ? "NORMAL" : imported.worldType().toUpperCase(Locale.ROOT);
            SMPWorldMetadata metadata = SMPWorldMetadata.create(worldId, template, null)
                    .withImportDetails(imported.displayName(), template, worldType, environment,
                            imported.sourceFormat(), imported.requiresBridge());
            repository.insertWorld(metadata, imported.data());
            logImportRows(worldId);
            plugin.getLogger().info("[SMPWorlds] Imported container " + source + " into database world " + worldId
                    + " source_format=" + imported.sourceFormat() + " requires_bridge=" + imported.requiresBridge());
            return "Imported SMPWorld " + worldId + " to database. Template: " + template;
        });
    }

    public CompletableFuture<SMPWorldImportResult> importSlimeFile(File slimeFile, String worldId, boolean template) {
        String validation = validateWorldId(worldId);
        if (validation != null) return CompletableFuture.completedFuture(SMPWorldImportResult.failure(worldId, validation));
        String fileName = slimeFile == null ? "" : slimeFile.getName();
        if (!fileName.toLowerCase(Locale.ROOT).endsWith(".slime")) {
            return CompletableFuture.completedFuture(SMPWorldImportResult.failure(worldId, "That file is not a .slime file."));
        }
        Path source = slimeFile.toPath().toAbsolutePath().normalize();
        Path allowedRoot = importRoot.toAbsolutePath().normalize();
        if (!source.startsWith(allowedRoot) || !Files.isRegularFile(source)) {
            return CompletableFuture.completedFuture(SMPWorldImportResult.failure(worldId,
                    "File not found: smp_imports/" + fileName));
        }
        return async(() -> {
            requireDatabase();
            plugin.getLogger().info("[SMPWorlds] Import check worldId=" + worldId);
            if (repository.exists(worldId)) {
                plugin.getLogger().info("[SMPWorlds] Import blocked; worldId already exists=true worldId=" + worldId);
                return SMPWorldImportResult.failure(worldId, "A SMPWorld with id " + worldId + " already exists.");
            }
            plugin.getLogger().info("[SMPWorlds] Import worldId already exists=false worldId=" + worldId);
            SMPWorldData data = container.wrapSlime(worldId, source.toFile(), template);
            String worldType = template ? "DUNGEON_TEMPLATE" : "NORMAL";
            SMPWorldMetadata metadata = SMPWorldMetadata.create(worldId, template, null)
                    .withImportDetails(worldId, template, worldType, World.Environment.NORMAL, "SLIME", true);
            repository.insertWorld(metadata, data);
            logImportRows(worldId);
            plugin.getLogger().info("[SMPWorlds] Imported Slime file " + source + " as " + worldId
                    + ". container_bytes=" + data.sizeBytes());
            return new SMPWorldImportResult(true, worldId, "Imported", data.sizeBytes(), "SLIME");
        });
    }

    public CompletableFuture<SMPWorld> loadWorld(String worldId) {
        String validation = validateWorldId(worldId);
        if (validation != null) return failedWorld(validation);
        World managed = loadedWorlds.get(worldId);
        if (managed == null) managed = Bukkit.getWorld(bukkitWorldName(worldId));
        if (managed != null) {
            return CompletableFuture.completedFuture(snapshot(worldId, SMPWorldState.LOADED, managed));
        }
        return async(() -> {
            requireDatabase();
            SMPWorldMetadata metadata = repository.findMetadata(worldId)
                    .orElseThrow(() -> new IOException("SMPWorld " + worldId + " does not exist in the database."));
            if (metadata.requiresBridge()) {
                throw new IOException("World " + worldId + " requires the SlimeWorldManager/AdvancedSlimePaper bridge and cannot be loaded as a Paper folder.");
            }
            SMPWorldData data = repository.findData(worldId).orElseThrow(() -> new IOException("No world data exists for " + worldId));
            container.extract(data, runtimeFolder(worldId));
            return metadata;
        }).thenCompose(metadata -> main(() -> {
            World world = Bukkit.getWorld(bukkitWorldName(worldId));
            if (world == null) {
                WorldCreator creator = new WorldCreator(bukkitWorldName(worldId));
                creator.environment(metadata.environment());
                if (metadata.seed() != null) creator.seed(metadata.seed());
                world = Bukkit.createWorld(creator);
            }
            if (world == null) throw new IOException("Bukkit could not load runtime world " + worldId);
            loadedWorlds.put(worldId, world);
            loadedMetadata.put(worldId, metadata.withState(SMPWorldState.LOADED));
            repository.updateState(worldId, SMPWorldState.LOADED);
            plugin.getLogger().info("[SMPWorlds] Loaded database world " + worldId + " as " + world.getName());
            return new SMPWorld(worldId, metadata.withState(SMPWorldState.LOADED), SMPWorldState.LOADED,
                    null, runtimeFolder(worldId), world);
        }));
    }

    public CompletableFuture<String> unloadWorld(String worldId, boolean save) {
        String validation = validateWorldId(worldId);
        if (validation != null) return failed(validation);
        return main(() -> {
            World world = getBukkitWorld(worldId);
            if (world == null) return "not-loaded";
            movePlayersToSpawn(world);
            if (save) world.save();
            if (!Bukkit.unloadWorld(world, save)) throw new IOException("Bukkit refused to unload world " + worldId);
            loadedWorlds.remove(worldId);
            loadedMetadata.remove(worldId);
            repository.updateState(worldId, SMPWorldState.UNLOADED);
            return save ? "save" : "no-save";
        }).thenCompose(result -> {
            if ("not-loaded".equals(result)) return CompletableFuture.completedFuture("World " + worldId + " is not loaded.");
            if (!"save".equals(result)) return CompletableFuture.completedFuture("Unloaded " + worldId + " without saving to the database.");
            return async(() -> {
                requireDatabase();
                return repository.findMetadata(worldId).orElse(SMPWorldMetadata.create(worldId, false, null));
            }).thenCompose(metadata -> persistRuntimeWorldToDatabase(worldId, metadata.withState(SMPWorldState.UNLOADED))
                    .thenApply(ignored -> "Unloaded and saved " + worldId + " to the database."));
        });
    }

    public CompletableFuture<String> saveWorldToDatabase(String worldId) {
        return saveWorldToDatabase(worldId, false, false, null, null, null);
    }

    public CompletableFuture<String> saveWorldToDatabase(String worldId, boolean force, boolean dungeonTemplate) {
        return saveWorldToDatabase(worldId, force, dungeonTemplate, null, null, null);
    }

    public CompletableFuture<String> saveWorldToDatabase(String worldId, boolean force, boolean dungeonTemplate,
                                                          String savedByUuid, String savedByName, String linkedDungeonId) {
        String validation = validateWorldId(worldId);
        if (validation != null) return failed(validation);
        return main(() -> {
            World world = findLoadedWorld(worldId);
            if (world == null) throw new WorldNotLoadedException(worldId);
            if (isDungeonInstanceWorld(world)) throw new IOException("Active dungeon instances cannot be saved as templates.");
            world.save();
            return world;
        }).thenCompose(world -> async(() -> {
            requireDatabase();
            SMPWorldMetadata existing = repository.findMetadata(worldId).orElse(null);
            if (existing != null && (existing.temporary() || "DUNGEON_INSTANCE".equalsIgnoreCase(existing.worldType())))
                throw new IOException("Active dungeon instances cannot be saved as templates.");
            Path source = sourceFolder(worldId, world);
            if (!force && existing != null && repository.findData(worldId).isPresent() && worldLooksEmpty(source)) {
                throw new EmptyWorldOverwriteException(worldId);
            }
            SMPWorldMetadata metadata = existing == null
                    ? SMPWorldMetadata.fromLoadedWorld(worldId, world, dungeonTemplate)
                    : existing.refreshedFrom(world, dungeonTemplate);
            SMPWorldData data = container.pack(worldId, source);
            repository.saveWorld(metadata, data);
            repository.updateSaveMetadata(worldId, savedByUuid, savedByName, linkedDungeonId);
            loadedWorlds.put(worldId, world);
            loadedMetadata.put(worldId, metadata);
            plugin.getLogger().info("[SMPWorlds] Saved loaded world " + world.getName() + " as " + worldId
                    + " to database. bytes=" + data.sizeBytes());
            return "Saved SMPWorld " + worldId + " to the database.";
        }));
    }

    public CompletableFuture<Boolean> savedWorldExists(String worldId) {
        return async(() -> repository.isAvailable() && repository.findMetadata(worldId).isPresent()
                && repository.findData(worldId).isPresent());
    }

    public CompletableFuture<SMPWorldSaveResult> saveLoadedWorldsToDatabase() {
        List<String> worldIds = new ArrayList<>(loadedWorlds.keySet());
        return async(() -> {
            requireDatabase();
            SavePlan plan = new SavePlan();
            for (String worldId : worldIds) {
                try {
                    SMPWorldMetadata metadata = repository.findMetadata(worldId).orElse(null);
                    if (metadata == null) {
                        skip(plan, worldId, "metadata missing");
                        continue;
                    }
                    String skipReason = hubsaveSkipReason(metadata);
                    if (skipReason != null) {
                        skip(plan, worldId, skipReason);
                        continue;
                    }
                    plan.targets.add(new WorldSaveTarget(worldId, metadata.withState(SMPWorldState.LOADED)));
                } catch (Exception exception) {
                    fail(plan, worldId, exception);
                }
            }
            return plan;
        }).thenCompose(plan -> main(() -> {
            for (WorldSaveTarget target : plan.targets) {
                try {
                    World world = getBukkitWorld(target.worldId());
                    if (world == null) {
                        plan.failed.add(target.worldId() + ": not loaded");
                        plugin.getLogger().warning("[SMPWorlds] Hubsave failed for " + target.worldId() + ": not loaded");
                        continue;
                    }
                    plugin.getLogger().info("[SMPWorlds] Hubsave saving Bukkit world " + target.worldId() + ".");
                    world.save();
                    plan.savedOnMainThread.add(target);
                } catch (Exception exception) {
                    fail(plan, target.worldId(), exception);
                }
            }
            return plan;
        })).thenCompose(plan -> async(() -> {
            for (WorldSaveTarget target : plan.savedOnMainThread) {
                try {
                    SMPWorldData data = container.pack(target.worldId(), runtimeFolder(target.worldId()));
                    repository.saveWorld(target.metadata(), data);
                    plan.saved.add(target.worldId());
                    plugin.getLogger().info("[SMPWorlds] Hubsave wrote " + target.worldId()
                            + " to database. bytes=" + data.sizeBytes()
                            + " checksum=" + (data.checksum() == null ? "none" : data.checksum()));
                } catch (Exception exception) {
                    fail(plan, target.worldId(), exception);
                }
            }
            return new SMPWorldSaveResult(List.copyOf(plan.saved), List.copyOf(plan.failed), List.copyOf(plan.skipped));
        }));
    }

    public CompletableFuture<String> cloneWorld(String templateId, String newWorldId) {
        String validation = validateWorldId(templateId);
        if (validation != null) return failed(validation);
        validation = validateWorldId(newWorldId);
        if (validation != null) return failed(validation);
        return async(() -> {
            requireDatabase();
            templateService.cloneTemplate(templateId, newWorldId);
            plugin.getLogger().info("[SMPWorlds] Cloned database world " + templateId + " to " + newWorldId);
            return "Cloned " + templateId + " to " + newWorldId + " in the database.";
        });
    }

    public CompletableFuture<String> deleteWorld(String worldId) {
        String validation = validateWorldId(worldId);
        if (validation != null) return failed(validation);
        if (DEFAULT_WORLDS.contains(worldId.toLowerCase(Locale.ROOT))) return failed("Refusing to delete default server world " + worldId + ".");
        if (isLoaded(worldId)) return failed("Refusing to delete loaded world " + worldId + ".");
        return async(() -> {
            requireDatabase();
            repository.deleteWorld(worldId);
            container.deleteDirectory(runtimeFolder(worldId));
            plugin.getLogger().info("[SMPWorlds] Deleted database world " + worldId);
            return "Deleted " + worldId + " from the database and runtime cache.";
        });
    }

    public CompletableFuture<SMPWorldInstance> createDungeonInstance(String templateId) {
        String instanceId = "dungeon_" + templateId + "_" + System.currentTimeMillis();
        return cloneWorld(templateId, instanceId)
                .thenCompose(ignored -> async(() -> {
                    SMPWorldMetadata metadata = repository.findMetadata(instanceId)
                            .orElseThrow(() -> new SQLException("Instance metadata not found: " + instanceId));
                    repository.saveMetadata(metadata.asTemporaryDungeonInstance());
                    repository.insertInstance(instanceId, templateId, instanceId, null, null, null, null);
                    return instanceId;
                }))
                .thenCompose(id -> loadWorld(id).thenApply(world -> new SMPWorldInstance(id, world.worldId(), world.bukkitWorld())));
    }

    public CompletableFuture<String> createVoidWorld(String worldId) {
        String validation = validateWorldId(worldId);
        if (validation != null) return failed(validation);
        return main(() -> {
            if (isLoaded(worldId)) throw new IOException("World is already loaded: " + worldId);
            WorldCreator creator = new WorldCreator(bukkitWorldName(worldId));
            creator.generator(new VoidGenerator());
            World world = Bukkit.createWorld(creator);
            if (world == null) throw new IOException("Bukkit could not create void world " + worldId);
            world.getBlockAt(0, 64, 0).setType(Material.BEDROCK);
            world.setSpawnLocation(new Location(world, 0.5D, 65.0D, 0.5D));
            world.save();
            loadedWorlds.put(worldId, world);
            return worldId;
        }).thenCompose(id -> async(() -> {
            requireDatabase();
            repository.saveWorld(SMPWorldMetadata.create(worldId, false, null).withState(SMPWorldState.LOADED),
                    container.pack(worldId, runtimeFolder(worldId)));
            plugin.getLogger().info("[SMPWorlds] Created void database world " + worldId);
            return "Created void SMP world " + worldId + " with one bedrock block at y=64.";
        }));
    }

    public CompletableFuture<List<String>> listSummariesAsync() {
        return async(() -> {
            requireDatabase();
            List<String> out = new ArrayList<>();
            for (SMPWorldMetadata metadata : repository.listWorlds()) {
                String loaded = loadedWorlds.containsKey(metadata.worldId()) ? "loaded" : metadata.state().name().toLowerCase(Locale.ROOT);
                out.add(metadata.worldId() + " [" + loaded + ", template=" + metadata.template() + "]");
            }
            plugin.getLogger().info("[SMPWorlds] /smpworlds list count=" + out.size());
            if (out.isEmpty()) out.add("No database worlds found.");
            return out;
        });
    }

    public CompletableFuture<String> info(String worldId) {
        String validation = validateWorldId(worldId);
        if (validation != null) return failed(validation);
        return async(() -> {
            requireDatabase();
            SMPWorldMetadata metadata = repository.findMetadata(worldId).orElseThrow(() -> new IOException("World not found: " + worldId));
            SMPWorldData data = repository.findData(worldId).orElse(null);
            return "worldId=" + metadata.worldId()
                    + ", displayName=" + metadata.displayName()
                    + ", template=" + metadata.template()
                    + ", parentTemplate=" + (metadata.parentTemplate() == null ? "none" : metadata.parentTemplate())
                    + ", state=" + metadata.state()
                    + ", environment=" + metadata.environment()
                    + ", bytes=" + (data == null ? 0 : data.sizeBytes())
                    + ", checksum=" + (data == null || data.checksum() == null ? "none" : data.checksum());
        });
    }

    public boolean isLoaded(String worldId) {
        return findLoadedWorld(worldId) != null;
    }

    public World getBukkitWorld(String worldId) {
        World cached = loadedWorlds.get(worldId);
        return cached == null ? findLoadedWorld(worldId) : cached;
    }

    public boolean isDungeonInstanceWorld(World world) {
        if (world == null || world.getName().equalsIgnoreCase("lobby")) return false;
        for (Map.Entry<String, World> entry : loadedWorlds.entrySet()) {
            if (!entry.getValue().getUID().equals(world.getUID())) continue;
            SMPWorldMetadata metadata = loadedMetadata.get(entry.getKey());
            return metadata != null && "DUNGEON_INSTANCE".equalsIgnoreCase(metadata.worldType());
        }
        return false;
    }

    private SMPWorld snapshot(String worldId, SMPWorldState state, World world) {
        return new SMPWorld(worldId, SMPWorldMetadata.create(worldId, false, null).withState(state), state, null, runtimeFolder(worldId), world);
    }

    private void movePlayersToSpawn(World world) {
        World fallback = Bukkit.getWorlds().isEmpty() ? world : Bukkit.getWorlds().getFirst();
        Location target = fallback.getSpawnLocation();
        for (Player player : new ArrayList<>(world.getPlayers())) {
            player.teleport(target);
            player.sendMessage("You were moved to spawn because world " + world.getName() + " is unloading.");
        }
    }

    private Path resolveWorldFolder(String value) {
        Path path = Path.of(value);
        if (!path.isAbsolute()) path = plugin.getServer().getWorldContainer().toPath().resolve(value);
        return path.normalize();
    }

    private Path runtimeFolder(String worldId) {
        return runtimeRoot.resolve(worldId).normalize();
    }

    private World findLoadedWorld(String worldId) {
        if (worldId == null) return null;
        World cached = loadedWorlds.get(worldId);
        if (cached != null) return cached;
        World world = Bukkit.getWorld(bukkitWorldName(worldId));
        if (world == null) world = Bukkit.getWorld(worldId);
        if (world != null) return world;
        for (World candidate : Bukkit.getWorlds()) {
            if (candidate.getName().equalsIgnoreCase(worldId)
                    || candidate.getName().equalsIgnoreCase(bukkitWorldName(worldId))) return candidate;
        }
        return null;
    }

    private Path sourceFolder(String worldId, World world) {
        return world.getWorldFolder().toPath().toAbsolutePath().normalize();
    }

    private boolean worldLooksEmpty(Path folder) throws IOException {
        Path region = folder.resolve("region");
        if (!Files.isDirectory(region)) return true;
        try (var files = Files.list(region)) {
            return files.filter(path -> path.getFileName().toString().endsWith(".mca"))
                    .noneMatch(path -> {
                        try { return Files.size(path) > 8192L; }
                        catch (IOException ignored) { return false; }
                    });
        }
    }

    private String bukkitWorldName(String worldId) {
        return "smp_runtime/" + worldId;
    }

    private void requireDatabase() throws SQLException {
        if (!repository.isAvailable()) throw new SQLException("SMP database is unavailable");
    }

    private void tryUpdateState(String worldId, SMPWorldState state) {
        try {
            if (repository.isAvailable()) repository.updateState(worldId, state);
        } catch (SQLException exception) {
            plugin.getLogger().warning("[SMPWorlds] Could not update state for " + worldId + ": " + exception.getMessage());
        }
    }

    private CompletableFuture<SMPWorldData> persistRuntimeWorldToDatabase(String worldId, SMPWorldMetadata metadata) {
        return async(() -> {
            requireDatabase();
            SMPWorldData data = container.pack(worldId, runtimeFolder(worldId));
            repository.saveWorld(metadata, data);
            plugin.getLogger().info("[SMPWorlds] Saved runtime world " + worldId
                    + " to database. bytes=" + data.sizeBytes()
                    + " checksum=" + (data.checksum() == null ? "none" : data.checksum()));
            return data;
        });
    }

    private String hubsaveSkipReason(SMPWorldMetadata metadata) {
        if (!metadata.persistent()) return "persistent=false";
        if (metadata.temporary()) return "temporary=true";
        if (!metadata.saveOnHubsave()) return "save_on_hubsave=false";
        if ("DUNGEON_INSTANCE".equalsIgnoreCase(metadata.worldType())) return "world_type=DUNGEON_INSTANCE";
        if (metadata.state() != SMPWorldState.LOADED) return "state=" + metadata.state();
        return null;
    }

    private void skip(SavePlan plan, String worldId, String reason) {
        plan.skipped.add(worldId + ": " + reason);
        plugin.getLogger().info("[SMPWorlds] Hubsave skipped " + worldId + ": " + reason);
    }

    private void fail(SavePlan plan, String worldId, Exception exception) {
        String message = exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage();
        plan.failed.add(worldId + ": " + message);
        plugin.getLogger().severe("[SMPWorlds] Hubsave failed for " + worldId + ": " + message);
    }

    private String validateWorldId(String worldId) {
        if (worldId == null || worldId.isBlank()) return "World id is required.";
        if (!worldId.matches("[A-Za-z0-9_-]+")) return "Invalid world id. Use only letters, numbers, underscore, and dash.";
        if (worldId.contains("..") || worldId.contains("/") || worldId.contains("\\") || worldId.contains(".") || worldId.contains(" ")) {
            return "Invalid world id. Path traversal is not allowed.";
        }
        return null;
    }

    private World.Environment parseEnvironment(String value) {
        if (value == null || value.isBlank()) return World.Environment.NORMAL;
        try {
            return World.Environment.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return World.Environment.NORMAL;
        }
    }

    private void rejectExistingImport(String worldId) throws SQLException, WorldAlreadyExistsException {
        plugin.getLogger().info("[SMPWorlds] Import check worldId=" + worldId);
        boolean exists = repository.exists(worldId);
        plugin.getLogger().info("[SMPWorlds] Import worldId already exists=" + exists + " worldId=" + worldId);
        if (exists) throw new WorldAlreadyExistsException(worldId);
    }

    private void logImportRows(String worldId) throws SQLException {
        plugin.getLogger().info("[SMPWorlds] Import inserted rows smp_worlds=1 worldId=" + worldId);
        plugin.getLogger().info("[SMPWorlds] Import inserted rows smp_world_data=1 worldId=" + worldId);
        plugin.getLogger().info("[SMPWorlds] Total worlds after import=" + repository.countWorlds());
    }

    private <T> CompletableFuture<T> async(ThrowingSupplier<T> supplier) {
        CompletableFuture<T> future = new CompletableFuture<>();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                future.complete(supplier.get());
            } catch (Exception exception) {
                future.completeExceptionally(exception);
            }
        });
        return future;
    }

    private <T> CompletableFuture<T> main(ThrowingSupplier<T> supplier) {
        CompletableFuture<T> future = new CompletableFuture<>();
        Bukkit.getScheduler().runTask(plugin, () -> {
            try {
                future.complete(supplier.get());
            } catch (Exception exception) {
                future.completeExceptionally(exception);
            }
        });
        return future;
    }

    private CompletableFuture<String> failed(String message) {
        return CompletableFuture.completedFuture("Error: " + message);
    }

    private CompletableFuture<SMPWorld> failedWorld(String message) {
        CompletableFuture<SMPWorld> future = new CompletableFuture<>();
        future.completeExceptionally(new IllegalArgumentException(message));
        return future;
    }

    private interface ThrowingSupplier<T> {
        T get() throws Exception;
    }

    public static final class WorldAlreadyExistsException extends IOException {
        public WorldAlreadyExistsException(String worldId) {
            super("A SMPWorld with id " + worldId + " already exists.");
        }
    }

    public static final class WorldNotLoadedException extends IOException {
        public WorldNotLoadedException(String worldId) { super("World " + worldId + " is not loaded."); }
    }

    public static final class EmptyWorldOverwriteException extends IOException {
        public EmptyWorldOverwriteException(String worldId) {
            super("This world appears empty and a saved SMPWorld already exists. Use --force only if you really want to overwrite it.");
        }
    }

    private record WorldSaveTarget(String worldId, SMPWorldMetadata metadata) {
    }

    private static final class SavePlan {
        private final List<WorldSaveTarget> targets = new ArrayList<>();
        private final List<WorldSaveTarget> savedOnMainThread = new ArrayList<>();
        private final List<String> saved = new ArrayList<>();
        private final List<String> failed = new ArrayList<>();
        private final List<String> skipped = new ArrayList<>();
    }

    private static final class VoidGenerator extends ChunkGenerator {
    }
}
